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

    // --- battle FX ---
    private class FloatText(var x: Float, var y: Float, val text: String, val color: Color, var ttl: Float = 1.1f)
    private val floats = ArrayList<FloatText>()
    private var enemyShake = 0f
    private var playerShake = 0f
    private var playerAttackTimer = 0f
    private val ATTACK_ANIM = 1   // anim index used as the "attack" action when present

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

        // Advance FX timers.
        if (enemyShake > 0f) enemyShake -= delta
        if (playerShake > 0f) playerShake -= delta
        if (playerAttackTimer > 0f) playerAttackTimer -= delta
        val enemyDX = if (enemyShake > 0f) (Math.sin(enemyShake.toDouble() * 60).toFloat() * 5f) else 0f
        val playerDX = if (playerShake > 0f) (Math.sin(playerShake.toDouble() * 60).toFloat() * 5f) else 0f
        playerAnim?.setAnim(if (playerAttackTimer > 0f) ATTACK_ANIM else 0)

        // Real sprites (y-down world space): enemy top-right, your pet bottom-left (mirrored).
        if (enemyAnim != null || playerAnim != null) {
            batch.projectionMatrix = worldCam.combined
            batch.begin()
            enemyAnim?.let { it.update(delta); it.draw(batch, sw * 0.66f + enemyDX, sh * 0.28f) }
            playerAnim?.let { it.update(delta); it.draw(batch, sw * 0.30f + playerDX, sh * 0.60f, mirror = true) }
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

        // Floating damage numbers (rise + fade).
        val it = floats.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.ttl -= delta
            if (f.ttl <= 0f) { it.remove(); continue }
            f.y += delta * 40f
            font.color = f.color.cpy().apply { a = (f.ttl / 1.1f).coerceIn(0f, 1f) }
            font.draw(batch, f.text, f.x, f.y)
        }
        font.color = Color.WHITE

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
        if (selectedAction == 0) playerAttackTimer = 0.45f   // play attack animation
    }

    override fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String) {
        val dmgEnemy = this.enemyHp - enemyHp     // damage dealt to enemy this turn
        val dmgPlayer = this.playerHp - playerHp  // damage taken by player this turn
        this.playerHp = playerHp
        this.enemyHp = enemyHp
        GameState.battlePlayerHp = playerHp
        GameState.battleEnemyHp = enemyHp
        GameState.battleLog.add(log)
        waitingForServer = false

        // Battle FX: floating damage + shake (rendered on the GL thread).
        Gdx.app.postRunnable {
            val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
            if (dmgEnemy > 0) { floats.add(FloatText(sw * 0.66f, sh * 0.45f, "-$dmgEnemy", Color.SCARLET)); enemyShake = 0.35f }
            if (dmgPlayer > 0) { floats.add(FloatText(sw * 0.30f, sh * 0.30f, "-$dmgPlayer", Color.YELLOW)); playerShake = 0.35f }
        }

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
