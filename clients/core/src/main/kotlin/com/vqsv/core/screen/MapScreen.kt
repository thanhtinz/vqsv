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
 * present under assets/game/) using a y-down camera so tiles line up with the
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

    private val PLACEHOLDER_TILE = 32f
    private val MAP_COLS = 20
    private val MAP_ROWS = 12
    private var moveCooldown = 0f
    private val MOVE_DELAY = 0.18f

    override fun show() {
        game.tcp.listener = this
        resize(Gdx.graphics.width, Gdx.graphics.height)
        if (GameAssets.available()) tileMap = TileMap.load(GameState.mapId)
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
            shape.color = Color.CYAN
            shape.rect(GameState.posX * PLACEHOLDER_TILE, (MAP_ROWS - 1 - GameState.posY) * PLACEHOLDER_TILE, PLACEHOLDER_TILE, PLACEHOLDER_TILE)
            shape.end()
        }

        // HUD (y-up).
        batch.projectionMatrix = hudCam.combined
        batch.begin()
        val h = Gdx.graphics.height.toFloat()
        font.color = Color.WHITE
        font.draw(batch, "${GameState.playerName} | Lv.${GameState.level} | HP:${GameState.hp}/${GameState.hpMax} | Xu:${GameState.kimTien}", 6f, h - 6f)
        font.draw(batch, "WASD di chuyen | P: Cua hang | B: Tui | M: Menu | T: Chat", 6f, 22f)
        // Chat log (recent messages).
        font.color = Color(0.8f, 0.9f, 1f, 1f)
        chatLog.forEachIndexed { i, line ->
            font.draw(batch, line, 6f, 46f + (chatLog.size - 1 - i) * 18f)
        }
        batch.end()
    }

    private fun handleInput() {
        // Menu / shop / bag.
        if (Gdx.input.isKeyJustPressed(Keys.M)) {
            game.setScreen(UiScreen(game, "gamemenu") { game.setScreen(MapScreen(game)) }); return
        }
        if (Gdx.input.isKeyJustPressed(Keys.P)) { game.setScreen(ShopScreen(game)); return }
        if (Gdx.input.isKeyJustPressed(Keys.B)) { game.setScreen(PetsScreen(game)); return }
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
