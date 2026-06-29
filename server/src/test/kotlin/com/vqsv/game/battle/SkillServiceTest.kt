package com.vqsv.game.battle

import com.vqsv.entity.Skill
import com.vqsv.repository.SkillRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * The faithful skill-learning rule: a pet draws from its element block, learns the
 * lowest-id eligible skills (required_level met, that actually do something), capped
 * at min(5, level/10 + 1).
 */
class SkillServiceTest {

    private fun skill(id: Int, req: Int, power: Int, behavior: Int) = Skill(
        id = id.toShort(), name = "S$id", element = 0,
        requiredLevel = req.toShort(), spCost = 10, power = power.toShort(),
        effectId = null, behaviorFlag = behavior.toShort(), targetCode = 0
    )

    private fun service(): SkillService {
        val repo = mock(SkillRepository::class.java)
        `when`(repo.findByElement(0)).thenReturn(
            listOf(
                skill(0, 1, 0, 0),     // basic (no power, no effect) -> filtered out
                skill(1, 1, 0, 2),     // status skill -> kept
                skill(2, 1, 10, 0),    // weak damage -> kept
                skill(3, 1, 120, 0),   // strong damage -> kept
                skill(4, 5, 50, 0),    // unlocks at lv5
                skill(5, 20, 200, 0),  // unlocks at lv20
                skill(6, 30, 250, 0)   // unlocks at lv30
            )
        )
        return SkillService(repo)
    }

    @Test
    fun `spMax scales with level`() {
        assertEquals(45, service().spMax(5))
        assertEquals(90, service().spMax(50))
    }

    @Test
    fun `learned count is capped at min(5, level over 10 + 1)`() {
        val s = service()
        assertEquals(1, s.learnedSkills(0, 5).size)    // cap 1
        assertEquals(2, s.learnedSkills(0, 10).size)   // cap 2
        assertEquals(3, s.learnedSkills(0, 25).size)   // cap 3
        assertEquals(5, s.learnedSkills(0, 50).size)   // cap 5
    }

    @Test
    fun `the no-power no-effect basic skill is never offered`() {
        assertTrue(service().learnedSkills(0, 50).none { it.id.toInt() == 0 })
    }

    @Test
    fun `skills above the pet level are not learned`() {
        // level 25 -> eligible ids {1,2,3,4,5}; capped to 3 lowest -> {1,2,3}; id 5 excluded
        val learned = service().learnedSkills(0, 25).map { it.id.toInt() }
        assertEquals(listOf(1, 2, 3), learned)
    }

    @Test
    fun `usableSkill only returns a learned skill`() {
        val s = service()
        assertNotNull(s.usableSkill(0, 25, 3))   // learned at lv25
        assertNull(s.usableSkill(0, 25, 5))      // eligible by level but beyond the cap
        assertNull(s.usableSkill(0, 5, 99))      // doesn't exist
    }
}
