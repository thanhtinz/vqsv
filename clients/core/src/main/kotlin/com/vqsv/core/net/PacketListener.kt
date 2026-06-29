package com.vqsv.core.net

interface PacketListener {
    fun onAuthOk(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int)
    fun onMoveOk(x: Int, y: Int)
    fun onWildEncounter(x: Int, y: Int, battleId: String, name: String, level: Int, hp: Int, catchable: Boolean, spriteId: Int)
    fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String)
    fun onChat(name: String, text: String)
    fun onPlayerNear(playerId: Long, present: Boolean, mapId: Int, x: Int, y: Int, name: String)
    fun onPvpInvite(challengerId: Long, name: String)
    fun onPvpStart(battleId: String, oppName: String, myHp: Int, oppHp: Int, oppSpriteId: Int)
    fun onEnemySwap(name: String, hpMax: Int, spriteId: Int)
    // npcType: 0=DIALOG, 1=SHOP, 2=BATTLE_TRAINER. Default no-op so only the map screen handles it.
    fun onNpcDialog(npcName: String, dialog: String, npcType: Int) {}
    // Warp to another map. Default no-op so only the map screen handles it.
    fun onMapChange(mapId: Int, x: Int, y: Int) {}
    fun onError(msg: String)
    fun onPong()
}
