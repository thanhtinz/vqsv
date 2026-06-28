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
import com.vqsv.core.net.RestClient

/** Bag / pet list — shows the player's pets with their real sprites, evolve via /api/pets. */
class PetsScreen(private val game: VqsvGame) : Screen {

    private val shape = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val hudCam = OrthographicCamera()
    private val worldCam = OrthographicCamera()

    private var pets: List<RestClient.PetInfo> = emptyList()
    private val anims = HashMap<Int, SpriteAnimator?>()
    private var selected = 0
    private var message = "Dang tai..."

    override fun show() {
        resize(Gdx.graphics.width, Gdx.graphics.height)
        load()
    }

    private fun load() {
        game.rest.getMyPets(GameState.token) { list, err ->
            Gdx.app.postRunnable {
                if (err != null) { message = "Loi: $err"; return@postRunnable }
                pets = list ?: emptyList()
                message = if (pets.isEmpty()) "Chua co sung vat" else "ENTER tien hoa | UP/DOWN chon | ESC thoat"
            }
        }
    }

    private fun animFor(spriteId: Int): SpriteAnimator? = anims.getOrPut(spriteId) {
        if (GameAssets.available()) GameAssets.sprite(spriteId)?.let { SpriteAnimator(it) } else null
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.07f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        handleInput()

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val rowH = 56f
        val top = sh - 70f

        shape.projectionMatrix = hudCam.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        pets.forEachIndexed { i, _ ->
            val y = top - i * rowH
            shape.color = if (i == selected) Color(0.2f, 0.45f, 0.3f, 1f) else Color(0.12f, 0.15f, 0.13f, 1f)
            shape.rect(sw * 0.06f, y - rowH + 8f, sw * 0.88f, rowH - 8f)
        }
        shape.end()

        // Pet sprites (y-down) on the left of each row.
        if (GameAssets.available()) {
            batch.projectionMatrix = worldCam.combined
            batch.begin()
            pets.forEachIndexed { i, p ->
                val yTopDown = (70f + i * rowH) + rowH * 0.5f
                animFor(p.spriteId)?.let { it.update(delta); it.draw(batch, sw * 0.12f, yTopDown) }
            }
            batch.end()
        }

        batch.projectionMatrix = hudCam.combined
        batch.begin()
        font.color = Color.CYAN
        font.draw(batch, "SUNG VAT CUA BAN", sw * 0.06f, sh - 28f)
        font.color = Color.WHITE
        pets.forEachIndexed { i, p ->
            val y = top - i * rowH
            font.draw(batch, "${p.name}  Lv.${p.level}  [${p.element}]", sw * 0.20f, y - 12f)
            font.draw(batch, "HP ${p.hp}/${p.hpMax}  ATK ${p.atk} DEF ${p.def} SPD ${p.spd}", sw * 0.20f, y - 30f)
        }
        font.color = Color.LIGHT_GRAY
        font.draw(batch, message, sw * 0.06f, 28f)
        batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) {
            game.setScreen(MapScreen(game)); return
        }
        if (pets.isEmpty()) return
        if (Gdx.input.isKeyJustPressed(Keys.UP)) selected = (selected - 1 + pets.size) % pets.size
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)) selected = (selected + 1) % pets.size
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) evolve()
    }

    private fun evolve() {
        val pet = pets.getOrNull(selected) ?: return
        val petId = (pet.id as? Number)?.toLong() ?: return
        message = "Dang tien hoa ${pet.name}..."
        game.rest.evolvePet(GameState.token, petId) { info, err ->
            Gdx.app.postRunnable {
                message = if (err == null) "${pet.name} -> ${info?.name ?: "?"}!" else "Loi: $err"
                if (err == null) load()
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        hudCam.setToOrtho(false, width.toFloat(), height.toFloat())
        worldCam.setToOrtho(true, width.toFloat(), height.toFloat())
    }
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { shape.dispose(); batch.dispose(); font.dispose() }
}
