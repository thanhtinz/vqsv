package com.vqsv.core.net

interface PacketListener {
    fun onAuthOk(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int)
    fun onMoveOk(x: Int, y: Int)
    fun onWildEncounter(x: Int, y: Int, battleId: String, name: String, level: Int, hp: Int, catchable: Boolean, spriteId: Int)
    fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String)
    fun onChat(name: String, text: String)
    fun onPlayerNear(playerId: Long, present: Boolean, mapId: Int, x: Int, y: Int, name: String)
    fun onError(msg: String)
    fun onPong()
}
