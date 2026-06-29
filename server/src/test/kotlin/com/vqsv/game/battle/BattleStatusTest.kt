package com.vqsv.game.battle

import com.vqsv.dto.BattleAction
import com.vqsv.entity.PetTemplate
import com.vqsv.entity.PlayerPet
import com.vqsv.entity.Skill
import com.vqsv.repository.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

/** PvE skill behaviour: a self-buff spends the turn buffing (the enemy still acts). */
class BattleStatusTest {

    private fun template(): PetTemplate {
        val t = mock(PetTemplate::class.java)
        `when`(t.element).thenReturn("WOOD")
        `when`(t.skillElem).thenReturn(0)
        `when`(t.spriteId).thenReturn(1)
        `when`(t.name).thenReturn("Pet")
        return t
    }

    private fun pet(): PlayerPet {
        val tmpl = template()
        val p = mock(PlayerPet::class.java)
        `when`(p.id).thenReturn(1L)
        `when`(p.slot).thenReturn(0)
        `when`(p.hp).thenReturn(200); `when`(p.hpMax).thenReturn(200)
        `when`(p.atk).thenReturn(20); `when`(p.def).thenReturn(10); `when`(p.spd).thenReturn(5)
        `when`(p.level).thenReturn(5)
        `when`(p.nickname).thenReturn("Pet")
        `when`(p.template).thenReturn(tmpl)
        return p
    }

    @Test
    fun `a self-buff skill buffs the pet and lets the enemy act, without striking`() {
        val petRepo = mock(PlayerPetRepository::class.java)
        val tmplRepo = mock(PetTemplateRepository::class.java)
        val skillRepo = mock(SkillRepository::class.java)
        // behavior_flag = 1, effect_id = 0 -> DEF_UP self buff (power 0).
        val defBuff = Skill(id = 0, name = "Đằng chi bích lũy", element = 0, requiredLevel = 1, spCost = 5,
            power = 0, effectId = 0, behaviorFlag = 1, targetCode = 1)
        `when`(skillRepo.findByElement(0)).thenReturn(listOf(defBuff))
        val skillService = SkillService(skillRepo)

        val hero = pet()
        `when`(petRepo.findByPlayerIdAndSlot(100L, 0)).thenReturn(Optional.of(hero))
        `when`(petRepo.findById(1L)).thenReturn(Optional.of(hero))
        `when`(petRepo.findByPlayerIdOrdered(100L)).thenReturn(listOf(hero))
        `when`(petRepo.save(any(PlayerPet::class.java))).thenAnswer { it.getArgument(0) }

        // A slow, weak wild enemy: it survives (no strike from us) and lands a hit.
        val enemyTmpl = mock(PetTemplate::class.java)
        `when`(enemyTmpl.element).thenReturn("FIRE")
        `when`(enemyTmpl.skillElem).thenReturn(0); `when`(enemyTmpl.spriteId).thenReturn(2)
        `when`(enemyTmpl.name).thenReturn("Quái")
        `when`(enemyTmpl.hpBase).thenReturn(100); `when`(enemyTmpl.hpPer).thenReturn(0); `when`(enemyTmpl.hpFlat).thenReturn(0)
        `when`(enemyTmpl.atkBase).thenReturn(10); `when`(enemyTmpl.atkPer).thenReturn(0); `when`(enemyTmpl.atkFlat).thenReturn(0)
        `when`(enemyTmpl.defBase).thenReturn(0); `when`(enemyTmpl.defPer).thenReturn(0); `when`(enemyTmpl.defFlat).thenReturn(0)
        `when`(enemyTmpl.spdBase).thenReturn(0); `when`(enemyTmpl.spdPer).thenReturn(0); `when`(enemyTmpl.spdFlat).thenReturn(0)
        `when`(enemyTmpl.catchRate).thenReturn(30)
        `when`(tmplRepo.findById(9.toShort())).thenReturn(Optional.of(enemyTmpl))

        val svc = BattleService(
            mock(PlayerRepository::class.java), petRepo, tmplRepo,
            mock(PlayerItemRepository::class.java), mock(BattleLogRepository::class.java),
            mock(BadgeRepository::class.java), mock(PlayerBadgeRepository::class.java),
            mock(NpcEnemyTemplateRepository::class.java), skillService
        )
        val session = svc.startPveBattle(100L, 9, 5)
        val enemyHpStart = session.enemyHp

        val r = svc.processTurn(100L, BattleAction(session.battleId, "SKILL", itemId = 0))

        assertEquals("ONGOING", r.status)
        assertTrue(r.log.any { it.contains("tăng Phòng Ngự") }, "the buff should be reported")
        assertEquals(enemyHpStart, r.enemyHp, "a self-buff does not damage the enemy")
        assertTrue(r.log.any { it.contains("tấn công") }, "the enemy still gets to act")
        assertTrue(r.playerPetHp < 200, "the enemy's hit landed on us")
    }
}
