package com.vqsv.core.net

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

class TcpClient {
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var input: DataInputStream? = null
    private val sendQueue = LinkedBlockingQueue<ByteArray>()
    var listener: PacketListener? = null

    fun connect(host: String, port: Int) {
        val t = Thread({
            try {
                val s = Socket(host, port)
                socket = s
                output = DataOutputStream(s.outputStream)
                input = DataInputStream(s.inputStream)
                startSendLoop()
                startReceiveLoop()
            } catch (e: Exception) {
                listener?.onError("Connection failed: ${e.message}")
            }
        }, "tcp-connect")
        t.isDaemon = true
        t.start()
    }

    private fun startSendLoop() {
        val t = Thread({
            try {
                while (socket?.isClosed == false) {
                    val payload = sendQueue.take()
                    val out = output ?: break
                    out.writeShort(payload.size)
                    out.write(payload)
                    out.flush()
                }
            } catch (_: InterruptedException) {
            } catch (e: Exception) {
                listener?.onError("Send error: ${e.message}")
            }
        }, "tcp-send")
        t.isDaemon = true
        t.start()
    }

    private fun startReceiveLoop() {
        val t = Thread({
            try {
                val inp = input ?: return@Thread
                while (socket?.isClosed == false) {
                    val len = inp.readUnsignedShort()
                    val data = ByteArray(len)
                    inp.readFully(data)
                    handlePacket(data)
                }
            } catch (e: Exception) {
                if (socket?.isClosed == false) {
                    listener?.onError("Receive error: ${e.message}")
                }
            }
        }, "tcp-recv")
        t.isDaemon = true
        t.start()
    }

    private fun handlePacket(data: ByteArray) {
        if (data.isEmpty()) return
        var pos = 1

        val readByte: () -> Int = { (data[pos++].toInt() and 0xFF) }
        val readShort: () -> Int = {
            val hi = (data[pos++].toInt() and 0xFF)
            val lo = (data[pos++].toInt() and 0xFF)
            (hi shl 8) or lo
        }
        val readInt: () -> Int = {
            val b0 = (data[pos++].toInt() and 0xFF)
            val b1 = (data[pos++].toInt() and 0xFF)
            val b2 = (data[pos++].toInt() and 0xFF)
            val b3 = (data[pos++].toInt() and 0xFF)
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
        val readBytes: (Int) -> ByteArray = { n ->
            val arr = data.copyOfRange(pos, pos + n)
            pos += n
            arr
        }
        val readStr: (Int) -> String = { n -> String(readBytes(n), Charsets.UTF_8) }

        when (data[0]) {
            Op.AUTH_OK -> {
                val tokenLen = readShort()
                val token = readStr(tokenLen)
                val level = readShort()
                val kimTien = readInt()
                val mapId = readShort()
                val posX = readByte()
                val posY = readByte()
                listener?.onAuthOk(token, level, kimTien, mapId, posX, posY)
            }
            Op.MOVE_OK -> {
                val newX = readByte()
                val newY = readByte()
                listener?.onMoveOk(newX, newY)
            }
            Op.WILD_ENC -> {
                val x = readByte()
                val y = readByte()
                val battleIdLen = readShort()
                val battleId = readStr(battleIdLen)
                val nameLen = readShort()
                val name = readStr(nameLen)
                val level = readByte()
                val hp = readShort()
                val catchable = readByte() != 0
                val spriteId = readShort()
                listener?.onWildEncounter(x, y, battleId, name, level, hp, catchable, spriteId)
            }
            Op.BATTLE_TURN -> {
                val playerHp = readShort()
                val enemyHp = readShort()
                val statusLen = readByte()
                val status = readStr(statusLen)
                val logLen = readShort()
                val log = readStr(logLen)
                listener?.onBattleTurn(playerHp, enemyHp, status, log)
            }
            Op.PONG -> {
                listener?.onPong()
            }
            Op.ERROR -> {
                val msgLen = readShort()
                val msg = readStr(msgLen)
                listener?.onError(msg)
            }
        }
    }

    fun sendLogin(username: String, password: String) {
        val uBytes = username.toByteArray(Charsets.UTF_8)
        val pBytes = password.toByteArray(Charsets.UTF_8)
        val buf = ByteArray(1 + 2 + uBytes.size + 2 + pBytes.size)
        var i = 0
        buf[i++] = Op.LOGIN
        buf[i++] = (uBytes.size shr 8).toByte()
        buf[i++] = (uBytes.size and 0xFF).toByte()
        uBytes.forEach { buf[i++] = it }
        buf[i++] = (pBytes.size shr 8).toByte()
        buf[i++] = (pBytes.size and 0xFF).toByte()
        pBytes.forEach { buf[i++] = it }
        sendQueue.put(buf)
    }

    fun sendMove(direction: Int) {
        val buf = ByteArray(2)
        buf[0] = Op.MOVE
        buf[1] = direction.toByte()
        sendQueue.put(buf)
    }

    fun sendBattleAct(battleId: String, action: Int, itemId: Int? = null) {
        val bBytes = battleId.toByteArray(Charsets.UTF_8)
        val extraSize = if (itemId != null) 2 else 0
        val buf = ByteArray(1 + 2 + bBytes.size + 1 + extraSize)
        var i = 0
        buf[i++] = Op.BATTLE_ACT
        buf[i++] = (bBytes.size shr 8).toByte()
        buf[i++] = (bBytes.size and 0xFF).toByte()
        bBytes.forEach { buf[i++] = it }
        buf[i++] = action.toByte()
        if (itemId != null) {
            buf[i++] = (itemId shr 8).toByte()
            buf[i++] = (itemId and 0xFF).toByte()
        }
        sendQueue.put(buf)
    }

    fun sendChat(text: String) {
        val tBytes = text.toByteArray(Charsets.UTF_8).take(128).toByteArray()
        val buf = ByteArray(1 + 2 + tBytes.size)
        var i = 0
        buf[i++] = Op.CHAT
        buf[i++] = (tBytes.size shr 8).toByte()
        buf[i++] = (tBytes.size and 0xFF).toByte()
        tBytes.forEach { buf[i++] = it }
        sendQueue.put(buf)
    }

    fun sendPing() {
        sendQueue.put(byteArrayOf(Op.PING))
    }

    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
    }
}
