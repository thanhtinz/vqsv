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
import com.vqsv.core.asset.SpriteAnimator
import com.vqsv.core.model.GameState
import com.vqsv.core.net.PacketListener

class BattleScreen(private val game: VqsvGame) : Screen, PacketListener {

    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val worldCam = OrthographicCamera()   // sprites (y-down)
    private val hudCam = OrthographicCamera()      // bars/text (y-up)
    private var enemyAnim: SpriteAnimator? = null  // real monster sprite, if assets present
    private var playerAnim: SpriteAnimator? = null // player's active pet sprite

    private var selectedAction = 0  // 0=Attack 1=UseItem 2=Catch 3=Run
    private var waitingForServer = false
    private var playerHp = GameState.battlePlayerHp
    private var enemyHp = GameState.battleEnemyHp

    private val actionLabels = arrayOf("Tan cong", "Dung do", "Bat thu", "Bo chay")

    override fun show() {
        game.tcp.listener = this
        playerHp = GameState.battlePlayerHp
        enemyHp = GameState.battleEnemyHp
        resize(Gdx.graphics.width, Gdx.graphics.height)
        if (!GameAssets.available()) return
        val sid = GameState.battleEnemySpriteId
        if (sid >= 0) GameAssets.sprite(sid)?.let { enemyAnim = SpriteAnimator(it) }

        // Load the player's active pet sprite (slot 0) asynchronously.
        if (GameState.token.isNotEmpty()) {
            game.rest.getMyPets(GameState.token) { pets, _ ->
                val active = pets?.minByOrNull { it.slot } ?: pets?.firstOrNull()
                if (active != null) Gdx.app.postRunnable {
                    GameState.playerPetSpriteId = active.spriteId
                    GameAssets.sprite(active.spriteId)?.let { playerAnim = SpriteAnimator(it) }
                }
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        val maxHp = GameState.hpMax.coerceAtLeast(1)
        val enemyMaxHp = GameState.battleEnemyHp.coerceAtLeast(1)
        val barW = sw * 0.6f
        val barH = 20f

        // Real sprites (y-down world space): enemy top-right, your pet bottom-left (mirrored).
        if (enemyAnim != null || playerAnim != null) {
            batch.projectionMatrix = worldCam.combined
            batch.begin()
            enemyAnim?.let { it.update(delta); it.draw(batch, sw * 0.66f, sh * 0.28f) }
            playerAnim?.let { it.update(delta); it.draw(batch, sw * 0.30f, sh * 0.60f, mirror = true) }
            batch.end()
        }

        shapeRenderer.projectionMatrix = hudCam.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Enemy HP bar (red) near top
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.rect(sw * 0.2f, sh - 60f, barW, barH)
        shapeRenderer.color = Color.RED
        val enemyFrac = enemyHp.toFloat() / enemyMaxHp
        shapeRenderer.rect(sw * 0.2f, sh - 60f, barW * enemyFrac.coerceIn(0f, 1f), barH)

        // Player HP bar (green) above action buttons
        val btnAreaTop = sh * 0.4f
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.rect(sw * 0.2f, btnAreaTop + 10f, barW, barH)
        shapeRenderer.color = Color.GREEN
        val playerFrac = playerHp.toFloat() / maxHp
        shapeRenderer.rect(sw * 0.2f, btnAreaTop + 10f, barW * playerFrac.coerceIn(0f, 1f), barH)

        // Action buttons 2x2 grid
        val btnW = sw * 0.35f
        val btnH = 50f
        val btnPad = 10f
        val gridLeft = sw * 0.1f
        val gridBottom = 20f

        for (i in 0..3) {
            val col = i % 2
            val row = i / 2
            val bx = gridLeft + col * (btnW + btnPad)
            val by = gridBottom + row * (btnH + btnPad)
            shapeRenderer.color = if (i == selectedAction) Color.BLUE else Color.DARK_GRAY
            shapeRenderer.rect(bx, by, btnW, btnH)
        }

        shapeRenderer.end()

        batch.projectionMatrix = hudCam.combined
        batch.begin()
        font.color = Color.WHITE

        // Enemy name/level/hp
        font.draw(batch, "${GameState.battleEnemyName} Lv.${GameState.battleEnemyLevel}  HP: $enemyHp",
            sw * 0.2f, sh - 65f)

        // Player HP text
        font.draw(batch, "HP: $playerHp / $maxHp", sw * 0.2f, btnAreaTop + 40f)

        // Battle log (last 5 lines)
        val logs = GameState.battleLog.takeLast(5)
        val logStartY = sh * 0.75f
        logs.forEachIndexed { idx, line ->
            font.draw(batch, line, sw * 0.05f, logStartY - idx * 18f)
        }

        // Action button labels
        for (i in 0..3) {
            val col = i % 2
            val row = i / 2
            val bx = gridLeft + col * (btnW + btnPad) + 8f
            val by = gridBottom + row * (btnH + btnPad) + btnH * 0.6f
            font.color = Color.WHITE
            font.draw(batch, actionLabels[i], bx, by)
        }

        // Hint
        font.color = Color.LIGHT_GRAY
        font.draw(batch, if (waitingForServer) "Dang cho server..." else "Mui ten chon, ENTER/SPACE xac nhan",
            5f, sh - 5f)

        batch.end()
    }

    private fun handleInput() {
        if (waitingForServer) return
        if (Gdx.input.isKeyJustPressed(Keys.LEFT))  selectedAction = ((selectedAction - 1 + 4) % 4)
        if (Gdx.input.isKeyJustPressed(Keys.RIGHT)) selectedAction = (selectedAction + 1) % 4
        if (Gdx.input.isKeyJustPressed(Keys.UP))    selectedAction = ((selectedAction - 2 + 4) % 4)
        if (Gdx.input.isKeyJustPressed(Keys.DOWN))  selectedAction = (selectedAction + 2) % 4
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            doAction()
        }
    }

    private fun doAction() {
        val battleId = GameState.currentBattleId ?: return
        game.tcp.sendBattleAct(battleId, selectedAction)
        waitingForServer = true
    }

    override fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String) {
        this.playerHp = playerHp
        this.enemyHp = enemyHp
        GameState.battlePlayerHp = playerHp
        GameState.battleEnemyHp = enemyHp
        GameState.battleLog.add(log)
        waitingForServer = false

        val endStatuses = setOf("VICTORY", "DEFEAT", "ESCAPED", "CAUGHT")
        if (status in endStatuses) {
            GameState.clearBattle()
            Gdx.app.postRunnable {
                game.setScreen(MapScreen(game))
                dispose()
            }
        }
    }

    override fun onAuthOk(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int) {}
    override fun onMoveOk(x: Int, y: Int) {}
    override fun onWildEncounter(x: Int, y: Int, battleId: String, name: String, level: Int, hp: Int, catchable: Boolean, spriteId: Int) {}
    override fun onPong() {}
    override fun onError(msg: String) {
        waitingForServer = false
        GameState.battleLog.add("Loi: $msg")
    }

    override fun resize(width: Int, height: Int) {
        worldCam.setToOrtho(true, width.toFloat(), height.toFloat())
        hudCam.setToOrtho(false, width.toFloat(), height.toFloat())
    }
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        font.dispose()
    }
}
