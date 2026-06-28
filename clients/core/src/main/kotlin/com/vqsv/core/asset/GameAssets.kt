package com.vqsv.core.asset

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import java.util.Base64
import java.util.zip.ZipInputStream

/**
 * Loads the original-game assets that are BAKED INTO THE REPO — no external JAR
 * needed. The converted assets (PNG atlases + JSON metadata) are committed as
 * base64-chunked zip parts under assets/ (game.pack.NNN.b64); on first use they
 * are concatenated, base64-decoded and unzipped into memory.
 *
 * Pack entries (paths inside the zip):
 *   png/img/img_<id>.png        texture atlases
 *   spr/spr_<id>.json           sprite tables (modules/frames/anims)
 *   map/map_<id>.json           tile maps
 *   mod/mod_<t>.json            tileset tile rects
 *   meta/sprite_table.json      sprite id -> (sprFileId, imgId...)
 *   meta/modInfo.json           tileset id -> (imgId...)
 *
 * Regenerate the pack from a new game JAR with:
 *   tools/asset-extractor/pack.sh   (extract -> zip -> base64 split)
 */
object GameAssets {
    private val json = JsonReader()

    private val blobs = HashMap<String, ByteArray>()      // zip entry name -> bytes
    private var loaded = false

    private val textures = HashMap<Int, Texture?>()
    private val sprites = HashMap<Int, GameSprite?>()
    private var spriteTable: JsonValue? = null
    private var modInfo: JsonValue? = null
    private val tilesets = HashMap<Int, JsonValue?>()

    /** Concatenate the base64 chunks, decode, and unzip into memory (idempotent). */
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val b64 = StringBuilder()
        var i = 0
        while (true) {
            val name = "game.pack.%03d.b64".format(i)
            val f = Gdx.files.internal(name)
            if (!f.exists()) break
            b64.append(f.readString("UTF-8"))
            i++
        }
        if (b64.isEmpty()) return
        try {
            val zipBytes = Base64.getDecoder().decode(b64.toString())
            ZipInputStream(zipBytes.inputStream()).use { zin ->
                var e = zin.nextEntry
                while (e != null) {
                    if (!e.isDirectory) blobs[e.name] = zin.readBytes()
                    zin.closeEntry()
                    e = zin.nextEntry
                }
            }
        } catch (ex: Exception) {
            Gdx.app?.error("GameAssets", "Failed to unpack asset bundle: ${ex.message}")
        }
    }

    fun available(): Boolean { ensureLoaded(); return blobs.containsKey("meta/sprite_table.json") }

    private fun bytes(name: String): ByteArray? { ensureLoaded(); return blobs[name] }

    private fun readJson(name: String): JsonValue? {
        val b = bytes(name) ?: return null
        return json.parse(String(b, Charsets.UTF_8))
    }

    fun atlas(imgId: Int): Texture? = textures.getOrPut(imgId) {
        val b = bytes("png/img/img_$imgId.png") ?: return@getOrPut null
        val pix = Pixmap(b, 0, b.size)
        Texture(pix).also { pix.dispose() }
    }

    private fun spriteTable(): JsonValue? {
        if (spriteTable == null) spriteTable = readJson("meta/sprite_table.json")
        return spriteTable
    }

    fun modInfo(): JsonValue? {
        if (modInfo == null) modInfo = readJson("meta/modInfo.json")
        return modInfo
    }

    fun tileset(tilesetId: Int): JsonValue? = tilesets.getOrPut(tilesetId) {
        readJson("mod/mod_$tilesetId.json")
    }

    fun spriteImageIds(spriteId: Int): IntArray {
        val table = spriteTable() ?: return IntArray(0)
        if (spriteId < 0 || spriteId >= table.size) return IntArray(0)
        val row = table[spriteId]
        if (row.size <= 1) return IntArray(0)
        return IntArray(row.size - 1) { row[it + 1].asInt() }
    }

    fun sprite(spriteId: Int): GameSprite? = sprites.getOrPut(spriteId) {
        val root = readJson("spr/spr_$spriteId.json") ?: return@getOrPut null
        GameSprite.parse(spriteId, root, spriteImageIds(spriteId))
    }

    fun mapJson(mapId: Int): JsonValue? = readJson("map/map_$mapId.json")

    fun dispose() {
        textures.values.forEach { it?.dispose() }
        textures.clear(); sprites.clear(); tilesets.clear()
        spriteTable = null; modInfo = null; blobs.clear(); loaded = false
    }
}
