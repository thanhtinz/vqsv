package com.vqsv.util

import com.vqsv.entity.PetTemplate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Game rules ported to match the ORIGINAL J2ME game ("Sủng vật Vương quốc").
 * Formulas reverse-engineered from `game/j.java` -- see docs/ORIGINAL-MECHANICS.md.
 *
 * Note on exact stat VALUES: the original computes stats as a linear
 * `(base + perLvl*level + flat) * gradeMult/100` from the species table in
 * `data/script/db.mid` (c[0]). Until that table is imported, [petStat] keeps the
 * current template-driven growth; the battle FORMULAS below (damage, crit,
 * element, exp, capture, flee) match the original exactly at grade 3.
 */
object GameFormula {

    const val LEVEL_CAP = 50

    // Grade (quality tier 1..5) multiplier, percent -- N[] in j.java.
    private val GRADE_MULT = intArrayOf(90, 95, 100, 110, 125)
    fun gradeMult(grade: Int): Int = GRADE_MULT[(grade - 1).coerceIn(0, 4)]

    // EXP required to reach the next level: j.B(level) = 15*level^2 - 200 (cap 50).
    fun expForLevel(level: Int): Int =
        (15 * level * level - 200).coerceAtLeast(50)

    // ---- Legacy exponential growth (UNUSED, kept for signature compatibility) ----
    fun petStat(base: Int, growth: Double, level: Int): Int =
        (base * Math.pow(growth, (level - 1).toDouble())).roundToInt()

    // ---- Original LINEAR stat formula (decoded from db.mid species table) ----
    // stat = (base + perLvl*level/div + flat) * gradeMult/100
    //   div = 1 for HP/ATK, div = 10 for DEF/SPD. Default grade 3 (=100).
    fun petHpMax(template: PetTemplate, level: Int, grade: Int = 3): Int =
        ((template.hpBase + template.hpPer * level + template.hpFlat) * gradeMult(grade) / 100).coerceAtLeast(1)

    fun petAtk(template: PetTemplate, level: Int, grade: Int = 3): Int =
        ((template.atkBase + template.atkPer * level + template.atkFlat) * gradeMult(grade) / 100).coerceAtLeast(1)

    fun petDef(template: PetTemplate, level: Int, grade: Int = 3): Int =
        ((template.defBase + template.defPer * level / 10 + template.defFlat) * gradeMult(grade) / 100).coerceAtLeast(1)

    fun petSpd(template: PetTemplate, level: Int, grade: Int = 3): Int =
        ((template.spdBase + template.spdPer * level / 10 + template.spdFlat) * gradeMult(grade) / 100).coerceAtLeast(1)

    /**
     * Damage = (ATK - DEF) * skillPower% * elementMult * crit, minimum 1.
     * Matches j.b(j): base is the flat ATK-DEF difference (NOT a ratio), crit x1.5.
     */
    fun calcDamage(atk: Int, def: Int, elementMult: Float = 1.0f, isCrit: Boolean = false, skillPower: Int = 100): Int {
        val base = (atk - def).coerceAtLeast(1)
        var dmg = base * (skillPower / 100.0f) * elementMult
        if (isCrit) dmg *= 1.5f
        return dmg.roundToInt().coerceAtLeast(1)
    }

    // Crit chance = 5 + SPD/2 (percent). (+30 for max-evolved final stage -- not modelled here.)
    fun isCrit(spd: Int): Boolean = Random.nextInt(100) < (5 + spd / 2)

    // ---- Element relations (7 elements, ids per j.a(j)) ----
    // 0=WOOD(Mộc) 1=EARTH(Thổ) 2=WATER(Thủy) 3=FIRE(Hỏa) 4=GHOST(Quỷ) 5=WIND(Phong) 6=ELECTRIC(Điện)
    // Super-effective chain: 0->1, 1->2, 2->3, 3->0, and 5->6, 6->4, 4->5.
    private val ELEMENT_ID = mapOf(
        "WOOD" to 0, "EARTH" to 1, "WATER" to 2, "FIRE" to 3,
        "GHOST" to 4, "WIND" to 5, "ELECTRIC" to 6,
        // legacy aliases from the earlier 6-element seed
        "LIGHT" to 6, "DARK" to 4
    )
    private val BEATS = mapOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 5 to 6, 6 to 4, 4 to 5)

    // Element id (0-6, as stored on skills.element / pet_templates.skill_elem) -> name.
    private val ELEMENT_NAMES = arrayOf("WOOD", "EARTH", "WATER", "FIRE", "GHOST", "WIND", "ELECTRIC")
    fun elementName(id: Int): String = ELEMENT_NAMES.getOrElse(id) { "FIRE" }

    /** 3.0 super-effective, 0.6 resisted, 1.0 neutral (matches battle.ui 300%/60%/100%). */
    fun elementMult(attackerElement: String, defenderElement: String): Float {
        val a = ELEMENT_ID[attackerElement.uppercase()] ?: return 1.0f
        val d = ELEMENT_ID[defenderElement.uppercase()] ?: return 1.0f
        return when {
            BEATS[a] == d -> 3.0f
            BEATS[d] == a -> 0.6f
            else -> 1.0f
        }
    }

    fun wildPetLevel(minLv: Int, maxLv: Int): Int = Random.nextInt(minLv, maxLv + 1)

    /**
     * Capture rate % (j.m(ballId)). ballBonus is the ball quality (item.effectVal):
     * 100 = guaranteed-tier. HP factor: <=15% -> 85, <=50% -> 45, else 20.
     * Grade harder at higher grade. Clamped 1..100. (rarity/status factors need db.mid.)
     */
    fun catchSuccess(baseRate: Int, enemyHpPercent: Float, ballBonus: Int, grade: Int = 3): Boolean {
        if (ballBonus >= 100) return true                       // guaranteed seal-ball
        val hpFactor = when {
            enemyHpPercent <= 0.15f -> 85
            enemyHpPercent <= 0.50f -> 45
            else -> 20
        }
        val gradeFactor = intArrayOf(110, 100, 95, 80, 70)[(grade - 1).coerceIn(0, 4)]
        val rate = (hpFactor * (ballBonus / 50.0f) * (gradeFactor / 100.0f)).roundToInt().coerceIn(1, 100)
        return Random.nextInt(100) < rate
    }

    /** Flee chance % (game/l.java action 5): faster auto, equal 95, else max(15, 95 - levelDiff*10). */
    fun fleeChance(mySpd: Int, enemySpd: Int, myLevel: Int, enemyLevel: Int): Int = when {
        mySpd > enemySpd -> 100
        myLevel == enemyLevel -> 95
        else -> (95 - (enemyLevel - myLevel) * 10).coerceAtLeast(15)
    }

    /**
     * EXP pool on enemy faint (j.h): ((enemyLevel*2)*enemyLevel + 50) * aN[grade-1]/10 + 400.
     * baseExp/playerLevel kept for signature compatibility; enemyLevel + grade drive it.
     */
    fun battleExpReward(baseExp: Int, playerLevel: Int, enemyLevel: Int, grade: Int = 3): Int {
        val aN = intArrayOf(10, 11, 12, 13, 15)[(grade - 1).coerceIn(0, 4)]
        return ((enemyLevel * 2) * enemyLevel + 50) * aN / 10 + 400
    }
}
