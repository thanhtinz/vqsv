package com.vqsv.core.asset

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.JsonValue

/**
 * A parsed original-game tile map (map_<id>.json) with its tileset.
 *
 * Tile lookup: a grid cell value's low 12 bits index the tileset's tile-rect
 * list (mod_<tileset>.json); each rect's `img` indexes modInfo[tileset] to pick
 * the atlas. Draw with a y-DOWN camera. Render the ground layers first, then
 * (optionally) object layers on top.
 */
class TileMap private constructor(
    val width: Int,
    val height: Int,
    val tileSize: Int,
    val tilesetId: Int,
    private val tileRects: Array<IntArray>,   // [imgIdInModInfo, x, y, w, h]
    private val modImgIds: IntArray,          // modInfo[tileset]
    private val groundLayers: List<Array<IntArray>>  // each: grid[col][row] value (-1 empty)
) {
    val pxWidth get() = width * tileSize
    val pxHeight get() = height * tileSize

    fun draw(batch: SpriteBatch) {
        for (grid in groundLayers) {
            for (col in 0 until width) {
                val column = grid.getOrNull(col) ?: continue
                for (row in 0 until height) {
                    val value = column.getOrElse(row) { -1 }
                    if (value < 0) continue
                    drawTile(batch, value and 0x0FFF, col * tileSize.toFloat(), row * tileSize.toFloat())
                }
            }
        }
    }

    private fun drawTile(batch: SpriteBatch, tileId: Int, x: Float, y: Float) {
        val rect = tileRects.getOrNull(tileId) ?: return
        val imgSlot = rect[0]
        if (imgSlot < 0 || imgSlot >= modImgIds.size) return
        val tex = GameAssets.atlas(modImgIds[imgSlot]) ?: return
        batch.draw(tex, x, y, rect[3].toFloat(), rect[4].toFloat(), rect[1], rect[2], rect[3], rect[4], false, false)
    }

    companion object {
        /** Load map by id; null if assets/map absent. */
        fun load(mapId: Int): TileMap? {
            val root = GameAssets.mapJson(mapId) ?: return null
            val width = root.getInt("width")
            val height = root.getInt("height")
            val tileSize = root.getInt("tile_size", 16)
            val tilesetId = root.getInt("tileset", 0)

            val tileRects = GameAssets.tileset(tilesetId)?.let { ts ->
                val out = ArrayList<IntArray>(ts.size)
                var c = ts.child
                while (c != null) {
                    out.add(intArrayOf(c.getInt("img"), c.getInt("x"), c.getInt("y"), c.getInt("w"), c.getInt("h")))
                    c = c.next
                }
                out.toTypedArray()
            } ?: emptyArray()

            val modInfo = GameAssets.modInfo()
            val modImgIds = if (modInfo != null && tilesetId < modInfo.size) {
                val row = modInfo[tilesetId]
                IntArray(row.size) { row[it].asInt() }
            } else IntArray(0)

            val ground = ArrayList<Array<IntArray>>()
            val layers = root.get("layers")
            var layer = layers?.child
            while (layer != null) {
                val type = layer.getInt("type", -1)
                if (type == 0 || type == 1) {
                    val gridJson = layer.get("grid")
                    if (gridJson != null) {
                        val grid = ArrayList<IntArray>(gridJson.size)
                        var colJson = gridJson.child
                        while (colJson != null) {
                            grid.add(IntArray(colJson.size) { colJson.get(it).asInt() })
                            colJson = colJson.next
                        }
                        ground.add(grid.toTypedArray())
                    }
                }
                layer = layer.next
            }
            return TileMap(width, height, tileSize, tilesetId, tileRects, modImgIds, ground)
        }
    }
}
