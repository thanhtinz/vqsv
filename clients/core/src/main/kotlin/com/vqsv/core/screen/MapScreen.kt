package com.vqsv.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.vqsv.core.VqsvGame
import com.vqsv.core.asset.GameAssets
import com.vqsv.core.asset.TileMap
import com.vqsv.core.model.GameState
import com.vqsv.core.net.PacketListener

/**
 * World screen. Renders the REAL original tile map (when the converted assets are
 * present under src/main/resources/game/) using a y-down camera so tiles line up with the
 * J2ME coordinate system. Falls back to a placeholder grid when assets are absent.
 */
class MapScreen(private val game: VqsvGame) : Screen, PacketListener {

    private val shape = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()                       // HUD text (y-up)
    private val worldCam = OrthographicCamera()           // tiles/sprites (y-down)
    private val hudCam = OrthographicCamera()             // HUD (y-up)

    private var tileMap: TileMap? = null
    private val tile get() = tileMap?.tileSize ?: 32

    private val chatLog = ArrayList<String>()   // recent chat messages

    private class Other(val name: String, val mapId: Int, val x: Int, val y: Int)
    private val others = HashMap<Long, Other>()  // other players' live positions

    // BATTLE_TRAINER NPCs on the current map (walk up to duel them).
    private val trainers = ArrayList<com.vqsv.core.net.RestClient.NpcInfo>()

    private var pvpInviteFrom: Long? = null       // pending PvP invite (challenger id)
    private var pvpInviteName: String = ""

    private val PLACEHOLDER_TILE = 32f
    private val MAP_COLS = 20
    private val MAP_ROWS = 12
    private var moveCooldown = 0f
    private val MOVE_DELAY = 0.18f

    override fun show() {
        game.tcp.listener = this
        resize(Gdx.graphics.width, Gdx.graphics.height)
        if (GameAssets.available()) tileMap = TileMap.load(GameState.mapId)
        loadTrainers()
        // Pull medal (huy chuong) + hp from the REST profile so the HUD can show them
        // (the TCP auth packet only carries gold/kim tien).
        if (GameState.token.isNotEmpty()) {
            game.rest.getMyPlayer(GameState.token) { p, _ ->
                if (p != null) Gdx.app.postRunnable {
                    GameState.huyChuong = p.huyChuong
                    GameState.hp = p.hp; GameState.hpMax = p.hpMax
                }
            }
        }
    }

    /** Fetch BATTLE_TRAINER NPCs for the current map so they can be drawn and dueled. */
    private fun loadTrainers() {
        game.rest.getNpcs(GameState.token, GameState.mapId) { list, _ ->
            Gdx.app.postRunnable {
                trainers.clear()
                if (list != null) trainers.addAll(list.filter { it.npcType == "BATTLE_TRAINER" })
            }
        }
    }

    /** The trainer the player is standing on or next to, if any. */
    private fun adjacentTrainer(): com.vqsv.core.net.RestClient.NpcInfo? =
        trainers.firstOrNull {
            Math.abs(it.posX - GameState.posX) <= 1 && Math.abs(it.posY - GameState.posY) <= 1
        }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        moveCooldown -= delta
        handleInput()

        val map = tileMap
        if (map != null) {
            // Real map, y-down world space.
            batch.projectionMatrix = worldCam.combined
            batch.begin(); map.draw(batch); batch.end()
            // Player marker in the same y-down space.
            shape.projectionMatrix = worldCam.combined
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = Color.ORANGE
            trainers.forEach { shape.rect(it.posX * tile.toFloat(), it.posY * tile.toFloat(), tile.toFloat(), tile.toFloat()) }
            shape.color = Color.MAGENTA
            others.values.forEach { if (it.mapId == GameState.mapId) shape.rect(it.x * tile.toFloat(), it.y * tile.toFloat(), tile.toFloat(), tile.toFloat()) }
            shape.color = Color.CYAN
            shape.rect(GameState.posX * tile.toFloat(), GameState.posY * tile.toFloat(), tile.toFloat(), tile.toFloat())
            shape.end()
        } else {
            // Placeholder grid (y-up).
            shape.projectionMatrix = hudCam.combined
            shape.begin(ShapeRenderer.ShapeType.Filled)
            for (row in 0 until MAP_ROWS) for (col in 0 until MAP_COLS) {
                shape.color = if ((row + col) % 2 == 0) Color(0.2f, 0.6f, 0.2f, 1f) else Color(0.15f, 0.5f, 0.15f, 1f)
                shape.rect(col * PLACEHOLDER_TILE, row * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE)
            }
            shape.color = Color.ORANGE
            trainers.forEach { shape.rect(it.posX * PLACEHOLDER_TILE, (MAP_ROWS - 1 - it.posY) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE) }
            shape.color = Color.MAGENTA
            others.values.forEach { if (it.mapId == GameState.mapId) shape.rect(it.x * PLACEHOLDER_TILE, (MAP_ROWS - 1 - it.y) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE) }
            shape.color = Color.CYAN
            shape.rect(GameState.posX * PLACEHOLDER_TILE, (MAP_ROWS - 1 - GameState.posY) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE)
            shape.end()
        }

        // HUD (y-up).
        batch.projectionMatrix = hudCam.combined
        batch.begin()
        val h = Gdx.graphics.height.toFloat()
        font.color = Color.WHITE
        font.draw(batch, "${GameState.playerName} | Lv.${GameState.level} | HP:${GameState.hp}/${GameState.hpMax} | Xu:${GameState.kimTien} | HC:${GameState.huyChuong}", 6f, h - 6f)
        val onlineHere = others.values.filter { it.mapId == GameState.mapId }
        if (onlineHere.isNotEmpty()) {
            font.color = Color.MAGENTA
            font.draw(batch, "Online: " + onlineHere.joinToString(", ") { it.name }.take(60), 6f, h - 24f)
            font.color = Color.WHITE
        }
        font.draw(batch, "WASD | P:Shop B:Tui M:Menu T:Chat F:PvP", 6f, 22f)
        // Contextual trainer prompt (original walk-up-to-fight behaviour).
        adjacentTrainer()?.let {
            font.color = Color.ORANGE
            font.draw(batch, "G: Giao dau voi ${it.name}", 6f, 40f)
            font.color = Color.WHITE
        }
        // Chat log (recent messages).
        font.color = Color(0.8f, 0.9f, 1f, 1f)
        chatLog.forEachIndexed { i, line ->
            font.draw(batch, line, 6f, 46f + (chatLog.size - 1 - i) * 18f)
        }
        // PvP invite popup.
        pvpInviteFrom?.let {
            font.color = Color.YELLOW
            font.draw(batch, "$pvpInviteName moi ban PvP!  Y = Dong y   N = Tu choi", 6f, h * 0.5f)
            font.color = Color.WHITE
        }
        batch.end()
    }

    private fun handleInput() {
        // Menu / shop / bag.
        if (Gdx.input.isKeyJustPressed(Keys.M)) {
            game.setScreen(UiScreen(game, "gamemenu", onBack = { game.setScreen(MapScreen(game)) }, menuItems = listOf(
                "Cua hang" to { game.setScreen(ShopScreen(game)) },
                "Sung vat / Tui do" to { game.setScreen(PetsScreen(game)) },
                "Tro lai ban do" to { game.setScreen(MapScreen(game)) }
            ))); return
        }
        if (Gdx.input.isKeyJustPressed(Keys.P)) { game.setScreen(ShopScreen(game)); return }
        if (Gdx.input.isKeyJustPressed(Keys.B)) { game.setScreen(PetsScreen(game)); return }
        // Respond to a pending PvP invite.
        pvpInviteFrom?.let { cid ->
            if (Gdx.input.isKeyJustPressed(Keys.Y)) { game.tcp.sendPvpRespond(cid, true); pvpInviteFrom = null; return }
            if (Gdx.input.isKeyJustPressed(Keys.N)) { game.tcp.sendPvpRespond(cid, false); pvpInviteFrom = null; return }
        }
        // G: duel the NPC trainer you are standing next to (walk up to fight).
        if (Gdx.input.isKeyJustPressed(Keys.G)) {
            val t = adjacentTrainer()
            if (t != null) { game.tcp.sendStartTrainer(t.id); chatLog.add("Giao dau voi ${t.name}!") }
            else chatLog.add("Hay den gan mot huan luyen vien")
            return
        }
        // F: challenge the nearest other player on this map to PvP.
        if (Gdx.input.isKeyJustPressed(Keys.F)) {
            val target = others.entries.firstOrNull { it.value.mapId == GameState.mapId }
            if (target != null) { game.tcp.sendPvpChallenge(target.key); chatLog.add("Da gui loi moi dau ${target.value.name}") }
            else chatLog.add("Khong co nguoi choi gan day")
            return
        }
        // Chat: T opens a text input; the message is broadcast to all players.
        if (Gdx.input.isKeyJustPressed(Keys.T)) {
            Gdx.input.getTextInput(object : com.badlogic.gdx.Input.TextInputListener {
                override fun input(text: String) { if (text.isNotBlank()) game.tcp.sendChat(text.take(128)) }
                override fun canceled() {}
            }, "Chat", "", "Nhap tin nhan")
            return
        }
        if (moveCooldown > 0f) return
        val dir = when {
            Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP) -> 0
            Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN) -> 1
            Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT) -> 2
            Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT) -> 3
            else -> -1
        }
        if (dir >= 0) { game.tcp.sendMove(dir); moveCooldown = MOVE_DELAY }
    }

    override fun onMoveOk(x: Int, y: Int) { GameState.posX = x; GameState.posY = y }

    override fun onWildEncounter(x: Int, y: Int, battleId: String, name: String, level: Int, hp: Int, catchable: Boolean, spriteId: Int) {
        GameState.posX = x; GameState.posY = y
        GameState.currentBattleId = battleId
        GameState.battleEnemyName = name
        GameState.battleEnemyLevel = level
        GameState.battleEnemyHp = hp
        GameState.battleCatchable = catchable
        GameState.battleEnemySpriteId = spriteId
        GameState.battlePlayerHp = GameState.hp
        Gdx.app.postRunnable { game.setScreen(BattleScreen(game)) }
    }

    override fun onAuthOk(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int) {}
    override fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String) {}
    override fun onChat(name: String, text: String) {
        Gdx.app.postRunnable {
            chatLog.add("$name: $text")
            while (chatLog.size > 6) chatLog.removeAt(0)
        }
    }
    override fun onPlayerNear(playerId: Long, present: Boolean, mapId: Int, x: Int, y: Int, name: String) {
        Gdx.app.postRunnable {
            if (present) others[playerId] = Other(name, mapId, x, y) else others.remove(playerId)
        }
    }
    override fun onPvpInvite(challengerId: Long, name: String) {
        Gdx.app.postRunnable { pvpInviteFrom = challengerId; pvpInviteName = name }
    }
    override fun onPvpStart(battleId: String, oppName: String, myHp: Int, oppHp: Int, oppSpriteId: Int) {
        Gdx.app.postRunnable {
            GameState.currentBattleId = battleId
            GameState.battleEnemyName = oppName
            GameState.battleEnemyLevel = 0
            GameState.battleEnemyHp = oppHp
            GameState.battlePlayerHp = myHp
            GameState.battleEnemySpriteId = oppSpriteId
            GameState.battleCatchable = false
            GameState.battleIsPvp = true
            game.setScreen(BattleScreen(game))
        }
    }
    override fun onEnemySwap(name: String, hpMax: Int, spriteId: Int) {}
    override fun onPong() {}
    override fun onError(msg: String) {}

    override fun resize(width: Int, height: Int) {
        worldCam.setToOrtho(true, width.toFloat(), height.toFloat())   // y-down
        hudCam.setToOrtho(false, width.toFloat(), height.toFloat())    // y-up
    }

    override fun hide() {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        shape.dispose(); batch.dispose(); font.dispose()
    }
}
