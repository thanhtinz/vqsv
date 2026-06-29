package com.vqsv.core.model

object GameState {
    var token: String = ""
    var playerName: String = ""
    var level: Int = 1
    var exp: Int = 0
    var kimTien: Int = 0
    var huyChuong: Int = 0
    var mapId: Int = 0
    var posX: Int = 0
    var posY: Int = 0
    var hp: Int = 100
    var hpMax: Int = 100

    var currentBattleId: String? = null
    var battlePlayerHp: Int = 0
    var battleEnemyHp: Int = 0
    var battleEnemyName: String = ""
    var battleEnemyLevel: Int = 1
    var battleEnemySpriteId: Int = -1
    var playerPetSpriteId: Int = -1
    var battleCatchable: Boolean = false
    var battleIsPvp: Boolean = false
    val battleLog: MutableList<String> = mutableListOf()

    fun updateFromTcpAuth(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int) {
        this.token = token
        this.level = level
        this.kimTien = kimTien
        this.mapId = mapId
        this.posX = posX
        this.posY = posY
    }

    fun clearBattle() {
        currentBattleId = null
        battlePlayerHp = 0
        battleEnemyHp = 0
        battleEnemyName = ""
        battleEnemyLevel = 1
        battleCatchable = false
        battleIsPvp = false
        battleEnemySpriteId = -1
        battleLog.clear()
    }
}
