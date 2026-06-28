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
import com.vqsv.core.asset.UiLayout
import com.vqsv.core.asset.UiNode
import com.vqsv.core.asset.UiRenderer

/**
 * Renders an original-game UI layout (game/ui/<name>.json) faithfully to its
 * pixel coordinates. Reusable base for the menu/shop/bag/title screens.
 *
 * @param uiName  the .ui name without extension (e.g. "gamemenu", "bag", "bodyShop")
 * @param onBack  invoked on the back key (ESC / BACK); defaults to returning to the map
 */
class UiScreen(
    private val game: VqsvGame,
    private val uiName: String,
    private val onBack: (() -> Unit)? = null
) : Screen {

    private val shape = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val cam = OrthographicCamera()
    private var root: UiNode? = null

    override fun show() {
        resize(Gdx.graphics.width, Gdx.graphics.height)
        root = UiLayout.load(uiName)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.07f, 0.10f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) {
            (onBack ?: { game.setScreen(MapScreen(game)) }).invoke()
            return
        }

        val r = root
        if (r != null) {
            shape.projectionMatrix = cam.combined
            shape.begin(ShapeRenderer.ShapeType.Filled)
            UiRenderer.renderBackgrounds(shape, r)
            shape.end()

            batch.projectionMatrix = cam.combined
            batch.begin()
            UiRenderer.renderText(batch, font, r)
            batch.end()
        } else {
            batch.projectionMatrix = cam.combined
            batch.begin()
            font.color = Color.WHITE
            font.draw(batch, "UI '$uiName' chua co asset", 20f, 30f)
            batch.end()
        }
    }

    override fun resize(width: Int, height: Int) {
        cam.setToOrtho(true, width.toFloat(), height.toFloat())   // y-down to match .ui coords
    }

    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { shape.dispose(); batch.dispose(); font.dispose() }
}
