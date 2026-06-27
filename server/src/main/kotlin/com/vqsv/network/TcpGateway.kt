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

object Op {
    const val LOGIN        = 0x01.toByte()
    const val MOVE         = 0x02.toByte()
    const val BATTLE_ACT   = 0x03.toByte()
    const val CHAT         = 0x04.toByte()
    const val PING         = 0x05.toByte()
    const val BUY_ITEM     = 0x06.toByte()
    const val USE_ITEM     = 0x07.toByte()

    const val AUTH_OK      = 0x81.toByte()
    const val AUTH_FAIL    = 0x82.toByte()
    const val MOVE_OK      = 0x83.toByte()
    const val WILD_ENC     = 0x84.toByte()
    const val BATTLE_TURN  = 0x85.toByte()
    const val PLAYER_NEAR  = 0x86.toByte()
    const val CHAT_MSG     = 0x87.toByte()
    const val PONG         = 0x88.toByte()
    const val ERROR        = 0xFF.toByte()
}

@Component
class TcpGateway(
    @Value("\${vqsv.tcp.port}") private val port: Int,
    private val authService: AuthService,
    private val mapService: MapService,
    private val battleService: BattleService,
    private val jwtUtil: JwtUtil
) {
    private val log = LoggerFactory.getLogger(TcpGateway::class.java)
    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()
    private val sessions = ConcurrentHashMap<ChannelId, Long>()

    @PostConstruct
    fun start() {
        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
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
                    Op.LOGIN      -> handleLogin(ctx, msg)
                    Op.MOVE       -> handleMove(ctx, msg)
                    Op.BATTLE_ACT -> handleBattleAct(ctx, msg)
                    Op.CHAT       -> handleChat(ctx, msg)
                    Op.PING       -> ctx.write(buildPong())
                    else          -> sendError(ctx, "Unknown opcode 0x${opcode.toString(16)}")
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

            val result = runCatching { authService.login(LoginRequest(username, password)) }

            if (result.isSuccess) {
                val auth = result.getOrThrow()
                sessions[ctx.channel().id()] = auth.player.id
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
            } else {
                sendError(ctx, result.exceptionOrNull()?.message ?: "Login failed")
            }
        }

        private fun handleMove(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val playerId = sessions[ctx.channel().id()] ?: run { sendError(ctx, "Not authenticated"); return }
            val direction = when (buf.readByte().toInt()) {
                0 -> "UP"; 1 -> "DOWN"; 2 -> "LEFT"; 3 -> "RIGHT"
                else -> { sendError(ctx, "Bad direction"); return }
            }

            val result = mapService.move(playerId, direction)
            val resp = ctx.alloc().buffer()

            if (result.wildEncounter != null) {
                val enc = result.wildEncounter
                resp.writeByte(Op.WILD_ENC.toInt())
                resp.writeByte(result.newX.toInt())
                resp.writeByte(result.newY.toInt())
                val battleIdBytes = enc.battleId.toByteArray()
                resp.writeShort(battleIdBytes.size)
                resp.writeBytes(battleIdBytes)
                val nameBytes = enc.enemyName.toByteArray(Charsets.UTF_8)
                resp.writeShort(nameBytes.size)
                resp.writeBytes(nameBytes)
                resp.writeByte(enc.enemyLevel.toInt())
                resp.writeShort(enc.enemyHp)
                resp.writeByte(if (enc.catchable) 1 else 0)
            } else {
                resp.writeByte(Op.MOVE_OK.toInt())
                resp.writeByte(result.newX.toInt())
                resp.writeByte(result.newY.toInt())
            }
            ctx.write(resp)
        }

        private fun handleBattleAct(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val playerId = sessions[ctx.channel().id()] ?: run { sendError(ctx, "Not authenticated"); return }
            val battleIdLen = buf.readShort().toInt()
            val battleId = buf.readBytes(battleIdLen).toString(Charsets.UTF_8)
            val action = when (buf.readByte().toInt()) {
                0 -> "ATTACK"; 1 -> "USE_ITEM"; 2 -> "CATCH"; 3 -> "RUN"; else -> "ATTACK"
            }
            val itemId: Short? = if (buf.readableBytes() >= 2) buf.readShort() else null

            val turnResult = battleService.processTurn(playerId, BattleAction(battleId = battleId, action = action, itemId = itemId))

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

        private fun handleChat(ctx: ChannelHandlerContext, buf: ByteBuf) {
            val playerId = sessions[ctx.channel().id()] ?: return
            val textLen = buf.readShort().toInt()
            val text = buf.readBytes(textLen.coerceAtMost(128)).toString(Charsets.UTF_8)
            log.debug("[CHAT] player $playerId: $text")
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

        override fun channelInactive(ctx: ChannelHandlerContext) {
            sessions.remove(ctx.channel().id())
            log.debug("[INFO] Client disconnected: ${ctx.channel().remoteAddress()}")
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            log.warn("[WARN] Channel exception: ${cause.message}")
            ctx.close()
        }
    }
}
