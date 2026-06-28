package com.vqsv.network

import com.vqsv.dto.BattleAction
import com.vqsv.dto.LoginRequest
import com.vqsv.dto.MoveRequest
import com.vqsv.game.battle.BattleService
import com.vqsv.game.map.MapService
import com.vqsv.service.AuthService
import com.vqsv.util.JwtUtil
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

// ============================================================
// J2ME BINARY PROTOCOL OPCODES
// Matches original game packet structure (reversed from bytecode)
// ============================================================
object Op {
    // Client -> Server
    const val LOGIN        = 0x01.toByte()
    const val MOVE         = 0x02.toByte()
    const val BATTLE_ACT   = 0x03.toByte()
    const val CHAT         = 0x04.toByte()
    const val PING         = 0x05.toByte()
    const val BUY_ITEM     = 0x06.toByte()
    const val USE_ITEM     = 0x07.toByte()
    const val PVP_CHALLENGE = 0x08.toByte() // [4B targetPlayerId]
    const val PVP_RESPOND   = 0x09.toByte() // [4B challengerId][1B accept]
    const val START_TRAINER = 0x0A.toByte() // [2B trainerId] (0 = pick one on current map)

    // Server -> Client
    const val AUTH_OK      = 0x81.toByte()
    const val AUTH_FAIL    = 0x82.toByte()
    const val MOVE_OK      = 0x83.toByte()
    const val WILD_ENC     = 0x84.toByte()  // wild encounter
    const val BATTLE_TURN  = 0x85.toByte()
    const val PLAYER_NEAR  = 0x86.toByte()  // nearby player update
    const val CHAT_MSG     = 0x87.toByte()
    const val PONG         = 0x88.toByte()
    const val PVP_INVITE   = 0x89.toByte()  // [4B challengerId][2B nameLen][name]
    const val PVP_START    = 0x8A.toByte()  // [2B bidLen][battleId][2B oppNameLen][oppName][2B myHp][2B oppHp]
    const val ERROR        = 0xFF.toByte()
}

@Component
class TcpGateway(
    @Value("\${vqsv.tcp.port}") private val port: Int,
    private val authService: AuthService,
    private val mapService: MapService,
    private val battleService: BattleService,
    private val pvpService: com.vqsv.game.battle.PvpService,
    private val npcRepo: com.vqsv.repository.NpcRepository,
    private val jwtUtil: JwtUtil
) {
    private val log = LoggerFactory.getLogger(TcpGateway::class.java)
    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()

    // channel -> playerId for authenticated sessions
    private val sessions = ConcurrentHashMap<ChannelId, Long>()
    // channel -> player name (for chat broadcast)
    private val names = ConcurrentHashMap<ChannelId, String>()
    // channel -> [mapId, x, y] live position (for presence broadcast)
    private val positions = ConcurrentHashMap<ChannelId, IntArray>()
    // playerId -> channel (to push targeted messages for PvP)
    private val playerChannels = ConcurrentHashMap<Long, io.netty.channel.Channel>()
    // pending PvP challenges: challengerId -> targetId
    private val pendingChallenges = ConcurrentHashMap<Long, Long>()
    // all live channels, for broadcasting
    private val channels = io.netty.channel.group.DefaultChannelGroup(io.netty.util.concurrent.GlobalEventExecutor.INSTANCE)

    @PostConstruct
    fun start() {
        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        // 2-byte length prefix frame
                        addLast(LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2))
                        addLast(LengthFieldPrepender(2))
                        addLast(GameHandler())
                    }
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)

        bootstrap.bind(port).addListener { f ->
            if (f.isSuccess) log.info("[OK] J2ME TCP gateway listening on port $port")
            else log.error("[ERR] TCP gateway failed to bind: ${f.cause().message}")
        }
    }

    @PreDestroy
    fun stop() {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    inner class GameHandler : SimpleChannelInboundHandler<ByteBuf>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
            if (!msg.isReadable) return
            val opcode = msg.readByte()

            try {
                when (opcode) {
                    Op.LOGIN -> handleLogin(ctx, msg)
                    Op.MOVE  -> handleMove(ctx, msg)
                    Op.BATTLE_ACT -> handleBattleAct(ctx, msg)
                    Op.CHAT  -> handleChat(ctx, msg)
                    Op.PVP_CHALLENGE -> handlePvpChallenge(ctx, msg)
                    Op.PVP_RESPOND   -> handlePvpRespond(ctx, msg)
                    Op.START_TRAINER -> handleStartTrainer(ctx, msg)
                    Op.PING  -> ctx.write(buildPong())
                    else     -> sendError(ctx, "Unknown opcode 0x${opcode.toString(16)}")
                }
            } catch (e: Exception) {
                log.warn("[WARN] TCP handler error: ${e.message}")
                sendError(ctx, e.message ?: "Error")
            }
            ctx.flush()
        }

        private fun handleLogin(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val usernameLen = buf.readShort().toInt()
            val username = buf.readBytes(usernameLen).toString(Charsets.UTF_8)
            val passwordLen = buf.readShort().toInt()
            val password = buf.readBytes(passwordLen).toString(Charsets.UTF_8)

            val result = runCatching {
                authService.login(LoginRequest(username, password))
            }

            if (result.isSuccess) {
                val auth = result.getOrThrow()
                sessions[ctx.channel().id()] = auth.player.id
                names[ctx.channel().id()] = auth.player.name
                val resp = ctx.alloc().buffer()
                resp.writeByte(Op.AUTH_OK.toInt())
                val tokenBytes = auth.token.toByteArray()
                resp.writeShort(tokenBytes.size)
                resp.writeBytes(tokenBytes)
                resp.writeShort(auth.player.level.toInt())
                resp.writeInt(auth.player.kimTien)
                resp.writeShort(auth.player.mapId.toInt())
                resp.writeByte(auth.player.posX.toInt())
                resp.writeByte(auth.player.posY.toInt())
                ctx.write(resp)

                val id = ctx.channel().id()
                positions[id] = intArrayOf(auth.player.mapId.toInt(), auth.player.posX.toInt(), auth.player.posY.toInt())
                playerChannels[auth.player.id] = ctx.channel()
                // Send existing players to the newcomer, then announce the newcomer to everyone else.
                positions.forEach { (cid, p) ->
                    if (cid != id) ctx.write(buildPresence(ctx, sessions[cid] ?: 0L, true, p[0], p[1], p[2], names[cid] ?: "?"))
                }
                broadcastPresence(id, auth.player.id, true, auth.player.mapId.toInt(),
                    auth.player.posX.toInt(), auth.player.posY.toInt(), auth.player.name)
            } else {
                sendError(ctx, result.exceptionOrNull()?.message ?: "Login failed")
            }
        }

        private fun handleMove(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val playerId = sessions[ctx.channel().id()] ?: run {
                sendError(ctx, "Not authenticated"); return
            }
            val dirCode = buf.readByte().toInt()
            val direction = when (dirCode) {
                0 -> "UP"; 1 -> "DOWN"; 2 -> "LEFT"; 3 -> "RIGHT"
                else -> { sendError(ctx, "Bad direction"); return }
            }

            val result = mapService.move(playerId, direction)
            val resp = ctx.alloc().buffer()

            if (result.wildEncounter != null) {
                resp.writeByte(Op.WILD_ENC.toInt())
                resp.writeByte(result.newX.toInt())
                resp.writeByte(result.newY.toInt())
                val enc = result.wildEncounter
                val battleIdBytes = enc.battleId.toByteArray()
                resp.writeShort(battleIdBytes.size)
                resp.writeBytes(battleIdBytes)
                val nameBytes = enc.enemyName.toByteArray(Charsets.UTF_8)
                resp.writeShort(nameBytes.size)
                resp.writeBytes(nameBytes)
                resp.writeByte(enc.enemyLevel.toInt())
                resp.writeShort(enc.enemyHp)
                resp.writeByte(if (enc.catchable) 1 else 0)
                resp.writeShort(enc.enemySpriteId.toInt())
            } else {
                resp.writeByte(Op.MOVE_OK.toInt())
                resp.writeByte(result.newX.toInt())
                resp.writeByte(result.newY.toInt())
            }
            ctx.write(resp)

            // Update tracked position and broadcast presence to other players.
            val id = ctx.channel().id()
            val pos = positions.getOrPut(id) { intArrayOf(1, result.newX.toInt(), result.newY.toInt()) }
            result.newMapId?.let { pos[0] = it.toInt() }
            pos[1] = result.newX.toInt(); pos[2] = result.newY.toInt()
            broadcastPresence(id, playerId, true, pos[0], pos[1], pos[2], names[id] ?: "?")
        }

        // ---- presence helpers ----
        private fun buildPresence(ctx: ChannelHandlerContext, playerId: Long, present: Boolean,
                                  mapId: Int, x: Int, y: Int, name: String): ByteBuf {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val buf = ctx.alloc().buffer()
            buf.writeByte(Op.PLAYER_NEAR.toInt())
            buf.writeInt(playerId.toInt())
            buf.writeByte(if (present) 1 else 0)
            buf.writeShort(mapId)
            buf.writeByte(x); buf.writeByte(y)
            buf.writeShort(nameBytes.size); buf.writeBytes(nameBytes)
            return buf
        }

        private fun broadcastPresence(exclude: ChannelId, playerId: Long, present: Boolean,
                                      mapId: Int, x: Int, y: Int, name: String) {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            channels.forEach { ch ->
                if (ch.id() == exclude) return@forEach
                val buf = ch.alloc().buffer()
                buf.writeByte(Op.PLAYER_NEAR.toInt())
                buf.writeInt(playerId.toInt())
                buf.writeByte(if (present) 1 else 0)
                buf.writeShort(mapId)
                buf.writeByte(x); buf.writeByte(y)
                buf.writeShort(nameBytes.size); buf.writeBytes(nameBytes)
                ch.writeAndFlush(buf)
            }
        }

        private fun handleBattleAct(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val playerId = sessions[ctx.channel().id()] ?: run {
                sendError(ctx, "Not authenticated"); return
            }
            val battleIdLen = buf.readShort().toInt()
            val battleId = buf.readBytes(battleIdLen).toString(Charsets.UTF_8)
            val actionCode = buf.readByte()
            val action = when (actionCode.toInt()) {
                0 -> "ATTACK"; 1 -> "USE_ITEM"; 2 -> "CATCH"; 3 -> "RUN"
                else -> "ATTACK"
            }
            val itemId: Short? = if (buf.readableBytes() >= 2) buf.readShort() else null

            // PvP battles are routed to the PvpService and resolve when both players act.
            if (pvpService.isPvp(battleId)) {
                val rr = pvpService.submitAction(battleId, playerId, actionCode.toInt())
                if (rr != null) sendPvpRound(rr)
                return
            }

            val turnResult = battleService.processTurn(
                playerId,
                BattleAction(battleId = battleId, action = action, itemId = itemId)
            )

            val resp = ctx.alloc().buffer()
            resp.writeByte(Op.BATTLE_TURN.toInt())
            resp.writeShort(turnResult.playerPetHp)
            resp.writeShort(turnResult.enemyHp)
            val statusBytes = turnResult.status.toByteArray()
            resp.writeByte(statusBytes.size)
            resp.writeBytes(statusBytes)
            val logText = turnResult.log.joinToString("\n").toByteArray(Charsets.UTF_8)
            resp.writeShort(logText.size)
            resp.writeBytes(logText)
            ctx.write(resp)
        }

        // ---- PvP ----
        private fun handlePvpChallenge(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val challengerId = sessions[ctx.channel().id()] ?: run { sendError(ctx, "Not authenticated"); return }
            val targetId = buf.readInt().toLong()
            val targetCh = playerChannels[targetId] ?: run { sendError(ctx, "Người chơi không trực tuyến"); return }
            pendingChallenges[challengerId] = targetId
            val name = (names[ctx.channel().id()] ?: "?").toByteArray(Charsets.UTF_8)
            val inv = targetCh.alloc().buffer()
            inv.writeByte(Op.PVP_INVITE.toInt())
            inv.writeInt(challengerId.toInt())
            inv.writeShort(name.size); inv.writeBytes(name)
            targetCh.writeAndFlush(inv)
        }

        private fun handlePvpRespond(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val responderId = sessions[ctx.channel().id()] ?: run { sendError(ctx, "Not authenticated"); return }
            val challengerId = buf.readInt().toLong()
            val accept = buf.readByte().toInt() != 0
            if (pendingChallenges.remove(challengerId) != responderId) { sendError(ctx, "Lời mời không hợp lệ"); return }
            val challengerCh = playerChannels[challengerId] ?: run { sendError(ctx, "Đối thủ đã rời"); return }
            if (!accept) { sendError(challengerCh, "${names[ctx.channel().id()] ?: "?"} đã từ chối"); return }

            val challengerName = names[challengerCh.id()] ?: "?"
            val responderName = names[ctx.channel().id()] ?: "?"
            val s = pvpService.start(challengerId, challengerName, responderId, responderName)
            if (s == null) { sendError(ctx, "Cần sủng vật để PvP"); sendError(challengerCh, "Cần sủng vật để PvP"); return }
            // a = challenger, b = responder
            sendPvpStart(challengerCh, s.battleId, s.b.name, s.a.hp, s.b.hp, s.b.spriteId)
            sendPvpStart(ctx.channel(), s.battleId, s.a.name, s.b.hp, s.a.hp, s.a.spriteId)
        }

        // ---- NPC trainer duels (original offline trainer battles) ----
        // The player must be standing on or next to a BATTLE_TRAINER NPC (the
        // original walk-up-to-fight behaviour). START_TRAINER carries the NPC id;
        // 0 means "the trainer adjacent to me".
        private fun handleStartTrainer(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val playerId = sessions[ctx.channel().id()] ?: run { sendError(ctx, "Not authenticated"); return }
            val id = ctx.channel().id()
            val pos = positions[id] ?: intArrayOf(1, 0, 0)
            val requestedNpc = if (buf.readableBytes() >= 2) buf.readShort() else 0

            val mapNpcs = npcRepo.findByMapId(pos[0].toShort())
                .filter { it.npcType == "BATTLE_TRAINER" }
            fun adjacent(n: com.vqsv.entity.Npc) =
                Math.abs(n.posX - pos[1]) <= 1 && Math.abs(n.posY - pos[2]) <= 1

            val npc = if (requestedNpc > 0) {
                mapNpcs.firstOrNull { it.id == requestedNpc }
            } else {
                mapNpcs.firstOrNull { adjacent(it) }
            }
            if (npc == null) { sendError(ctx, "Không có huấn luyện viên ở gần đây"); return }
            if (!adjacent(npc)) { sendError(ctx, "Hãy đến gần huấn luyện viên trước"); return }

            val templateId = npc.enemyTemplateId
                ?: run { sendError(ctx, "Huấn luyện viên này chưa sẵn sàng"); return }

            val session = battleService.startTrainerBattle(playerId, templateId)

            // Reuse the wild-encounter frame; catchable = 0 marks it as a duel.
            val resp = ctx.alloc().buffer()
            resp.writeByte(Op.WILD_ENC.toInt())
            resp.writeByte(pos[1]); resp.writeByte(pos[2])
            val bid = session.battleId.toByteArray()
            resp.writeShort(bid.size); resp.writeBytes(bid)
            val nm = session.enemyName.toByteArray(Charsets.UTF_8)
            resp.writeShort(nm.size); resp.writeBytes(nm)
            resp.writeByte(session.enemyLevel.toInt())
            resp.writeShort(session.enemyHp)
            resp.writeByte(0)   // not catchable
            resp.writeShort(session.enemySpriteId.toInt())
            ctx.write(resp)
        }

        private fun sendPvpStart(ch: io.netty.channel.Channel, battleId: String, oppName: String,
                                 myHp: Int, oppHp: Int, oppSpriteId: Short) {
            val bid = battleId.toByteArray(); val nm = oppName.toByteArray(Charsets.UTF_8)
            val b = ch.alloc().buffer()
            b.writeByte(Op.PVP_START.toInt())
            b.writeShort(bid.size); b.writeBytes(bid)
            b.writeShort(nm.size); b.writeBytes(nm)
            b.writeShort(myHp); b.writeShort(oppHp); b.writeShort(oppSpriteId.toInt())
            ch.writeAndFlush(b)
        }

        private fun sendPvpRound(rr: com.vqsv.game.battle.PvpService.RoundResult) {
            val logText = rr.log.joinToString("\n")
            // A's perspective
            playerChannels[rr.aId]?.let { sendBattleTurn(it, rr.aHp, rr.bHp, statusFor(rr.status, true), logText) }
            // B's perspective
            playerChannels[rr.bId]?.let { sendBattleTurn(it, rr.bHp, rr.aHp, statusFor(rr.status, false), logText) }
        }

        private fun statusFor(status: String, isA: Boolean): String = when (status) {
            "A_WIN" -> if (isA) "VICTORY" else "DEFEAT"
            "B_WIN" -> if (isA) "DEFEAT" else "VICTORY"
            else -> "ONGOING"
        }

        private fun sendBattleTurn(ch: io.netty.channel.Channel, myHp: Int, oppHp: Int, status: String, log: String) {
            val b = ch.alloc().buffer()
            b.writeByte(Op.BATTLE_TURN.toInt())
            b.writeShort(myHp); b.writeShort(oppHp)
            val st = status.toByteArray(); b.writeByte(st.size); b.writeBytes(st)
            val lg = log.toByteArray(Charsets.UTF_8); b.writeShort(lg.size); b.writeBytes(lg)
            ch.writeAndFlush(b)
        }

        private fun handleChat(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val id = ctx.channel().id()
            sessions[id] ?: run { sendError(ctx, "Not authenticated"); return }
            val textLen = buf.readShort().toInt()
            val text = buf.readBytes(textLen.coerceAtMost(128)).toString(Charsets.UTF_8)
            val name = names[id] ?: "?"
            log.debug("[CHAT] $name: $text")
            broadcastChat(name, text)
        }

        // Broadcast a CHAT_MSG to every connected client (fresh buffer per channel).
        private fun broadcastChat(name: String, text: String) {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val textBytes = text.toByteArray(Charsets.UTF_8)
            channels.forEach { ch ->
                val msg = ch.alloc().buffer()
                msg.writeByte(Op.CHAT_MSG.toInt())
                msg.writeShort(nameBytes.size); msg.writeBytes(nameBytes)
                msg.writeShort(textBytes.size); msg.writeBytes(textBytes)
                ch.writeAndFlush(msg)
            }
        }

        private fun buildPong(): ByteBuf {
            val buf = io.netty.buffer.Unpooled.buffer(1)
            buf.writeByte(Op.PONG.toInt())
            return buf
        }

        private fun sendError(ctx: ChannelHandlerContext, msg: String) {
            val resp = ctx.alloc().buffer()
            resp.writeByte(Op.ERROR.toInt())
            val msgBytes = msg.toByteArray(Charsets.UTF_8)
            resp.writeShort(msgBytes.size)
            resp.writeBytes(msgBytes)
            ctx.write(resp)
        }

        private fun sendError(ch: io.netty.channel.Channel, msg: String) {
            val resp = ch.alloc().buffer()
            resp.writeByte(Op.ERROR.toInt())
            val b = msg.toByteArray(Charsets.UTF_8)
            resp.writeShort(b.size); resp.writeBytes(b)
            ch.writeAndFlush(resp)
        }

        override fun channelActive(ctx: ChannelHandlerContext) {
            channels.add(ctx.channel())   // auto-removed on close
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            val id = ctx.channel().id()
            val pid = sessions.remove(id)
            val pos = positions.remove(id)
            names.remove(id)
            if (pid != null) {
                playerChannels.remove(pid)
                pendingChallenges.remove(pid)
                // If in a PvP battle, award the win to the remaining opponent.
                pvpService.abortFor(pid)?.let { (_, oppId) ->
                    playerChannels[oppId]?.let { sendBattleTurn(it, 1, 0, "VICTORY", "Đối thủ đã thoát.") }
                }
                broadcastPresence(id, pid, false, pos?.get(0) ?: 0, 0, 0, "")
            }
            log.debug("[INFO] Client disconnected: ${ctx.channel().remoteAddress()}")
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            log.warn("[WARN] Channel exception: ${cause.message}")
            ctx.close()
        }
    }
}
