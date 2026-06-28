package com.vqsv.core.asset

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * A parsed original-game UI layout (game/ui/<name>.json) — the recursive
 * component tree decoded from the `.ui` files. See docs/ASSET-FORMATS.md §5.
 *
 * Node coordinates are absolute (J2ME top-left origin). Colours are 24-bit RGB
 * ints (e.g. 16777215 = white). Use UiRenderer to draw a tree with a y-down
 * camera so it matches the original layout pixel-for-pixel.
 */
data class UiNode(
    val type: String,
    val id: Int,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val text: String,
    val bg: Int,          // -1 = none
    val border: Int,      // -1 = none
    val sprAction: Int,   // spr_focus.action, -1 = none
    val sprFrame: Int,    // spr_focus.frame
    val children: List<UiNode>
)

object UiLayout {
    private val json = JsonReader()

    fun load(name: String): UiNode? {
        val f = Gdx.files.internal("game/ui/$name.json")
        if (!f.exists()) return null
        return parse(json.parse(f))
    }

    private fun parse(v: JsonValue): UiNode {
        val spr = v.get("spr_focus")
        val children = ArrayList<UiNode>()
        v.get("children")?.let { c ->
            var n = c.child
            while (n != null) { children.add(parse(n)); n = n.next }
        }
        return UiNode(
            type = v.getString("type", "panel"),
            id = v.getInt("id", 0),
            x = v.getInt("x", 0),
            y = v.getInt("y", 0),
            w = v.getInt("w", 0),
            h = v.getInt("h", 0),
            text = v.getString("text", ""),
            bg = v.getInt("bg", -1),
            border = v.getInt("border", -1),
            sprAction = spr?.getInt("action", -1) ?: -1,
            sprFrame = spr?.getInt("frame", 0) ?: 0,
            children = children
        )
    }
}
