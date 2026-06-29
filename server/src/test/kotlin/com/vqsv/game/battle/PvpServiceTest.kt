package com.vqsv.game.battle

import com.vqsv.entity.PetTemplate
import com.vqsv.entity.PlayerPet
import com.vqsv.entity.Skill
import com.vqsv.repository.PlayerPetRepository
import com.vqsv.repository.SkillRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

/**
 * Turn-resolution tests for PvP. The repository is mocked so no DB is needed.
 * Combat damage has a crit roll, so assertions only rely on deterministic
 * outcomes (who reaches 0 HP, both sides taking damage, forfeit).
 */
class PvpServiceTest {

    private fun pet(hpMax: Int, atk: Int, def: Int, spd: Int, element: String = "FIRE"): PlayerPet {
        val tpl = mock(PetTemplate::class.java)
        `when`(tpl.element).thenReturn(element)
        `when`(tpl.spriteId).thenReturn(1)
        `when`(tpl.skillElem).thenReturn(0)
        val p = mock(PlayerPet::class.java)
        `when`(p.hpMax).thenReturn(hpMax)
        `when`(p.atk).thenReturn(atk.toShort())
        `when`(p.def).thenReturn(def.toShort())
        `when`(p.spd).thenReturn(spd.toShort())
        `when`(p.level).thenReturn(5)
        `when`(p.template).thenReturn(tpl)
        return p
    }

    private fun serviceWith(p1: PlayerPet?, p2: PlayerPet?, skills: List<Skill> = emptyList()): PvpService {
        val repo = mock(PlayerPetRepository::class.java)
        `when`(repo.findByPlayerIdAndSlot(1L, 0)).thenReturn(Optional.ofNullable(p1))
        `when`(repo.findByPlayerIdAndSlot(2L, 0)).thenReturn(Optional.ofNullable(p2))
        val skillRepo = mock(SkillRepository::class.java)
        `when`(skillRepo.findByElement(0)).thenReturn(skills)
        return PvpService(repo, SkillService(skillRepo))
    }

    @Test
    fun `start returns null when a player has no active pet`() {
        val svc = serviceWith(pet(100, 10, 5, 5), null)
        assertNull(svc.start(1L, "A", 2L, "B"))
    }

    @Test
    fun `a session is recognised as PvP and seeds full HP`() {
        val svc = serviceWith(pet(120, 10, 5, 5), pet(140, 10, 5, 5))
        val s = svc.start(1L, "A", 2L, "B")!!
        assertTrue(svc.isPvp(s.battleId))
        assertEquals(120, s.a.hp)
        assertEquals(140, s.b.hp)
    }

    @Test
    fun `round waits for the opponent before resolving`() {
        val svc = serviceWith(pet(200, 20, 5, 9), pet(200, 20, 5, 5))
        val s = svc.start(1L, "A", 2L, "B")!!
        assertNull(svc.submitAction(s.battleId, 1L, 0)) // only A has acted
    }

    @Test
    fun `both combatants attack in the same round`() {
        // High HP so neither dies even on a crit -> deterministic ONGOING with both damaged.
        val svc = serviceWith(pet(1000, 50, 10, 9), pet(1000, 50, 10, 5))
        val s = svc.start(1L, "A", 2L, "B")!!
        svc.submitAction(s.battleId, 1L, 0)
        val r = svc.submitAction(s.battleId, 2L, 0)!!
        assertEquals("ONGOING", r.status)
        assertTrue(r.aHp < 1000, "A should have taken damage")
        assertTrue(r.bHp < 1000, "B should have taken damage")
        assertEquals(2, r.log.size, "both sides should have attacked")
    }

    @Test
    fun `the faster, far stronger combatant wins by KO`() {
        // A is faster and overwhelmingly stronger -> A KOs B before B matters.
        val svc = serviceWith(pet(1000, 1000, 50, 99), pet(10, 1, 0, 1))
        val s = svc.start(1L, "A", 2L, "B")!!
        svc.submitAction(s.battleId, 1L, 0)
        val r = svc.submitAction(s.battleId, 2L, 0)!!
        assertEquals("A_WIN", r.status)
        assertEquals(0, r.bHp)
        assertTrue(svc.session(s.battleId) == null, "finished session is removed")
    }

    @Test
    fun `a skill the pet has learned is used and logged in PvP`() {
        // A high-power WOOD skill (element 0) that A has learned (required_level <= 5).
        val skill = Skill(id = 0, name = "Lá Sắc", element = 0, requiredLevel = 1, spCost = 10, power = 200)
        // A is fast so it strikes first; very high HP so neither dies -> deterministic ONGOING.
        val svc = serviceWith(pet(2000, 50, 10, 9), pet(2000, 50, 10, 5), skills = listOf(skill))
        val s = svc.start(1L, "A", 2L, "B")!!
        svc.submitAction(s.battleId, 1L, 4, 0) // A uses skill id 0
        val r = svc.submitAction(s.battleId, 2L, 0)!! // B attacks
        assertEquals("ONGOING", r.status)
        assertTrue(r.log.any { it.contains("dùng Lá Sắc") }, "the skill use should be logged")
    }

    @Test
    fun `a burn skill sets the opponent on fire and ticks each round`() {
        // behavior_flag = 2, effect_id = 0 -> BURN; power 0 so it's a low/basic hit.
        val burn = Skill(id = 0, name = "Dương Viêm", element = 0, requiredLevel = 1, spCost = 10,
            power = 0, effectId = 0, behaviorFlag = 2)
        // Huge HP so nobody faints; A is faster so it lands the burn first.
        val svc = serviceWith(pet(5000, 40, 10, 9), pet(5000, 40, 10, 5), skills = listOf(burn))
        val s = svc.start(1L, "A", 2L, "B")!!
        svc.submitAction(s.battleId, 1L, 4, 0)        // A burns B
        val r1 = svc.submitAction(s.battleId, 2L, 0)!!
        assertTrue(r1.log.any { it.contains("bị Đốt Cháy") }, "burn is applied")
        assertTrue(r1.log.any { it.contains("bị thiêu đốt") }, "burn ticks the same round")

        // A plain follow-up round still ticks the lingering burn on B.
        svc.submitAction(s.battleId, 1L, 0)
        val r2 = svc.submitAction(s.battleId, 2L, 0)!!
        assertTrue(r2.log.any { it.contains("B bị thiêu đốt") }, "burn keeps ticking next round")
    }

    @Test
    fun `forfeit resolves immediately as a loss for the quitter`() {
        val svc = serviceWith(pet(200, 20, 5, 5), pet(200, 20, 5, 5))
        val s = svc.start(1L, "A", 2L, "B")!!
        val r = svc.submitAction(s.battleId, 1L, 3)!! // A forfeits
        assertEquals("B_WIN", r.status)
    }

    @Test
    fun `abortFor reports the opponent to notify and clears the session`() {
        val svc = serviceWith(pet(200, 20, 5, 5), pet(200, 20, 5, 5))
        val s = svc.start(1L, "A", 2L, "B")!!
        val (battleId, opp) = svc.abortFor(1L)!!
        assertEquals(s.battleId, battleId)
        assertEquals(2L, opp)
        assertNull(svc.session(s.battleId))
    }
}
