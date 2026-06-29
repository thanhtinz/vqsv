package com.vqsv.game.quest

import com.vqsv.dto.QuestClaimResult
import com.vqsv.dto.QuestDto
import com.vqsv.entity.PlayerItem
import com.vqsv.entity.PlayerQuest
import com.vqsv.entity.Quest
import com.vqsv.repository.ItemRepository
import com.vqsv.repository.PlayerItemRepository
import com.vqsv.repository.PlayerQuestRepository
import com.vqsv.repository.PlayerRepository
import com.vqsv.repository.QuestRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * NPC quests, faithful to the original loop: an NPC hands out a quest, the player
 * meets the objective (hunt monsters / collect items / reach a level), then returns
 * to claim the reward. Quests can chain via a prerequisite.
 *
 *  - KILL_MOB   : kill `count` of pet template `target` (recorded on battle win)
 *  - COLLECT_ITEM: hold `count` of item `target` (consumed when claimed)
 *  - REACH_LEVEL: reach character level `target`
 */
@Service
class QuestService(
    private val questRepo: QuestRepository,
    private val playerQuestRepo: PlayerQuestRepository,
    private val playerRepo: PlayerRepository,
    private val playerItemRepo: PlayerItemRepository,
    private val itemRepo: ItemRepository
) {

    /** Quests this NPC offers that the player can take right now. */
    fun availableFrom(playerId: Long, npcId: Short): List<QuestDto> {
        val player = playerRepo.findByIdOrNull(playerId) ?: return emptyList()
        val taken = playerQuestRepo.findByPlayerId(playerId).associateBy { it.questId }
        return questRepo.findByGiverNpcId(npcId)
            .filter { canTake(it, player.level.toInt(), taken) }
            .map { dto(it, null, player.level.toInt(), playerId) }
    }

    /** The player's whole quest log (active + completed + claimed). */
    fun log(playerId: Long): List<QuestDto> {
        val player = playerRepo.findByIdOrNull(playerId) ?: return emptyList()
        return playerQuestRepo.findByPlayerId(playerId).mapNotNull { pq ->
            questRepo.findByIdOrNull(pq.questId)?.let { dto(it, pq, player.level.toInt(), playerId) }
        }
    }

    /** Take a quest. No-op if already taken or not eligible. */
    @Transactional
    fun accept(playerId: Long, questId: Short): QuestDto? {
        val quest = questRepo.findByIdOrNull(questId) ?: return null
        val player = playerRepo.findByIdOrNull(playerId) ?: return null
        val taken = playerQuestRepo.findByPlayerId(playerId).associateBy { it.questId }
        if (!canTake(quest, player.level.toInt(), taken)) return null
        val pq = playerQuestRepo.save(PlayerQuest(playerId = playerId, questId = questId, progress = 0, status = "IN_PROGRESS"))
        return dto(quest, pq, player.level.toInt(), playerId)
    }

    /**
     * Record a monster kill toward any in-progress KILL_MOB quest for that template.
     * Called when a wild battle is won. Safe to call with no matching quests.
     */
    @Transactional
    fun recordKill(playerId: Long, petTemplateId: Short) {
        playerQuestRepo.findByPlayerIdAndStatus(playerId, "IN_PROGRESS").forEach { pq ->
            val quest = questRepo.findByIdOrNull(pq.questId) ?: return@forEach
            if (quest.objectiveType == "KILL_MOB" && quest.objectiveTarget == petTemplateId) {
                pq.progress = (pq.progress + 1).coerceAtMost(quest.objectiveCount)
                if (pq.progress >= quest.objectiveCount) pq.status = "COMPLETED"
                playerQuestRepo.save(pq)
            }
        }
    }

    /** Claim a finished quest's reward. Returns null if the quest isn't actually complete. */
    @Transactional
    fun claim(playerId: Long, questId: Short): QuestClaimResult? {
        val quest = questRepo.findByIdOrNull(questId) ?: return null
        val pq = playerQuestRepo.findByIdOrNull(com.vqsv.entity.PlayerQuestId(playerId, questId)) ?: return null
        if (pq.status == "CLAIMED") return null
        val player = playerRepo.findByIdOrNull(playerId) ?: return null
        if (!isComplete(quest, pq, player.level.toInt(), playerId)) return null

        // COLLECT_ITEM consumes the required items on claim.
        if (quest.objectiveType == "COLLECT_ITEM") {
            val owned = playerItemRepo.findByPlayerIdAndItemId(playerId, quest.objectiveTarget).orElse(null)
            if (owned == null || owned.quantity < quest.objectiveCount) return null
            val left = owned.quantity - quest.objectiveCount
            if (left <= 0) playerItemRepo.delete(owned) else playerItemRepo.save(owned.copy(quantity = left))
        }

        // Grant rewards: gold + character exp + optional item.
        playerRepo.save(player.copy(
            kimTien = player.kimTien + quest.rewardGold,
            exp = player.exp + quest.rewardExp
        ))
        quest.rewardItemId?.let { itemId ->
            val item = itemRepo.findByIdOrNull(itemId)
            if (item != null) {
                val existing = playerItemRepo.findByPlayerIdAndItemId(playerId, itemId).orElse(null)
                if (existing != null) playerItemRepo.save(existing.copy(quantity = existing.quantity + 1))
                else playerItemRepo.save(PlayerItem(player = player, item = item, quantity = 1))
            }
        }

        pq.status = "CLAIMED"
        playerQuestRepo.save(pq)
        return QuestClaimResult(questId, quest.rewardGold, quest.rewardExp, quest.rewardItemId,
            "Hoàn thành \"${quest.name}\"! +${quest.rewardGold} kim tiền, +${quest.rewardExp} EXP")
    }

    // ---- helpers ----

    private fun canTake(quest: Quest, level: Int, taken: Map<Short, PlayerQuest>): Boolean {
        if (taken.containsKey(quest.id)) return false
        if (level < quest.requiredLevel) return false
        val prereq = quest.prerequisiteQuestId ?: return true
        return taken[prereq]?.status == "CLAIMED"
    }

    private fun currentProgress(quest: Quest, pq: PlayerQuest?, level: Int, playerId: Long): Int = when (quest.objectiveType) {
        "KILL_MOB" -> pq?.progress ?: 0
        "COLLECT_ITEM" -> playerItemRepo.findByPlayerIdAndItemId(playerId, quest.objectiveTarget).map { it.quantity }.orElse(0)
        "REACH_LEVEL" -> level
        else -> pq?.progress ?: 0
    }

    private fun isComplete(quest: Quest, pq: PlayerQuest?, level: Int, playerId: Long): Boolean = when (quest.objectiveType) {
        "REACH_LEVEL" -> level >= quest.objectiveTarget
        else -> currentProgress(quest, pq, level, playerId) >= quest.objectiveCount
    }

    private fun dto(quest: Quest, pq: PlayerQuest?, level: Int, playerId: Long): QuestDto {
        val progress = currentProgress(quest, pq, level, playerId)
        val status = when {
            pq == null -> "AVAILABLE"
            pq.status == "CLAIMED" -> "CLAIMED"
            isComplete(quest, pq, level, playerId) -> "COMPLETED"
            else -> "IN_PROGRESS"
        }
        return QuestDto(
            id = quest.id, name = quest.name, description = quest.description,
            giverNpcId = quest.giverNpcId, objectiveType = quest.objectiveType,
            objectiveTarget = quest.objectiveTarget, objectiveCount = quest.objectiveCount,
            progress = progress.coerceAtMost(quest.objectiveCount),
            rewardGold = quest.rewardGold, rewardExp = quest.rewardExp, rewardItemId = quest.rewardItemId,
            status = status
        )
    }
}
