package com.vqsv.util

import com.vqsv.entity.PetTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the deterministic game formulas (no Spring context needed).
 * These pin the original-game maths so future refactors can't silently drift.
 */
class GameFormulaTest {

    @Test
    fun `expForLevel matches 15·lvl² − 200 with a floor of 50`() {
        assertEquals(50, GameFormula.expForLevel(1))    // 15-200 -> floored to 50
        assertEquals(50, GameFormula.expForLevel(4))    // 240-200=40 -> floored to 50
        assertEquals(175, GameFormula.expForLevel(5))   // 375-200
        assertEquals(1300, GameFormula.expForLevel(10)) // 1500-200
    }

    @Test
    fun `gradeMult returns the percent tier and clamps out-of-range grades`() {
        assertEquals(90, GameFormula.gradeMult(1))
        assertEquals(100, GameFormula.gradeMult(3))
        assertEquals(125, GameFormula.gradeMult(5))
        assertEquals(90, GameFormula.gradeMult(0))    // clamped low
        assertEquals(125, GameFormula.gradeMult(9))   // clamped high
    }

    @Test
    fun `calcDamage is ATK−DEF scaled by skill, element and crit, never below 1`() {
        assertEquals(10, GameFormula.calcDamage(atk = 20, def = 10))
        // crit x1.5
        assertEquals(15, GameFormula.calcDamage(atk = 20, def = 10, isCrit = true))
        // super-effective x3
        assertEquals(30, GameFormula.calcDamage(atk = 20, def = 10, elementMult = 3.0f))
        // skill power 50%
        assertEquals(5, GameFormula.calcDamage(atk = 20, def = 10, skillPower = 50))
        // defense >= attack still deals the minimum 1
        assertEquals(1, GameFormula.calcDamage(atk = 5, def = 100))
    }

    @Test
    fun `elementMult follows the 7-element chain`() {
        assertEquals(3.0f, GameFormula.elementMult("WOOD", "EARTH"))    // 0 beats 1
        assertEquals(3.0f, GameFormula.elementMult("FIRE", "WOOD"))     // 3 beats 0
        assertEquals(3.0f, GameFormula.elementMult("WIND", "ELECTRIC")) // 5 beats 6
        assertEquals(0.6f, GameFormula.elementMult("EARTH", "WOOD"))    // resisted (reverse)
        assertEquals(1.0f, GameFormula.elementMult("WOOD", "WATER"))    // neutral
    }

    @Test
    fun `elementMult accepts legacy LIGHT and DARK aliases`() {
        assertEquals(3.0f, GameFormula.elementMult("ELECTRIC", "DARK"))  // 6 beats 4(GHOST/DARK)
        assertEquals(1.0f, GameFormula.elementMult("LIGHT", "LIGHT"))
        assertEquals(1.0f, GameFormula.elementMult("FIRE", "UNKNOWN"))   // unknown -> neutral
    }

    @Test
    fun `fleeChance is auto when faster and degrades with level gap`() {
        assertEquals(100, GameFormula.fleeChance(mySpd = 20, enemySpd = 10, myLevel = 5, enemyLevel = 5))
        assertEquals(95, GameFormula.fleeChance(mySpd = 5, enemySpd = 5, myLevel = 5, enemyLevel = 5))
        assertEquals(75, GameFormula.fleeChance(mySpd = 5, enemySpd = 9, myLevel = 5, enemyLevel = 7))
        // floor at 15 for a big gap
        assertEquals(15, GameFormula.fleeChance(mySpd = 1, enemySpd = 9, myLevel = 1, enemyLevel = 30))
    }

    @Test
    fun `catchSuccess is guaranteed for a perfect ball`() {
        assertTrue(GameFormula.catchSuccess(baseRate = 1, enemyHpPercent = 1.0f, ballBonus = 100))
    }

    @Test
    fun `battleExpReward grows with enemy level`() {
        val low = GameFormula.battleExpReward(baseExp = 0, playerLevel = 5, enemyLevel = 3)
        val high = GameFormula.battleExpReward(baseExp = 0, playerLevel = 5, enemyLevel = 30)
        assertTrue(high > low)
        // grade 3, enemyLevel 3: ((6)*3 + 50) * 12/10 + 400 = (18+50)*12/10 + 400 = 81 + 400 = 481
        assertEquals(481, low)
    }

    @Test
    fun `linear stat formula applies base + per·lvl + flat at grade 3`() {
        val t = PetTemplate(
            hpBase = 100, hpPer = 10, hpFlat = 0,
            atkBase = 20, atkPer = 2, atkFlat = 0,
            defBase = 10, defPer = 10, defFlat = 0,   // def uses /10
            spdBase = 8, spdPer = 10, spdFlat = 0     // spd uses /10
        )
        // grade 3 -> x100/100 (no change)
        assertEquals(100 + 10 * 5, GameFormula.petHpMax(t, 5))     // 150
        assertEquals(20 + 2 * 5, GameFormula.petAtk(t, 5))         // 30
        assertEquals(10 + 10 * 5 / 10, GameFormula.petDef(t, 5))   // 10 + 5 = 15
        assertEquals(8 + 10 * 5 / 10, GameFormula.petSpd(t, 5))    // 8 + 5 = 13
    }
}
