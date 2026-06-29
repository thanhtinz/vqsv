package com.vqsv.game.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusEffectsTest {

    @Test
    fun `only behavior_flag 2 with effect_id 0 is a burn skill`() {
        assertTrue(StatusEffects.isBurnSkill(2, 0), "đốt cháy")
        assertFalse(StatusEffects.isBurnSkill(2, 1), "mê muội is not burn")
        assertFalse(StatusEffects.isBurnSkill(0, 0), "a plain damage skill is not burn")
        assertFalse(StatusEffects.isBurnSkill(2, null), "no effect id")
    }

    @Test
    fun `burn lasts three turns and deals its magnitude each turn`() {
        val statuses = mutableListOf<StatusEffect>()
        StatusEffects.applyBurn(statuses, attackerAtk = 80)   // magnitude = 80/8 = 10
        val log = mutableListOf<String>()

        // tick returns a signed HP delta: burn is negative (damage).
        val t1 = StatusEffects.tick(statuses, "Mob", log)
        val t2 = StatusEffects.tick(statuses, "Mob", log)
        val t3 = StatusEffects.tick(statuses, "Mob", log)
        val t4 = StatusEffects.tick(statuses, "Mob", log)

        assertEquals(-10, t1)
        assertEquals(-10, t2)
        assertEquals(-10, t3)
        assertEquals(0, t4, "burn expires after 3 turns")
        assertTrue(statuses.isEmpty(), "the effect is removed once it expires")
    }

    @Test
    fun `regen heals a positive amount and a buff reports its percent`() {
        val regen = StatusEffects.selfBuffFor(StatusEffects.BUFF_REGEN, casterHpMax = 200)!! // 200/10 = 20
        val statuses = mutableListOf(regen)
        val delta = StatusEffects.tick(statuses, "Pet", mutableListOf())
        assertEquals(20, delta, "regen restores HP (positive delta)")

        assertEquals(150, StatusEffects.effectiveAtk(100, listOf(StatusEffects.selfBuffFor(StatusEffects.BUFF_ATK, 100)!!)))
        assertEquals(130, StatusEffects.effectiveDef(100, listOf(StatusEffects.selfBuffFor(StatusEffects.BUFF_DEF, 100)!!)))
    }

    @Test
    fun `bind always disables but a plain pet is free to act`() {
        assertTrue(StatusEffects.isDisabled(listOf(StatusEffect(StatusType.BIND, 3))), "bind locks the turn")
        assertFalse(StatusEffects.isDisabled(emptyList()), "no status, free to act")
    }

    @Test
    fun `re-applying burn refreshes rather than stacks`() {
        val statuses = mutableListOf<StatusEffect>()
        StatusEffects.applyBurn(statuses, 80)
        StatusEffects.applyBurn(statuses, 80)
        assertEquals(1, statuses.size, "burn does not stack")
    }
}
