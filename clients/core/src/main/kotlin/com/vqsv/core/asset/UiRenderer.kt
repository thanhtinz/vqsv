package com.vqsv.core.asset

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Renders a parsed [UiNode] tree faithfully to the original layout. Use a y-DOWN
 * camera so the J2ME top-left coordinates line up.
 *
 * Two passes (ShapeRenderer and SpriteBatch can't interleave):
 *   renderBackgrounds(shape, root)  — panel/border fills
 *   renderText(batch, font, root)   — labels
 */
object UiRenderer {

    private fun rgb(v: Int, into: Color): Boolean {
        if (v < 0) return false
        into.set(((v shr 16) and 0xFF) / 255f, ((v shr 8) and 0xFF) / 255f, (v and 0xFF) / 255f, 1f)
        return true
    }

    fun renderBackgrounds(shape: ShapeRenderer, root: UiNode, ox: Int = 0, oy: Int = 0) {
        val tmp = Color()
        fun walk(n: UiNode, px: Int, py: Int) {
            val ax = px + n.x; val ay = py + n.y
            if (n.w > 0 && n.h > 0 && rgb(n.bg, tmp)) {
                shape.color = tmp
                shape.rect(ax.toFloat(), ay.toFloat(), n.w.toFloat(), n.h.toFloat())
            }
            n.children.forEach { walk(it, ax, ay) }
        }
        walk(root, ox, oy)
    }

    fun renderText(batch: SpriteBatch, font: BitmapFont, root: UiNode, ox: Int = 0, oy: Int = 0) {
        val tmp = Color()
        fun walk(n: UiNode, px: Int, py: Int) {
            val ax = px + n.x; val ay = py + n.y
            if (n.text.isNotBlank()) {
                font.color = if (rgb(n.border, tmp)) tmp else Color.WHITE
                font.draw(batch, n.text, (ax + 4).toFloat(), (ay + 16).toFloat())
            }
            n.children.forEach { walk(it, ax, ay) }
        }
        walk(root, ox, oy)
    }
}
