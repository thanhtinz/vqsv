package com.vqsv.core.asset

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * Loads the original-game assets baked into the source tree under
 * `clients/core/src/main/resources/game/`:
 *
 *   game/png/img/img_<id>.png    texture atlases
 *   game/png/tex/tex_<id>.png    rebuilt tile textures
 *   game/spr/spr_<id>.json       sprite tables (modules/frames/anims) -> GameSprite
 *   game/map/map_<id>.json       tile maps -> TileMap
 *   game/mod/mod_<t>.json        tileset tile rects
 *   game/meta/sprite_table.json  sprite id -> (sprFileId, imgId...)
 *   game/meta/modInfo.json       tileset id -> (imgId...)
 *
 * The assets are committed as standard resources (no JAR needed at build or run
 * time). To re-extract from a game JAR you supply:
 *   tools/asset-extractor/extract.py <jar> clients/core/src/main/resources/game
 *
 * Everything is cached. If a file is missing the loaders return null/empty so
 * screens fall back to placeholder rendering and the game still runs.
 */
object GameAssets {
    private const val ROOT = "game"
    private val json = JsonReader()

    private val textures = HashMap<Int, Texture?>()
    private val sprites = HashMap<Int, GameSprite?>()
    private var spriteTable: JsonValue? = null
    private var modInfo: JsonValue? = null
    private val tilesets = HashMap<Int, JsonValue?>()

    fun available(): Boolean = Gdx.files.internal("$ROOT/meta/sprite_table.json").exists()

    private fun readJson(path: String): JsonValue? {
        val f = Gdx.files.internal(path)
        return if (f.exists()) json.parse(f) else null
    }

    /** Atlas texture by img id (game/png/img/img_<id>.png). Cached; null if absent. */
    fun atlas(imgId: Int): Texture? = textures.getOrPut(imgId) {
        val f = Gdx.files.internal("$ROOT/png/img/img_$imgId.png")
        if (f.exists()) Texture(f) else null
    }

    private fun spriteTable(): JsonValue? {
        if (spriteTable == null) spriteTable = readJson("$ROOT/meta/sprite_table.json")
        return spriteTable
    }

    fun modInfo(): JsonValue? {
        if (modInfo == null) modInfo = readJson("$ROOT/meta/modInfo.json")
        return modInfo
    }

    fun tileset(tilesetId: Int): JsonValue? = tilesets.getOrPut(tilesetId) {
        readJson("$ROOT/mod/mod_$tilesetId.json")
    }

    /** Resolve a sprite's atlas image-id list from the sprite table (entry[1..]). */
    fun spriteImageIds(spriteId: Int): IntArray {
        val table = spriteTable() ?: return IntArray(0)
        if (spriteId < 0 || spriteId >= table.size) return IntArray(0)
        val row = table[spriteId]
        if (row.size <= 1) return IntArray(0)
        return IntArray(row.size - 1) { row[it + 1].asInt() }
    }

    /** Load (and cache) a fully-parsed sprite. Null if assets/sprite absent. */
    fun sprite(spriteId: Int): GameSprite? = sprites.getOrPut(spriteId) {
        val root = readJson("$ROOT/spr/spr_$spriteId.json") ?: return@getOrPut null
        GameSprite.parse(spriteId, root, spriteImageIds(spriteId))
    }

    fun mapJson(mapId: Int): JsonValue? = readJson("$ROOT/map/map_$mapId.json")

    fun dispose() {
        textures.values.forEach { it?.dispose() }
        textures.clear(); sprites.clear(); tilesets.clear()
        spriteTable = null; modInfo = null
    }
}
