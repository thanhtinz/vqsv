package com.vqsv.core.asset

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.JsonValue

/**
 * A fully-parsed original-game sprite (spr_<id>.json + its atlas images).
 *
 * Structure (see docs/ASSET-FORMATS.md):
 *   modules  — clip rectangles into one of the sprite's atlas images
 *   frames   — each frame is a list of module placements {module, dx, dy, flip}
 *   anims    — each animation is a sequence of {delay, frame}
 *
 * Render with a y-DOWN camera (screen.setupCamera) so the J2ME top-left
 * coordinate system and dx/dy offsets line up.
 */
class GameSprite private constructor(
    val spriteId: Int,
    val imageIds: IntArray,
    val modules: Array<IntArray>,            // [img, x, y, w, h]
    val frames: Array<Array<IntArray>>,      // frame -> [ [module, dx, dy, flip], ... ]
    val anims: Array<Array<IntArray>>        // anim  -> [ [delay, frame], ... ]
) {
    val frameCount get() = frames.size
    val animCount get() = anims.size

    /** Draw a single frame, anchored at (ox, oy). `mirror` flips the whole sprite horizontally. */
    fun drawFrame(batch: SpriteBatch, frameIdx: Int, ox: Float, oy: Float, mirror: Boolean = false) {
        if (frameIdx < 0 || frameIdx >= frames.size) return
        for (p in frames[frameIdx]) {
            val m = modules.getOrNull(p[0]) ?: continue
            val imgIdx = m[0]
            if (imgIdx < 0 || imgIdx >= imageIds.size) continue
            val tex = GameAssets.atlas(imageIds[imgIdx]) ?: continue
            val w = m[3]; val h = m[4]
            val flipX = (p[3] and 4) != 0
            val drawX = if (mirror) ox - (p[1] + w) else ox + p[1]
            batch.draw(tex, drawX, oy + p[2], w.toFloat(), h.toFloat(),
                m[1], m[2], w, h, flipX xor mirror, false)
        }
    }

    /** Resolve the frame index for a given animation + step. */
    fun animFrame(animIdx: Int, step: Int): Int {
        val a = anims.getOrNull(animIdx) ?: return 0
        if (a.isEmpty()) return 0
        return a[step % a.size][1]
    }

    fun animSteps(animIdx: Int): Int = anims.getOrNull(animIdx)?.size ?: 0

    companion object {
        fun parse(spriteId: Int, root: JsonValue, imageIds: IntArray): GameSprite {
            val modules = root.get("modules").toArrayOf { m ->
                intArrayOf(m.getInt("img"), m.getInt("x"), m.getInt("y"), m.getInt("w"), m.getInt("h"))
            }
            val frames = root.get("frames").toArrayOf { fr ->
                fr.toArrayOf { p ->
                    intArrayOf(p.getInt("module"), p.getInt("dx"), p.getInt("dy"), p.getInt("flip", 0))
                }
            }
            val anims = root.get("anims").toArrayOf { an ->
                an.toArrayOf { s -> intArrayOf(s.getInt("delay", 0), s.getInt("frame")) }
            }
            return GameSprite(spriteId, imageIds, modules, frames, anims)
        }

        private inline fun <reified T> JsonValue?.toArrayOf(map: (JsonValue) -> T): Array<T> {
            if (this == null || this.size == 0) return emptyArray()
            val out = ArrayList<T>(this.size)
            var c = this.child
            while (c != null) { out.add(map(c)); c = c.next }
            return out.toTypedArray()
        }
    }
}

/** Plays an animation of a sprite over time (delay units are game ticks ~ frames). */
class SpriteAnimator(val sprite: GameSprite, var animIdx: Int = 0) {
    private var timer = 0f
    private var step = 0
    var fps = 10f                      // J2ME ran ~10 ticks/s

    fun setAnim(idx: Int) { if (idx != animIdx) { animIdx = idx; step = 0; timer = 0f } }

    fun update(delta: Float) {
        val steps = sprite.animSteps(animIdx)
        if (steps <= 0) return
        timer += delta
        val stepDur = 1f / fps
        while (timer >= stepDur) { timer -= stepDur; step = (step + 1) % steps }
    }

    fun draw(batch: SpriteBatch, x: Float, y: Float, mirror: Boolean = false) {
        val frame = if (sprite.animSteps(animIdx) > 0) sprite.animFrame(animIdx, step) else 0
        sprite.drawFrame(batch, frame, x, y, mirror)
    }
}
