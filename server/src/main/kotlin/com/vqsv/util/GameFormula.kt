package com.vqsv.util

import com.vqsv.entity.PetTemplate
import kotlin.math.roundToInt
import kotlin.random.Random

object GameFormula {

    fun expForLevel(level: Int): Int = (100 * level * 1.5).roundToInt()

    fun petStat(base: Int, growth: Double, level: Int): Int =
        (base * Math.pow(growth, (level - 1).toDouble())).roundToInt()

    fun petHpMax(template: PetTemplate, level: Int): Int =
        petStat(template.baseHp.toInt(), template.growthHp, level)

    fun petAtk(template: PetTemplate, level: Int): Int =
        petStat(template.baseAtk.toInt(), template.growthAtk, level)

    fun petDef(template: PetTemplate, level: Int): Int =
        petStat(template.baseDef.toInt(), template.growthDef, level)

    fun petSpd(template: PetTemplate, level: Int): Int =
        petStat(template.baseSpd.toInt(), template.growthSpd, level)

    fun calcDamage(atk: Int, def: Int, elementMult: Float = 1.0f, isCrit: Boolean = false): Int {
        val base = ((atk.toFloat() * elementMult) / (def.toFloat() + 10) * 10).roundToInt()
        return if (isCrit) (base * 1.5).roundToInt() else base.coerceAtLeast(1)
    }

    fun isCrit(spd: Int): Boolean = Random.nextInt(100) < (spd / 3).coerceAtMost(25)

    fun elementMult(attackerElement: String, defenderElement: String): Float {
        val advantages = mapOf(
            "FIRE"  to "WIND",
            "WATER" to "FIRE",
            "WIND"  to "EARTH",
            "EARTH" to "WATER",
            "LIGHT" to "DARK",
            "DARK"  to "LIGHT"
        )
        return when {
            advantages[attackerElement] == defenderElement -> 1.5f
            advantages[defenderElement] == attackerElement -> 0.67f
            else -> 1.0f
        }
    }

    fun catchSuccess(baseRate: Int, enemyHpPercent: Float, ballBonus: Int, loyalty: Int = 50): Boolean {
        val hpMod = when {
            enemyHpPercent < 0.1f -> 2.0f
            enemyHpPercent < 0.25f -> 1.5f
            enemyHpPercent < 0.5f -> 1.2f
            else -> 1.0f
        }
        val effectiveRate = (baseRate * hpMod * (ballBonus / 30.0f) * (loyalty / 50.0f)).roundToInt()
        return Random.nextInt(100) < effectiveRate
    }

    fun wildPetLevel(minLv: Int, maxLv: Int): Int = Random.nextInt(minLv, maxLv + 1)

    fun battleExpReward(baseExp: Int, playerLevel: Int, enemyLevel: Int): Int {
        val levelDiff = enemyLevel - playerLevel
        val mult = when {
            levelDiff > 5 -> 2.0f
            levelDiff > 0 -> 1.5f
            levelDiff < -10 -> 0.1f
            levelDiff < -5 -> 0.5f
            else -> 1.0f
        }
        return (baseExp * mult).roundToInt()
    }
}
