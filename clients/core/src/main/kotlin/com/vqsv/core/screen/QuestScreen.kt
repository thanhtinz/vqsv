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
import com.vqsv.core.model.GameState
import com.vqsv.core.net.RestClient

/** Quest log — lists the player's quests from /api/quests and claims finished ones. */
class QuestScreen(private val game: VqsvGame) : Screen {

    private val shape = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val cam = OrthographicCamera()

    private var quests: List<RestClient.QuestInfo> = emptyList()
    private var selected = 0
    private var message = "Dang tai nhiem vu..."

    override fun show() {
        resize(Gdx.graphics.width, Gdx.graphics.height)
        reload()
    }

    private fun reload() {
        game.rest.getQuests(GameState.token) { list, err ->
            Gdx.app.postRunnable {
                if (err != null) { message = "Loi: $err"; return@postRunnable }
                quests = list ?: emptyList()
                if (selected >= quests.size) selected = 0
                message = "ENTER nhan thuong (khi hoan thanh) | UP/DOWN chon | ESC thoat"
            }
        }
    }

    private fun statusLabel(s: String) = when (s) {
        "COMPLETED" -> "HOAN THANH"
        "CLAIMED" -> "DA NHAN"
        "IN_PROGRESS" -> "DANG LAM"
        else -> s
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.07f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        handleInput()

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val rowH = 48f
        val top = sh - 70f

        shape.projectionMatrix = cam.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        quests.forEachIndexed { i, _ ->
            val y = top - i * rowH
            shape.color = if (i == selected) Color(0.2f, 0.4f, 0.3f, 1f) else Color(0.12f, 0.14f, 0.13f, 1f)
            shape.rect(sw * 0.06f, y - rowH + 8f, sw * 0.88f, rowH - 8f)
        }
        shape.end()

        batch.projectionMatrix = cam.combined
        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, "NHIEM VU", sw * 0.06f, sh - 30f)
        font.color = Color.WHITE
        quests.forEachIndexed { i, q ->
            val y = top - i * rowH
            font.color = when (q.status) {
                "COMPLETED" -> Color.LIME
                "CLAIMED" -> Color.GRAY
                else -> Color.WHITE
            }
            font.draw(batch, "${q.name}  [${statusLabel(q.status)}]", sw * 0.08f, y - 8f)
            font.color = Color.LIGHT_GRAY
            val obj = "${q.objectiveType}  ${q.progress}/${q.objectiveCount}  -  ${q.rewardGold} xu, ${q.rewardExp} EXP"
            font.draw(batch, obj, sw * 0.08f, y - 26f)
        }
        if (quests.isEmpty()) {
            font.color = Color.LIGHT_GRAY
            font.draw(batch, "Chua co nhiem vu. Hay den noi chuyen voi NPC (phim E).", sw * 0.06f, sh * 0.6f)
        }
        font.color = Color.LIGHT_GRAY
        font.draw(batch, message, sw * 0.06f, 28f)
        batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) {
            game.setScreen(MapScreen(game)); return
        }
        if (quests.isEmpty()) return
        if (Gdx.input.isKeyJustPressed(Keys.UP)) selected = (selected - 1 + quests.size) % quests.size
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)) selected = (selected + 1) % quests.size
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) claim()
    }

    private fun claim() {
        val q = quests.getOrNull(selected) ?: return
        if (q.status != "COMPLETED") { message = "Nhiem vu chua hoan thanh"; return }
        message = "Dang nhan thuong..."
        game.rest.claimQuest(GameState.token, q.id) { ok, err ->
            Gdx.app.postRunnable {
                if (ok) {
                    message = "Da nhan thuong \"${q.name}\"!"
                    game.rest.getMyPlayer(GameState.token) { p, _ ->
                        p?.let { Gdx.app.postRunnable { GameState.kimTien = it.kimTien; GameState.level = it.level } }
                    }
                    reload()
                } else message = "Loi: $err"
            }
        }
    }

    override fun resize(width: Int, height: Int) { cam.setToOrtho(false, width.toFloat(), height.toFloat()) }
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { shape.dispose(); batch.dispose(); font.dispose() }
}
