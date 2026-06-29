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
import com.vqsv.core.net.RestClient

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

    // --- item picker (for Use-item / Catch, which need an itemId) ---
    private var pickingItem = false
    private var pickAction = 0
    private var invItems: List<RestClient.InventoryItem> = emptyList()
    private var invSelected = 0

    // --- skill picker (action 4 = SKILL; needs a skillId) ---
    private var pickingSkill = false
    private var skills: List<RestClient.SkillInfo> = emptyList()
    private var skillSelected = 0

    // --- party / pet-switch (action 5 = SWITCH; sends the target slot) ---
    private var party: List<RestClient.PetInfo> = emptyList()
    private var activePetId: Long = -1L
    private var pickingSwitch = false
    private var switchSelected = 0
    private fun switchable() = party.filter { (it.id as? Number)?.toLong() != activePetId && it.hp > 0 }
    private var playerHp = GameState.battlePlayerHp
    private var enemyHp = GameState.battleEnemyHp
    // Captured once at battle start; battleEnemyHp itself is later overwritten with
    // the enemy's CURRENT hp each turn, so the bar must use this fixed maximum.
    private var enemyMaxHp = GameState.battleEnemyHp.coerceAtLeast(1)

    private val actionLabels = arrayOf("Tan cong", "Dung do", "Bat thu", "Bo chay")

    override fun show() {
        game.tcp.listener = this
        playerHp = GameState.battlePlayerHp
        enemyHp = GameState.battleEnemyHp
        enemyMaxHp = GameState.battleEnemyHp.coerceAtLeast(1)
        resize(Gdx.graphics.width, Gdx.graphics.height)

        // Enemy sprite (needs converted assets).
        if (GameAssets.available()) {
            val sid = GameState.battleEnemySpriteId
            if (sid >= 0) GameAssets.sprite(sid)?.let { enemyAnim = SpriteAnimator(it) }
        }

        // Load the player's party; the active pet (lowest slot) drives the sprite +
        // skill menu. (PvP combat resolves server-side without skills/switch for now.)
        if (GameState.token.isNotEmpty()) {
            game.rest.getMyPets(GameState.token) { pets, _ ->
                party = pets ?: emptyList()
                val active = party.minByOrNull { it.slot } ?: party.firstOrNull() ?: return@getMyPets
                loadActivePet(active)
            }
        }
    }

    /** Point the on-screen sprite + skill menu at [pet] (the new active pet). */
    private fun loadActivePet(pet: RestClient.PetInfo) {
        val petId = (pet.id as? Number)?.toLong() ?: return
        activePetId = petId
        Gdx.app.postRunnable {
            GameState.playerPetSpriteId = pet.spriteId
            if (GameAssets.available()) GameAssets.sprite(pet.spriteId)?.let { playerAnim = SpriteAnimator(it) }
        }
        // Skills are available in PvE and PvP (both use the active pet's moves).
        game.rest.getPetSkills(GameState.token, petId) { sk, _ ->
            Gdx.app.postRunnable { skills = sk ?: emptyList() }
        }
    }

    /** Re-read the party and re-point at the active pet (after an auto send-next). */
    private fun refreshActivePet() {
        if (GameState.token.isEmpty()) return
        game.rest.getMyPets(GameState.token) { pets, _ ->
            party = pets ?: emptyList()
            party.filter { it.hp > 0 }.minByOrNull { it.slot }?.let { loadActivePet(it) }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        val maxHp = GameState.hpMax.coerceAtLeast(1)
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
        font.draw(batch, when {
            waitingForServer -> "Dang cho server..."
            !GameState.battleIsPvp -> "ENTER xac nhan | S: Ky nang | C: Doi pet"
            else -> "ENTER xac nhan | S: Ky nang"
        }, 5f, sh - 5f)

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

        if (pickingItem) drawItemPicker(sw, sh)
        if (pickingSkill) drawSkillPicker(sw, sh)
        if (pickingSwitch) drawSwitchPicker(sw, sh)
    }

    private fun drawItemPicker(sw: Float, sh: Float) {
        val rowH = 30f
        val boxX = sw * 0.15f; val boxW = sw * 0.7f
        val boxTop = sh * 0.7f
        shapeRenderer.projectionMatrix = hudCam.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.85f)
        shapeRenderer.rect(boxX, boxTop - invItems.size * rowH - 10f, boxW, invItems.size * rowH + 40f)
        invItems.forEachIndexed { i, _ ->
            val y = boxTop - i * rowH
            shapeRenderer.color = if (i == invSelected) Color(0.2f, 0.3f, 0.6f, 1f) else Color(0.15f, 0.15f, 0.18f, 1f)
            shapeRenderer.rect(boxX + 6f, y - rowH + 6f, boxW - 12f, rowH - 6f)
        }
        shapeRenderer.end()

        batch.projectionMatrix = hudCam.combined
        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, if (pickAction == 2) "Chon Tat de bat:" else "Chon thuoc:", boxX + 8f, boxTop + 28f)
        font.color = Color.WHITE
        invItems.forEachIndexed { i, it ->
            val y = boxTop - i * rowH
            font.draw(batch, "${it.name} x${it.quantity}", boxX + 14f, y - 8f)
        }
        batch.end()
    }

    private fun handleInput() {
        if (pickingItem) { handlePicker(); return }
        if (pickingSkill) { handleSkillPicker(); return }
        if (pickingSwitch) { handleSwitchPicker(); return }
        if (waitingForServer) return
        // S opens the skill menu (works in PvE and PvP).
        if (Gdx.input.isKeyJustPressed(Keys.S)) {
            if (skills.isEmpty()) GameState.battleLog.add("Chua hoc ky nang nao")
            else { skillSelected = 0; pickingSkill = true }
            return
        }
        // C opens the pet-switch menu (PvE only).
        if (Gdx.input.isKeyJustPressed(Keys.C) && !GameState.battleIsPvp) {
            if (switchable().isEmpty()) GameState.battleLog.add("Khong co pet de doi")
            else { switchSelected = 0; pickingSwitch = true }
            return
        }
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
        // In PvP only Attack and Run(=forfeit) are allowed (no catch / item).
        if (GameState.battleIsPvp && (selectedAction == 1 || selectedAction == 2)) {
            GameState.battleLog.add("Khong the dung trong PvP")
            return
        }
        when (selectedAction) {
            0 -> { game.tcp.sendBattleAct(battleId, 0); waitingForServer = true; playerAttackTimer = 0.45f }
            3 -> { game.tcp.sendBattleAct(battleId, 3); waitingForServer = true }
            1 -> openItemPicker(1, "MEDICINE")
            2 -> openItemPicker(2, "CATCH_BALL")
        }
    }

    private fun openItemPicker(action: Int, type: String) {
        pickAction = action
        game.rest.getInventory(GameState.token) { list, err ->
            Gdx.app.postRunnable {
                if (err != null) { GameState.battleLog.add("Loi tui do: $err"); return@postRunnable }
                invItems = (list ?: emptyList()).filter { it.itemType == type && it.quantity > 0 }
                invSelected = 0
                if (invItems.isEmpty())
                    GameState.battleLog.add(if (action == 2) "Khong co Tat de bat!" else "Khong co thuoc!")
                else pickingItem = true
            }
        }
    }

    private fun handlePicker() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) { pickingItem = false; return }
        if (invItems.isEmpty()) { pickingItem = false; return }
        if (Gdx.input.isKeyJustPressed(Keys.UP)) invSelected = (invSelected - 1 + invItems.size) % invItems.size
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)) invSelected = (invSelected + 1) % invItems.size
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            val item = invItems.getOrNull(invSelected) ?: return
            val battleId = GameState.currentBattleId ?: return
            game.tcp.sendBattleAct(battleId, pickAction, item.itemId)
            waitingForServer = true
            pickingItem = false
        }
    }

    private fun handleSkillPicker() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) { pickingSkill = false; return }
        if (skills.isEmpty()) { pickingSkill = false; return }
        if (Gdx.input.isKeyJustPressed(Keys.UP)) skillSelected = (skillSelected - 1 + skills.size) % skills.size
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)) skillSelected = (skillSelected + 1) % skills.size
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            val skill = skills.getOrNull(skillSelected) ?: return
            val battleId = GameState.currentBattleId ?: return
            // Action 4 = SKILL; the optional id field carries the skill id.
            game.tcp.sendBattleAct(battleId, 4, skill.id)
            waitingForServer = true
            pickingSkill = false
            playerAttackTimer = 0.45f
        }
    }

    private fun handleSwitchPicker() {
        val list = switchable()
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) { pickingSwitch = false; return }
        if (list.isEmpty()) { pickingSwitch = false; return }
        if (Gdx.input.isKeyJustPressed(Keys.UP)) switchSelected = (switchSelected - 1 + list.size) % list.size
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)) switchSelected = (switchSelected + 1) % list.size
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            val pet = list.getOrNull(switchSelected) ?: return
            val battleId = GameState.currentBattleId ?: return
            // Action 5 = SWITCH; the optional field carries the target slot.
            game.tcp.sendBattleAct(battleId, 5, pet.slot)
            loadActivePet(pet)            // we know the new pet -> refresh sprite + skills now
            waitingForServer = true
            pickingSwitch = false
        }
    }

    private fun drawSwitchPicker(sw: Float, sh: Float) {
        val list = switchable()
        val rowH = 26f
        val boxW = sw * 0.7f
        val boxX = sw * 0.15f
        val boxTop = sh * 0.62f
        shapeRenderer.projectionMatrix = hudCam.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.85f)
        shapeRenderer.rect(boxX, boxTop - list.size * rowH - 10f, boxW, list.size * rowH + 40f)
        list.forEachIndexed { i, _ ->
            if (i == switchSelected) {
                shapeRenderer.color = Color(0.2f, 0.5f, 0.3f, 1f)
                shapeRenderer.rect(boxX, boxTop - i * rowH - rowH, boxW, rowH)
            }
        }
        shapeRenderer.end()
        batch.projectionMatrix = hudCam.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "Doi pet (C/ESC dong):", boxX + 8f, boxTop + 24f)
        list.forEachIndexed { i, p ->
            val y = boxTop - i * rowH
            font.draw(batch, "${p.name}  Lv.${p.level}  HP:${p.hp}/${p.hpMax}", boxX + 14f, y - 8f)
        }
        batch.end()
    }

    private fun drawSkillPicker(sw: Float, sh: Float) {
        val rowH = 26f
        val boxW = sw * 0.7f
        val boxX = sw * 0.15f
        val boxTop = sh * 0.62f
        shapeRenderer.projectionMatrix = hudCam.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.85f)
        shapeRenderer.rect(boxX, boxTop - skills.size * rowH - 10f, boxW, skills.size * rowH + 40f)
        skills.forEachIndexed { i, _ ->
            if (i == skillSelected) {
                shapeRenderer.color = Color(0.2f, 0.3f, 0.6f, 1f)
                shapeRenderer.rect(boxX, boxTop - i * rowH - rowH, boxW, rowH)
            }
        }
        shapeRenderer.end()
        batch.projectionMatrix = hudCam.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "Ky nang (S/ESC dong):", boxX + 8f, boxTop + 24f)
        skills.forEachIndexed { i, sk ->
            val y = boxTop - i * rowH
            font.draw(batch, "${sk.name}  SP:${sk.spCost}  Pow:${if (sk.power == 0) 100 else sk.power}%", boxX + 14f, y - 8f)
        }
        batch.end()
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

        // Auto send-next on a faint (server-driven): re-point sprite + skills at the
        // new active pet. (Voluntary switch already refreshed locally.)
        if (log.contains("ra trận") && !GameState.battleIsPvp) refreshActivePet()

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
    override fun onChat(name: String, text: String) {}
    override fun onPlayerNear(playerId: Long, present: Boolean, mapId: Int, x: Int, y: Int, name: String) {}
    override fun onPvpInvite(challengerId: Long, name: String) {}
    override fun onPvpStart(battleId: String, oppName: String, myHp: Int, oppHp: Int, oppSpriteId: Int) {}
    override fun onEnemySwap(name: String, hpMax: Int, spriteId: Int) {
        // Trainer summoned its next enemy: reset the enemy display (name, HP bar, sprite).
        enemyMaxHp = hpMax.coerceAtLeast(1)
        enemyHp = hpMax
        GameState.battleEnemyName = name
        GameState.battleEnemyHp = hpMax
        GameState.battleEnemySpriteId = spriteId
        Gdx.app.postRunnable {
            if (GameAssets.available() && spriteId >= 0) GameAssets.sprite(spriteId)?.let { enemyAnim = SpriteAnimator(it) }
        }
    }
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
