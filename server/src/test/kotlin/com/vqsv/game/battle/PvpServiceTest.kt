package com.vqsv.game.battle

import com.vqsv.entity.PetTemplate
import com.vqsv.entity.PlayerPet
import com.vqsv.repository.PlayerPetRepository
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
        val p = mock(PlayerPet::class.java)
        `when`(p.hpMax).thenReturn(hpMax)
        `when`(p.atk).thenReturn(atk.toShort())
        `when`(p.def).thenReturn(def.toShort())
        `when`(p.spd).thenReturn(spd.toShort())
        `when`(p.template).thenReturn(tpl)
        return p
    }

    private fun serviceWith(p1: PlayerPet?, p2: PlayerPet?): PvpService {
        val repo = mock(PlayerPetRepository::class.java)
        `when`(repo.findByPlayerIdAndSlot(1L, 0)).thenReturn(Optional.ofNullable(p1))
        `when`(repo.findByPlayerIdAndSlot(2L, 0)).thenReturn(Optional.ofNullable(p2))
        return PvpService(repo)
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
