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

    // All NPCs on the current map (walk up to talk; trainers can be dueled).
    private val npcs = ArrayList<com.vqsv.core.net.RestClient.NpcInfo>()

    private var pvpInviteFrom: Long? = null       // pending PvP invite (challenger id)
    private var pvpInviteName: String = ""

    private var dialogNpcName: String = ""        // active NPC dialog overlay (empty = none)
    private var dialogText: String = ""
    private var talkingNpcId: Int = 0             // the NPC we last said E to (for quest offers)
    private var availableQuests: List<com.vqsv.core.net.RestClient.QuestInfo> = emptyList()

    private val PLACEHOLDER_TILE = 32f
    private val MAP_COLS = 20
    private val MAP_ROWS = 12
    private var moveCooldown = 0f
    private val MOVE_DELAY = 0.18f

    override fun show() {
        game.tcp.listener = this
        resize(Gdx.graphics.width, Gdx.graphics.height)
        if (GameAssets.available()) tileMap = TileMap.load(GameState.mapId)
        loadNpcs()
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

    /** Fetch all NPCs for the current map so they can be drawn, talked to, and dueled. */
    private fun loadNpcs() {
        game.rest.getNpcs(GameState.token, GameState.mapId) { list, _ ->
            Gdx.app.postRunnable {
                npcs.clear()
                if (list != null) npcs.addAll(list)
            }
        }
    }

    private fun isAdjacent(n: com.vqsv.core.net.RestClient.NpcInfo) =
        Math.abs(n.posX - GameState.posX) <= 1 && Math.abs(n.posY - GameState.posY) <= 1

    /** The trainer the player is standing on or next to, if any. */
    private fun adjacentTrainer(): com.vqsv.core.net.RestClient.NpcInfo? =
        npcs.firstOrNull { it.npcType == "BATTLE_TRAINER" && isAdjacent(it) }

    /** Any NPC the player is standing next to (to talk to), if any. */
    private fun adjacentNpc(): com.vqsv.core.net.RestClient.NpcInfo? =
        npcs.firstOrNull { isAdjacent(it) }

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
            npcs.forEach {
                shape.color = if (it.npcType == "BATTLE_TRAINER") Color.ORANGE else Color.LIME
                shape.rect(it.posX * tile.toFloat(), it.posY * tile.toFloat(), tile.toFloat(), tile.toFloat())
            }
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
            npcs.forEach {
                shape.color = if (it.npcType == "BATTLE_TRAINER") Color.ORANGE else Color.LIME
                shape.rect(it.posX * PLACEHOLDER_TILE, (MAP_ROWS - 1 - it.posY) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE)
            }
            shape.color = Color.MAGENTA
            others.values.forEach { if (it.mapId == GameState.mapId) shape.rect(it.x * PLACEHOLDER_TILE, (MAP_ROWS - 1 - it.y) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE) }
            shape.color = Color.CYAN
            shape.rect(GameState.posX * PLACEHOLDER_TILE, (MAP_ROWS - 1 - GameState.posY) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE)
            shape.end()
        }

        val w = Gdx.graphics.width.toFloat()
        // NPC dialog box background (y-up), drawn under the HUD text.
        if (dialogNpcName.isNotEmpty()) {
            shape.projectionMatrix = hudCam.combined
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = Color(0f, 0f, 0f, 0.85f)
            shape.rect(20f, 20f, w - 40f, 120f)
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
        font.draw(batch, "WASD | E:Noi chuyen Q:Nhiem vu P:Shop B:Tui M:Menu T:Chat F:PvP", 6f, 22f)
        // Contextual prompts (original walk-up-to-interact behaviour).
        adjacentTrainer()?.let {
            font.color = Color.ORANGE
            font.draw(batch, "G: Giao dau voi ${it.name}", 6f, 40f)
            font.color = Color.WHITE
        }
        adjacentNpc()?.takeIf { it.npcType != "BATTLE_TRAINER" }?.let {
            font.color = Color.LIME
            font.draw(batch, "E: Noi chuyen voi ${it.name}", 6f, 40f)
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
        // NPC dialog text overlay.
        if (dialogNpcName.isNotEmpty()) {
            font.color = Color.YELLOW
            font.draw(batch, dialogNpcName, 32f, 128f)
            font.color = Color.WHITE
            dialogText.split("\n").forEachIndexed { i, line ->
                font.draw(batch, line, 32f, 108f - i * 18f)
            }
            availableQuests.firstOrNull()?.let {
                font.color = Color.LIME
                font.draw(batch, "Y: Nhan nhiem vu \"${it.name}\"", 32f, 44f)
                font.color = Color.WHITE
            }
            font.color = Color.GRAY
            font.draw(batch, "[E/Enter de dong]", w - 160f, 36f)
            font.color = Color.WHITE
        }
        batch.end()
    }

    private fun handleInput() {
        // An open NPC dialog captures input.
        if (dialogNpcName.isNotEmpty()) {
            // Y accepts the first quest this NPC is offering.
            if (Gdx.input.isKeyJustPressed(Keys.Y) && availableQuests.isNotEmpty()) {
                val q = availableQuests.first()
                game.rest.acceptQuest(GameState.token, q.id) { ok, _ ->
                    Gdx.app.postRunnable { chatLog.add(if (ok) "Da nhan nhiem vu: ${q.name}" else "Khong the nhan nhiem vu") }
                }
                dialogNpcName = ""; dialogText = ""; availableQuests = emptyList(); return
            }
            // Any of E/Enter/Esc closes it.
            if (Gdx.input.isKeyJustPressed(Keys.E) || Gdx.input.isKeyJustPressed(Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                dialogNpcName = ""; dialogText = ""; availableQuests = emptyList()
            }
            return
        }
        // E: talk to the NPC you are standing next to (walk up to interact).
        if (Gdx.input.isKeyJustPressed(Keys.E)) {
            val n = adjacentNpc()
            if (n != null) { talkingNpcId = n.id; game.tcp.sendTalkNpc(n.id) }
            else chatLog.add("Khong co ai gan day de noi chuyen")
            return
        }
        // Q: open the quest log.
        if (Gdx.input.isKeyJustPressed(Keys.Q)) { game.setScreen(QuestScreen(game)); return }
        // Menu / shop / bag.
        if (Gdx.input.isKeyJustPressed(Keys.M)) {
            game.setScreen(UiScreen(game, "gamemenu", onBack = { game.setScreen(MapScreen(game)) }, menuItems = listOf(
                "Cua hang" to { game.setScreen(ShopScreen(game)) },
                "Sung vat / Tui do" to { game.setScreen(PetsScreen(game)) },
                "Nhiem vu" to { game.setScreen(QuestScreen(game)) },
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

    override fun onMapChange(mapId: Int, x: Int, y: Int) {
        Gdx.app.postRunnable {
            GameState.mapId = mapId; GameState.posX = x; GameState.posY = y
            others.clear()
            if (GameAssets.available()) tileMap = TileMap.load(mapId)
            loadNpcs()
            chatLog.add("Da den ban do moi!")
        }
    }

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
    override fun onNpcDialog(npcName: String, dialog: String, npcType: Int) {
        Gdx.app.postRunnable { dialogNpcName = npcName; dialogText = dialog; availableQuests = emptyList() }
        // Does this NPC have a quest to offer? Fetch it so the dialog can prompt.
        if (talkingNpcId > 0) game.rest.getAvailableQuests(GameState.token, talkingNpcId) { list, _ ->
            Gdx.app.postRunnable { availableQuests = list ?: emptyList() }
        }
    }
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
