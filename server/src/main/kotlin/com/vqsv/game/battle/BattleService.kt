package com.vqsv.game.battle

import com.vqsv.dto.BattleAction
import com.vqsv.dto.BattleTurnResult
import com.vqsv.entity.*
import com.vqsv.repository.*
import com.vqsv.util.GameFormula
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class BattleSession(
    val battleId: String = UUID.randomUUID().toString(),
    val playerId: Long,
    val playerPetId: Long,
    val battleType: String,         // PVE | PVP
    val enemyPlayerId: Long? = null,
    val enemyTemplateId: Short? = null,
    var enemyLevel: Short = 1,
    var enemyHp: Int = 0,
    var enemyHpMax: Int = 0,
    var enemyAtk: Short = 0,
    var enemyDef: Short = 0,
    var enemySpd: Short = 0,
    var enemyElement: String = "FIRE",
    var enemySpriteId: Short = 0,
    var enemyName: String = "",
    var playerPetCurrentHp: Int = 0,
    var turn: Int = 0,
    var status: String = "ONGOING",  // ONGOING | WIN | LOSE | RUN | CAUGHT
    val catchable: Boolean = false,
    val mapWildPetId: Int? = null,
    // Skill / SP state for the player's active pet (SP refills each battle).
    val playerPetSkillElem: Short = 0,
    val playerPetLevel: Int = 1,
    var playerPetSp: Int = 0,
    var playerPetSpMax: Int = 0,
    // Fixed rewards for TRAINER battles (0 = derive from level, as for wild PVE).
    val enemyExpReward: Int = 0,
    val enemyGoldReward: Int = 0,
    val trainerName: String = ""
)

@Service
class BattleService(
    private val playerRepo: PlayerRepository,
    private val playerPetRepo: PlayerPetRepository,
    private val petTemplateRepo: PetTemplateRepository,
    private val playerItemRepo: PlayerItemRepository,
    private val battleLogRepo: BattleLogRepository,
    private val badgeRepo: BadgeRepository,
    private val playerBadgeRepo: PlayerBadgeRepository,
    private val npcEnemyTemplateRepo: NpcEnemyTemplateRepository,
    private val skillService: SkillService
) {
    // In-memory battle sessions (can move to Redis for multi-node)
    private val activeBattles = ConcurrentHashMap<String, BattleSession>()

    @Transactional
    fun startPveBattle(playerId: Long, templateId: Short, level: Short, wildPetId: Int? = null): BattleSession {
        val template = petTemplateRepo.findByIdOrNull(templateId)
            ?: throw IllegalArgumentException("Pet template không tồn tại")

        val playerPet = playerPetRepo.findByPlayerIdAndSlot(playerId, 0)
            .orElseThrow { IllegalStateException("Không có sủng vật đang chiến đấu") }

        if (playerPet.hp <= 0)
            throw IllegalStateException("Sủng vật của bạn đã kiệt sức!")

        val session = BattleSession(
            playerId = playerId,
            playerPetId = playerPet.id,
            battleType = "PVE",
            enemyTemplateId = templateId,
            enemyLevel = level,
            enemyHp = GameFormula.petHpMax(template, level.toInt()),
            enemyHpMax = GameFormula.petHpMax(template, level.toInt()),
            enemyAtk = GameFormula.petAtk(template, level.toInt()).toShort(),
            enemyDef = GameFormula.petDef(template, level.toInt()).toShort(),
            enemySpd = GameFormula.petSpd(template, level.toInt()).toShort(),
            enemyElement = template.element,
            enemySpriteId = template.spriteId,
            enemyName = template.name,
            playerPetCurrentHp = playerPet.hp,
            catchable = wildPetId != null,
            mapWildPetId = wildPetId,
            playerPetSkillElem = playerPet.template.skillElem,
            playerPetLevel = playerPet.level.toInt(),
            playerPetSp = skillService.spMax(playerPet.level.toInt()),
            playerPetSpMax = skillService.spMax(playerPet.level.toInt())
        )

        activeBattles[session.battleId] = session
        return session
    }

    /**
     * Start a duel against an NPC trainer (the original game's offline trainer
     * battles). It is a normal PVE fight that is NOT catchable and grants the
     * trainer's fixed exp/gold reward on victory.
     */
    @Transactional
    fun startTrainerBattle(playerId: Long, trainerId: Short): BattleSession {
        val trainer = npcEnemyTemplateRepo.findByIdOrNull(trainerId)
            ?: throw IllegalArgumentException("Huấn luyện viên không tồn tại")

        val playerPet = playerPetRepo.findByPlayerIdAndSlot(playerId, 0)
            .orElseThrow { IllegalStateException("Không có sủng vật đang chiến đấu") }

        if (playerPet.hp <= 0)
            throw IllegalStateException("Sủng vật của bạn đã kiệt sức!")

        val session = BattleSession(
            playerId = playerId,
            playerPetId = playerPet.id,
            battleType = "TRAINER",
            enemyTemplateId = null,
            enemyLevel = trainer.level,
            enemyHp = trainer.hp,
            enemyHpMax = trainer.hp,
            enemyAtk = trainer.atk,
            enemyDef = trainer.def,
            enemySpd = trainer.spd,
            enemyElement = trainer.element,
            enemySpriteId = trainer.spriteId,
            enemyName = trainer.name,
            playerPetCurrentHp = playerPet.hp,
            catchable = false,
            enemyExpReward = trainer.expReward,
            enemyGoldReward = trainer.goldReward,
            trainerName = trainer.name,
            playerPetSkillElem = playerPet.template.skillElem,
            playerPetLevel = playerPet.level.toInt(),
            playerPetSp = skillService.spMax(playerPet.level.toInt()),
            playerPetSpMax = skillService.spMax(playerPet.level.toInt())
        )

        activeBattles[session.battleId] = session
        return session
    }

    @Transactional
    fun processTurn(playerId: Long, action: BattleAction): BattleTurnResult {
        val session = activeBattles[action.battleId]
            ?: throw IllegalArgumentException("Không tìm thấy trận đấu")
        if (session.playerId != playerId)
            throw SecurityException("Không phải trận đấu của bạn")
        if (session.status != "ONGOING")
            throw IllegalStateException("Trận đấu đã kết thúc")

        val log = mutableListOf<String>()
        val playerPet = playerPetRepo.findByIdOrNull(session.playerPetId)!!
        val playerPetTemplate = playerPet.template

        session.turn++

        when (action.action) {
            "ATTACK" -> {
                // Basic strike: pet's own element, 100% power, no SP cost.
                playerStrike(session, playerPet, log, playerPetTemplate.element, 100, "tấn công")
                    ?.let { return it }
            }

            "SKILL" -> {
                val skillId = action.itemId ?: throw IllegalArgumentException("Thiếu skill")
                val skill = skillService.usableSkill(session.playerPetSkillElem, session.playerPetLevel, skillId)
                    ?: throw IllegalArgumentException("Sủng vật chưa học kỹ năng này")
                if (session.playerPetSp < skill.spCost)
                    throw IllegalStateException("Không đủ SP để dùng ${skill.name}")
                session.playerPetSp -= skill.spCost.toInt()
                val power = if (skill.power.toInt() == 0) 100 else skill.power.toInt()
                val elementName = GameFormula.elementName(skill.element.toInt())
                playerStrike(session, playerPet, log, elementName, power, "dùng ${skill.name}")
                    ?.let { return it }
            }

            "USE_ITEM" -> {
                val itemId = action.itemId ?: throw IllegalArgumentException("Thiếu item_id")
                val playerItem = playerItemRepo.findByPlayerIdAndItemId(playerId, itemId)
                    .orElseThrow { IllegalArgumentException("Không có vật phẩm này") }

                val item = playerItem.item
                when (item.itemType) {
                    "MEDICINE" -> {
                        val heal = minOf(item.effectVal, session.playerPetCurrentHp.let { playerPet.hpMax - it })
                        session.playerPetCurrentHp = minOf(playerPet.hpMax, session.playerPetCurrentHp + item.effectVal)
                        log.add("Dùng ${item.name}! Hồi phục $heal HP!")
                    }
                    else -> throw IllegalArgumentException("Không thể dùng vật phẩm này trong chiến đấu")
                }

                // Consume item
                if (playerItem.quantity <= 1) {
                    playerItemRepo.delete(playerItem)
                } else {
                    playerItemRepo.save(playerItem.copy(quantity = playerItem.quantity - 1))
                }

                // Enemy still attacks
                val eMult = GameFormula.elementMult(session.enemyElement, playerPetTemplate.element)
                val eDmg = GameFormula.calcDamage(session.enemyAtk.toInt(), playerPet.def.toInt(), eMult)
                session.playerPetCurrentHp -= eDmg
                log.add("${session.enemyName} tấn công! Gây ${eDmg} sát thương!")

                if (session.playerPetCurrentHp <= 0) {
                    session.status = "LOSE"
                    return finalizeBattle(session, playerPet, log)
                }
            }

            "CATCH" -> {
                if (!session.catchable) throw IllegalArgumentException("Không thể bắt sủng vật này")
                val itemId = action.itemId ?: throw IllegalArgumentException("Thiếu item_id (cần Tất)")

                val playerItem = playerItemRepo.findByPlayerIdAndItemId(playerId, itemId)
                    .orElseThrow { IllegalArgumentException("Không có Tất bắt sủng vật") }

                val item = playerItem.item
                if (item.itemType != "CATCH_BALL") throw IllegalArgumentException("Đây không phải Tất bắt sủng vật")

                val hpPct = session.enemyHp.toFloat() / session.enemyHpMax
                val template = petTemplateRepo.findByIdOrNull(session.enemyTemplateId!!)!!
                val caught = GameFormula.catchSuccess(template.catchRate.toInt(), hpPct, item.effectVal)

                // Consume catch ball
                if (playerItem.quantity <= 1) playerItemRepo.delete(playerItem)
                else playerItemRepo.save(playerItem.copy(quantity = playerItem.quantity - 1))

                if (caught) {
                    session.status = "CAUGHT"
                    log.add("Đã bắt được ${template.name}!")
                    return finalizeBattle(session, playerPet, log)
                } else {
                    log.add("Bắt hụt! ${template.name} đã thoát!")
                    // Enemy retaliates
                    val eMult = GameFormula.elementMult(session.enemyElement, playerPetTemplate.element)
                    val eDmg = GameFormula.calcDamage(session.enemyAtk.toInt(), playerPet.def.toInt(), eMult)
                    session.playerPetCurrentHp -= eDmg
                    log.add("${session.enemyName} tấn công! Gây ${eDmg} sát thương!")

                    if (session.playerPetCurrentHp <= 0) {
                        session.status = "LOSE"
                        return finalizeBattle(session, playerPet, log)
                    }
                }
            }

            "RUN" -> {
                val runChance = GameFormula.fleeChance(
                    playerPet.spd.toInt(), session.enemySpd.toInt(),
                    playerPet.level.toInt(), session.enemyLevel.toInt()
                )
                if (Random.nextInt(100) < runChance) {
                    session.status = "RUN"
                    log.add("Chạy thoát thành công!")
                    activeBattles.remove(session.battleId)
                } else {
                    log.add("Chạy thoát thất bại!")
                    val eMult = GameFormula.elementMult(session.enemyElement, playerPetTemplate.element)
                    val eDmg = GameFormula.calcDamage(session.enemyAtk.toInt(), playerPet.def.toInt(), eMult)
                    session.playerPetCurrentHp -= eDmg
                    log.add("${session.enemyName} tấn công! Gây ${eDmg} sát thương!")
                }
            }

            else -> throw IllegalArgumentException("Hành động không hợp lệ")
        }

        // Sync HP back to pet entity each turn
        playerPetRepo.save(playerPet.copy(hp = session.playerPetCurrentHp.coerceAtLeast(0)))

        return BattleTurnResult(
            battleId = session.battleId,
            turn = session.turn,
            playerPetHp = session.playerPetCurrentHp.coerceAtLeast(0),
            enemyHp = session.enemyHp.coerceAtLeast(0),
            log = log,
            status = session.status
        )
    }

    /**
     * One player action (attack or skill) plus the enemy's counter, resolved in speed
     * order. [elementName]/[skillPower] let a skill override the pet's element/power.
     * Returns a finalized result if the battle ended, else null to continue the turn.
     */
    private fun playerStrike(
        session: BattleSession, playerPet: PlayerPet, log: MutableList<String>,
        elementName: String, skillPower: Int, label: String
    ): BattleTurnResult? {
        val tpl = playerPet.template
        val name = playerPet.nickname ?: tpl.name
        fun playerHit(prefix: String) {
            val crit = GameFormula.isCrit(playerPet.spd.toInt())
            val mult = GameFormula.elementMult(elementName, session.enemyElement)
            val dmg = GameFormula.calcDamage(playerPet.atk.toInt(), session.enemyDef.toInt(), mult, crit, skillPower)
            session.enemyHp -= dmg
            log.add("$name $prefix! Gây $dmg sát thương${if (crit) " (Chí mạng!)" else ""}!")
        }
        fun enemyHit(prefix: String) {
            val eMult = GameFormula.elementMult(session.enemyElement, tpl.element)
            val eDmg = GameFormula.calcDamage(session.enemyAtk.toInt(), playerPet.def.toInt(), eMult)
            session.playerPetCurrentHp -= eDmg
            log.add("${session.enemyName} $prefix! Gây $eDmg sát thương!")
        }
        if (playerPet.spd >= session.enemySpd) {
            playerHit(label)
            if (session.enemyHp <= 0) { session.status = "WIN"; return finalizeBattle(session, playerPet, log) }
            enemyHit("phản công")
        } else {
            enemyHit("tấn công trước")
            if (session.playerPetCurrentHp <= 0) { session.status = "LOSE"; return finalizeBattle(session, playerPet, log) }
            playerHit("phản công")
            if (session.enemyHp <= 0) { session.status = "WIN"; return finalizeBattle(session, playerPet, log) }
        }
        if (session.playerPetCurrentHp <= 0) { session.status = "LOSE"; return finalizeBattle(session, playerPet, log) }
        return null
    }

    @Transactional
    private fun finalizeBattle(session: BattleSession, playerPet: PlayerPet, log: MutableList<String>): BattleTurnResult {
        val player = playerRepo.findByIdOrNull(session.playerId)!!

        when (session.status) {
            "WIN" -> {
                // Trainer battles grant a fixed reward; wild battles derive from level.
                val expGain: Int
                val goldGain: Int
                if (session.battleType == "TRAINER") {
                    expGain = session.enemyExpReward
                    goldGain = session.enemyGoldReward
                } else {
                    expGain = GameFormula.battleExpReward(20 * session.enemyLevel, player.level.toInt(), session.enemyLevel.toInt())
                    goldGain = session.enemyLevel * 5
                }

                // Level up pet
                val newPetExp = playerPet.exp + expGain
                val petLevelAfter = calcNewLevel(playerPet.level.toInt(), newPetExp)
                val updatedPet = playerPet.copy(
                    exp = newPetExp % GameFormula.expForLevel(petLevelAfter),
                    level = petLevelAfter.toShort(),
                    hp = session.playerPetCurrentHp.coerceAtLeast(0),
                    atk = GameFormula.petAtk(playerPet.template, petLevelAfter).toShort(),
                    def = GameFormula.petDef(playerPet.template, petLevelAfter).toShort(),
                    spd = GameFormula.petSpd(playerPet.template, petLevelAfter).toShort(),
                    hpMax = GameFormula.petHpMax(playerPet.template, petLevelAfter)
                )
                playerPetRepo.save(updatedPet)

                // Player EXP
                val newPlayerExp = player.exp + expGain / 2
                val playerLevelAfter = calcNewLevel(player.level.toInt(), newPlayerExp)
                playerRepo.save(player.copy(
                    exp = newPlayerExp % GameFormula.expForLevel(playerLevelAfter),
                    level = playerLevelAfter.toShort(),
                    kimTien = player.kimTien + goldGain
                ))

                log.add("Thắng! +${expGain} EXP, +${goldGain} kim tiền")
                if (petLevelAfter > playerPet.level) log.add("${updatedPet.nickname ?: updatedPet.template.name} lên cấp ${petLevelAfter}!")
                if (playerLevelAfter > player.level) log.add("Nhân vật lên cấp ${playerLevelAfter}!")

                // Check evolve
                val tpl = updatedPet.template
                if (tpl.evolveInto != null && petLevelAfter >= (tpl.evolveLv?.toInt() ?: 999)) {
                    log.add("${updatedPet.nickname ?: updatedPet.template.name} đang tiến hóa...")
                }

                battleLogRepo.save(BattleLog(
                    attackerId = session.playerId,
                    winnerId = session.playerId,
                    battleType = session.battleType,
                    turns = session.turn.toShort(),
                    expGained = expGain,
                    goldGained = goldGain
                ))

                checkBattleBadges(player.id)
            }

            "CAUGHT" -> {
                val template = petTemplateRepo.findByIdOrNull(session.enemyTemplateId!!)!!
                val newPet = PlayerPet(
                    player = player,
                    template = template,
                    level = session.enemyLevel,
                    hp = session.enemyHp.coerceAtLeast(1),
                    hpMax = session.enemyHpMax,
                    atk = session.enemyAtk,
                    def = session.enemyDef,
                    spd = session.enemySpd,
                    slot = playerPetRepo.countByPlayerId(player.id).toShort().coerceAtMost(5)
                )
                playerPetRepo.save(newPet)
                log.add("${template.name} đã gia nhập đội của bạn!")
                checkCatchBadges(player.id)
            }

            "LOSE" -> {
                playerPetRepo.save(playerPet.copy(hp = 0))
                playerRepo.save(player.copy(hp = (player.hp - 10).coerceAtLeast(0)))
                log.add("Thua! Sủng vật cần nghỉ ngơi.")
                battleLogRepo.save(BattleLog(
                    attackerId = session.playerId,
                    battleType = session.battleType,
                    turns = session.turn.toShort()
                ))
            }
        }

        activeBattles.remove(session.battleId)
        return BattleTurnResult(
            battleId = session.battleId,
            turn = session.turn,
            playerPetHp = session.playerPetCurrentHp.coerceAtLeast(0),
            enemyHp = session.enemyHp.coerceAtLeast(0),
            log = log,
            status = session.status
        )
    }

    private fun calcNewLevel(currentLevel: Int, totalExp: Int): Int {
        var lv = currentLevel
        var exp = totalExp
        while (lv < GameFormula.LEVEL_CAP) {
            val need = GameFormula.expForLevel(lv)
            if (exp >= need) { exp -= need; lv++ } else break
        }
        return lv
    }

    private fun checkBattleBadges(playerId: Long) {
        val wins = battleLogRepo.countByAttackerIdAndWinnerId(playerId, playerId)
        val badgeConditions = mapOf(10L to 4.toShort(), 100L to 5.toShort())
        badgeConditions.forEach { (threshold, badgeId) ->
            if (wins >= threshold) {
                val badge = badgeRepo.findByIdOrNull(badgeId) ?: return@forEach
                val key = PlayerBadgeId(playerId, badgeId)
                if (!playerBadgeRepo.existsById(key)) {
                    val player = playerRepo.findByIdOrNull(playerId)!!
                    playerBadgeRepo.save(PlayerBadge(player = player, badge = badge))
                }
            }
        }
    }

    private fun checkCatchBadges(playerId: Long) {
        val petCount = playerPetRepo.countByPlayerId(playerId)
        val badgeConditions = mapOf(1L to 2.toShort(), 50L to 3.toShort())
        badgeConditions.forEach { (threshold, badgeId) ->
            if (petCount >= threshold) {
                val badge = badgeRepo.findByIdOrNull(badgeId) ?: return@forEach
                val key = PlayerBadgeId(playerId, badgeId)
                if (!playerBadgeRepo.existsById(key)) {
                    val player = playerRepo.findByIdOrNull(playerId)!!
                    playerBadgeRepo.save(PlayerBadge(player = player, badge = badge))
                }
            }
        }
    }

    fun getSession(battleId: String): BattleSession? = activeBattles[battleId]
}
