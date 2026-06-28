package com.vqsv.core.asset

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader

/**
 * Loads the converted original-game assets bundled under `assets/game/`.
 *
 * The asset pipeline (`tools/asset-extractor/extract.py`) turns the original
 * J2ME JAR into:
 *   game/img/img_N.png    — texture atlases (verified, byte-exact)
 *   game/spr/spr_N.json   — sprite frame tables (header + frame stream)
 *   game/map/map_N.json   — tile maps
 *
 * Run it once to populate the folder:
 *   cd tools/asset-extractor
 *   python3 extract.py ../../assets/original/vqsv-original.jar ../../clients/core/assets/game
 *
 * If the folder is empty the loaders return null/empty and screens fall back to
 * the placeholder rendering, so the game still runs without assets present.
 */
object GameAssets {
    private val images = HashMap<Int, Texture>()
    private val json = JsonReader()

    /** Lazily load and cache an image atlas (img_N.png). Returns null if absent. */
    fun image(id: Int): Texture? = images.getOrPut(id) {
        val path = "game/img/img_$id.png"
        if (Gdx.files.internal(path).exists()) Texture(Gdx.files.internal(path))
        else return null
    }

    /** A sprite frame: a clip rectangle into the matching img_N atlas. */
    data class Frame(val x: Int, val y: Int, val w: Int, val h: Int)

    /**
     * Parse a sprite table (spr_N.json). The frame/module stream is engine
     * specific and only partially decoded upstream, so this returns the raw
     * header + stream for callers that want to refine slicing. See
     * docs/ASSET-FORMATS.md.
     */
    fun spriteStream(id: Int): IntArray {
        val path = "game/spr/spr_$id.json"
        if (!Gdx.files.internal(path).exists()) return IntArray(0)
        val root = json.parse(Gdx.files.internal(path))
        val stream = root.get("stream") ?: return IntArray(0)
        return IntArray(stream.size) { stream.get(it).asInt() }
    }

    /** Build a TextureRegion clip from an atlas, if the atlas exists. */
    fun region(imgId: Int, frame: Frame): TextureRegion? {
        val tex = image(imgId) ?: return null
        return TextureRegion(tex, frame.x, frame.y, frame.w, frame.h)
    }

    fun isLoaded(): Boolean = Gdx.files.internal("game/img/img_0.png").exists()

    fun dispose() {
        images.values.forEach { it.dispose() }
        images.clear()
    }
}
