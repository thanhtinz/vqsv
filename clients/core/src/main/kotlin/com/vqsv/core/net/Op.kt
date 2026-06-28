package com.vqsv.core.net

object Op {
    // Client -> Server
    const val LOGIN: Byte        = 0x01
    const val MOVE: Byte         = 0x02
    const val BATTLE_ACT: Byte   = 0x03
    const val CHAT: Byte         = 0x04
    const val PING: Byte         = 0x05

    // Server -> Client
    val AUTH_OK: Byte     = 0x81.toByte()
    val MOVE_OK: Byte     = 0x83.toByte()
    val WILD_ENC: Byte    = 0x84.toByte()
    val BATTLE_TURN: Byte = 0x85.toByte()
    val PLAYER_NEAR: Byte = 0x86.toByte()
    val CHAT_MSG: Byte    = 0x87.toByte()
    val PONG: Byte        = 0x88.toByte()
    val ERROR: Byte       = 0xFF.toByte()
}
