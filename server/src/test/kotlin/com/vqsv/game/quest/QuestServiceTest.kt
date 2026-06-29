package com.vqsv.game.quest

import com.vqsv.entity.Player
import com.vqsv.entity.PlayerQuest
import com.vqsv.entity.PlayerQuestId
import com.vqsv.entity.Quest
import com.vqsv.repository.ItemRepository
import com.vqsv.repository.PlayerItemRepository
import com.vqsv.repository.PlayerQuestRepository
import com.vqsv.repository.PlayerRepository
import com.vqsv.repository.QuestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

/**
 * Quest rules with all repositories mocked (no DB). Covers the hunt loop end to end:
 * recording a kill completes a quest, and claiming grants the reward exactly once.
 */
class QuestServiceTest {

    private val questRepo = mock(QuestRepository::class.java)
    private val playerQuestRepo = mock(PlayerQuestRepository::class.java)
    private val playerRepo = mock(PlayerRepository::class.java)
    private val playerItemRepo = mock(PlayerItemRepository::class.java)
    private val itemRepo = mock(ItemRepository::class.java)
    private val svc = QuestService(questRepo, playerQuestRepo, playerRepo, playerItemRepo, itemRepo)

    private fun killQuest(count: Int = 2) = Quest(
        id = 1, name = "Săn", giverNpcId = 7, objectiveType = "KILL_MOB",
        objectiveTarget = 1, objectiveCount = count, rewardGold = 100, rewardExp = 50, requiredLevel = 1
    )

    @Test
    fun `recording the final kill completes the quest`() {
        val pq = PlayerQuest(playerId = 1, questId = 1, progress = 1, status = "IN_PROGRESS")
        `when`(playerQuestRepo.findByPlayerIdAndStatus(1L, "IN_PROGRESS")).thenReturn(listOf(pq))
        `when`(questRepo.findById(1)).thenReturn(Optional.of(killQuest(count = 2)))

        svc.recordKill(1L, 1)

        assertEquals(2, pq.progress)
        assertEquals("COMPLETED", pq.status, "reaching the target count completes the quest")
    }

    @Test
    fun `a kill for a different monster does not advance the quest`() {
        val pq = PlayerQuest(playerId = 1, questId = 1, progress = 0, status = "IN_PROGRESS")
        `when`(playerQuestRepo.findByPlayerIdAndStatus(1L, "IN_PROGRESS")).thenReturn(listOf(pq))
        `when`(questRepo.findById(1)).thenReturn(Optional.of(killQuest(count = 2)))

        svc.recordKill(1L, 99)  // wrong template id

        assertEquals(0, pq.progress)
        assertEquals("IN_PROGRESS", pq.status)
    }

    @Test
    fun `claiming a completed quest grants gold and exp once`() {
        val pq = PlayerQuest(playerId = 1, questId = 1, progress = 2, status = "COMPLETED")
        val player = Player(id = 1, level = 5, exp = 0, kimTien = 100)
        `when`(questRepo.findById(1)).thenReturn(Optional.of(killQuest(count = 2)))
        `when`(playerQuestRepo.findById(PlayerQuestId(1, 1))).thenReturn(Optional.of(pq))
        `when`(playerRepo.findById(1L)).thenReturn(Optional.of(player))

        val result = svc.claim(1L, 1)

        assertNotNull(result)
        assertEquals(100, result!!.rewardGold)
        val saved = ArgumentCaptor.forClass(Player::class.java)
        org.mockito.Mockito.verify(playerRepo).save(saved.capture())
        assertEquals(200, saved.value.kimTien, "gold reward added")
        assertEquals(50, saved.value.exp, "exp reward added")
        assertEquals("CLAIMED", pq.status, "the quest is marked claimed")
    }

    @Test
    fun `claiming before completion is rejected`() {
        val pq = PlayerQuest(playerId = 1, questId = 1, progress = 1, status = "IN_PROGRESS")
        `when`(questRepo.findById(1)).thenReturn(Optional.of(killQuest(count = 2)))
        `when`(playerQuestRepo.findById(PlayerQuestId(1, 1))).thenReturn(Optional.of(pq))
        `when`(playerRepo.findById(1L)).thenReturn(Optional.of(Player(id = 1, level = 5)))

        assertNull(svc.claim(1L, 1), "an unfinished quest cannot be claimed")
    }

    @Test
    fun `a REACH_LEVEL quest completes dynamically when the player is high enough`() {
        val quest = Quest(id = 2, name = "Trưởng thành", giverNpcId = 7, objectiveType = "REACH_LEVEL",
            objectiveTarget = 5, objectiveCount = 1, rewardGold = 200, rewardExp = 0, requiredLevel = 1)
        val pq = PlayerQuest(playerId = 1, questId = 2, progress = 0, status = "IN_PROGRESS")
        `when`(questRepo.findById(2)).thenReturn(Optional.of(quest))
        `when`(playerQuestRepo.findByPlayerId(1L)).thenReturn(listOf(pq))
        `when`(playerRepo.findById(1L)).thenReturn(Optional.of(Player(id = 1, level = 6)))

        val log = svc.log(1L)
        assertEquals(1, log.size)
        assertEquals("COMPLETED", log[0].status, "level 6 >= target 5 completes it")
    }

    @Test
    fun `availableFrom hides quests above the player level and shows eligible ones`() {
        val quest = killQuest().copy(requiredLevel = 10)
        `when`(questRepo.findByGiverNpcId(7)).thenReturn(listOf(quest))
        `when`(playerQuestRepo.findByPlayerId(1L)).thenReturn(emptyList())
        `when`(playerRepo.findById(1L)).thenReturn(Optional.of(Player(id = 1, level = 5)))

        assertTrue(svc.availableFrom(1L, 7).isEmpty(), "a level-10 quest is hidden from a level-5 player")

        `when`(playerRepo.findById(1L)).thenReturn(Optional.of(Player(id = 1, level = 12)))
        assertEquals(1, svc.availableFrom(1L, 7).size, "now eligible")
    }
}
