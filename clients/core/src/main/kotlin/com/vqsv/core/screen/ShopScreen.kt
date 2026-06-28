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

/** In-game shop — lists listings from /api/shop and buys with gold via /api/shop/buy. */
class ShopScreen(private val game: VqsvGame) : Screen {

    private val shape = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val cam = OrthographicCamera()

    private var items: List<RestClient.ShopItem> = emptyList()
    private var selected = 0
    private var message = "Dang tai cua hang..."

    override fun show() {
        resize(Gdx.graphics.width, Gdx.graphics.height)
        game.rest.getShop { list, err ->
            Gdx.app.postRunnable {
                if (err != null) { message = "Loi: $err"; return@postRunnable }
                items = list ?: emptyList()
                message = "ENTER mua | UP/DOWN chon | ESC thoat"
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.06f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        handleInput()

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val rowH = 34f
        val top = sh - 60f

        shape.projectionMatrix = cam.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        items.forEachIndexed { i, _ ->
            val y = top - i * rowH
            shape.color = if (i == selected) Color(0.2f, 0.3f, 0.6f, 1f) else Color(0.13f, 0.13f, 0.16f, 1f)
            shape.rect(sw * 0.08f, y - rowH + 6f, sw * 0.84f, rowH - 6f)
        }
        shape.end()

        batch.projectionMatrix = cam.combined
        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, "CUA HANG", sw * 0.08f, sh - 24f)
        font.draw(batch, "Vang: ${GameState.kimTien}", sw * 0.6f, sh - 24f)
        font.color = Color.WHITE
        items.forEachIndexed { i, it ->
            val y = top - i * rowH
            val price = it.priceGold?.let { p -> "$p vang" } ?: it.priceMedal?.let { m -> "$m huy chuong" } ?: "?"
            font.draw(batch, "${it.itemName}  -  $price", sw * 0.10f, y - 8f)
        }
        font.color = Color.LIGHT_GRAY
        font.draw(batch, message, sw * 0.08f, 28f)
        batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) {
            game.setScreen(MapScreen(game)); return
        }
        if (items.isEmpty()) return
        if (Gdx.input.isKeyJustPressed(Keys.UP)) selected = (selected - 1 + items.size) % items.size
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)) selected = (selected + 1) % items.size
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) buy()
    }

    private fun buy() {
        val item = items.getOrNull(selected) ?: return
        message = "Dang mua ${item.itemName}..."
        game.rest.buyItem(GameState.token, item.id, 1) { ok, err ->
            Gdx.app.postRunnable {
                message = if (ok) "Da mua ${item.itemName}!" else "Loi: $err"
                if (ok) game.rest.getMyPlayer(GameState.token) { p, _ ->
                    p?.let { Gdx.app.postRunnable { GameState.kimTien = it.kimTien } }
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) { cam.setToOrtho(false, width.toFloat(), height.toFloat()) }
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { shape.dispose(); batch.dispose(); font.dispose() }
}
