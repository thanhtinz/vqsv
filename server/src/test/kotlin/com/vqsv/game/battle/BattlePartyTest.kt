package com.vqsv.game.battle

import com.vqsv.dto.BattleAction
import com.vqsv.entity.PetTemplate
import com.vqsv.entity.PlayerPet
import com.vqsv.repository.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

/**
 * Team-battle survival: when the active pet faints and another party member is alive,
 * the battle continues with the next pet instead of ending in a loss.
 */
class BattlePartyTest {

    private fun template(strong: Boolean): PetTemplate {
        val t = mock(PetTemplate::class.java)
        `when`(t.element).thenReturn("WOOD")
        `when`(t.skillElem).thenReturn(0)
        `when`(t.spriteId).thenReturn(1)
        `when`(t.name).thenReturn(if (strong) "Boss" else "Pet")
        `when`(t.catchRate).thenReturn(30)
        // GameFormula reads these linear coefficients.
        `when`(t.hpBase).thenReturn(if (strong) 5000 else 10)
        `when`(t.hpPer).thenReturn(0); `when`(t.hpFlat).thenReturn(0)
        `when`(t.atkBase).thenReturn(if (strong) 2000 else 1); `when`(t.atkPer).thenReturn(0); `when`(t.atkFlat).thenReturn(0)
        `when`(t.defBase).thenReturn(0); `when`(t.defPer).thenReturn(0); `when`(t.defFlat).thenReturn(0)
        `when`(t.spdBase).thenReturn(if (strong) 500 else 0); `when`(t.spdPer).thenReturn(0); `when`(t.spdFlat).thenReturn(0)
        return t
    }

    private fun pet(id: Long, slot: Int, hp: Int, name: String): PlayerPet {
        val tmpl = template(strong = false)   // build fully before stubbing p
        val p = mock(PlayerPet::class.java)
        `when`(p.id).thenReturn(id)
        `when`(p.slot).thenReturn(slot.toShort())
        `when`(p.hp).thenReturn(hp); `when`(p.hpMax).thenReturn(hp)
        `when`(p.atk).thenReturn(1); `when`(p.def).thenReturn(0); `when`(p.spd).thenReturn(1)  // weak + slow
        `when`(p.level).thenReturn(5)
        `when`(p.nickname).thenReturn(name)
        `when`(p.template).thenReturn(tmpl)
        return p
    }

    @Test
    fun `a fainted pet is replaced by the next alive party member`() {
        val playerRepo = mock(PlayerRepository::class.java)
        val petRepo = mock(PlayerPetRepository::class.java)
        val tmplRepo = mock(PetTemplateRepository::class.java)
        val itemRepo = mock(PlayerItemRepository::class.java)
        val logRepo = mock(BattleLogRepository::class.java)
        val badgeRepo = mock(BadgeRepository::class.java)
        val pbRepo = mock(PlayerBadgeRepository::class.java)
        val enemyRepo = mock(NpcEnemyTemplateRepository::class.java)
        val skillRepo = mock(SkillRepository::class.java)
        `when`(skillRepo.findByElement(0)).thenReturn(emptyList())
        val skillService = SkillService(skillRepo)

        val pet1 = pet(1L, 0, 10, "P1")
        val pet2 = pet(2L, 1, 20, "P2")
        // findByIdOrNull is an extension over findById -> stub findById.
        `when`(petRepo.findByPlayerIdAndSlot(100L, 0)).thenReturn(Optional.of(pet1))
        `when`(petRepo.findById(1L)).thenReturn(Optional.of(pet1))
        `when`(petRepo.findById(2L)).thenReturn(Optional.of(pet2))
        `when`(petRepo.findByPlayerIdOrdered(100L)).thenReturn(listOf(pet1, pet2))
        `when`(petRepo.save(any(PlayerPet::class.java))).thenAnswer { it.getArgument(0) }
        val enemyTemplate = template(strong = true)
        `when`(tmplRepo.findById(9.toShort())).thenReturn(Optional.of(enemyTemplate))

        val svc = BattleService(playerRepo, petRepo, tmplRepo, itemRepo, logRepo, badgeRepo, pbRepo, enemyRepo, skillService)
        val session = svc.startPveBattle(100L, 9, 5)

        // pet1 is slow + frail; the strong enemy KOs it first, so the turn must end with
        // pet2 in play and the battle still ongoing.
        val result = svc.processTurn(100L, BattleAction(session.battleId, "ATTACK"))

        assertEquals("ONGOING", result.status, "team should survive after one pet faints")
        assertTrue(result.log.any { it.contains("đã gục") }, "the fainted pet should be reported")
        assertTrue(result.log.any { it.contains("Tung") }, "the next pet should be sent out")
        assertEquals(20, result.playerPetHp, "HP shown should be the new active pet's")
    }
}
