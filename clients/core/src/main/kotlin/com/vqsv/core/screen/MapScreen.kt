package com.vqsv.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.vqsv.core.VqsvGame
import com.vqsv.core.model.GameState
import com.vqsv.core.net.PacketListener

class MapScreen(private val game: VqsvGame) : Screen, PacketListener {

    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()

    private val TILE_SIZE = 32f
    private val MAP_COLS = 20
    private val MAP_ROWS = 12

    private var moveCooldown = 0f
    private val MOVE_DELAY = 0.2f

    override fun show() {
        game.tcp.listener = this
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        moveCooldown -= delta
        handleInput()

        // Draw tile grid
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (row in 0 until MAP_ROWS) {
            for (col in 0 until MAP_COLS) {
                val isAlt = (row + col) % 2 == 0
                shapeRenderer.color = if (isAlt) Color(0.2f, 0.6f, 0.2f, 1f) else Color(0.15f, 0.5f, 0.15f, 1f)
                shapeRenderer.rect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE)
            }
        }
        // Draw player as cyan rect
        shapeRenderer.color = Color.CYAN
        val px = GameState.posX * TILE_SIZE
        val py = (MAP_ROWS - 1 - GameState.posY) * TILE_SIZE
        shapeRenderer.rect(px, py, TILE_SIZE, TILE_SIZE)
        shapeRenderer.end()

        // HUD
        batch.begin()
        val screenH = Gdx.graphics.height.toFloat()
        font.color = Color.WHITE
        font.draw(batch, "${GameState.playerName} | Lv.${GameState.level} | HP:${GameState.hp}/${GameState.hpMax} | Xu:${GameState.kimTien}",
            5f, screenH - 5f)
        font.draw(batch, "Di chuyen: WASD hoac phim mui ten", 5f, 20f)
        batch.end()
    }

    private fun handleInput() {
        if (moveCooldown > 0f) return
        when {
            Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP) -> {
                game.tcp.sendMove(0)
                moveCooldown = MOVE_DELAY
            }
            Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN) -> {
                game.tcp.sendMove(1)
                moveCooldown = MOVE_DELAY
            }
            Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT) -> {
                game.tcp.sendMove(2)
                moveCooldown = MOVE_DELAY
            }
            Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT) -> {
                game.tcp.sendMove(3)
                moveCooldown = MOVE_DELAY
            }
        }
    }

    override fun onMoveOk(x: Int, y: Int) {
        GameState.posX = x
        GameState.posY = y
    }

    override fun onWildEncounter(x: Int, y: Int, battleId: String, name: String, level: Int, hp: Int, catchable: Boolean) {
        GameState.currentBattleId = battleId
        GameState.battleEnemyName = name
        GameState.battleEnemyLevel = level
        GameState.battleEnemyHp = hp
        GameState.battleCatchable = catchable
        GameState.battlePlayerHp = GameState.hp
        Gdx.app.postRunnable {
            game.setScreen(BattleScreen(game))
        }
    }

    override fun onAuthOk(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int) {}
    override fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String) {}
    override fun onPong() {}
    override fun onError(msg: String) {}

    override fun resize(width: Int, height: Int) {}
    override fun show() {}
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        font.dispose()
    }
}
