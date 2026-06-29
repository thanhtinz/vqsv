package com.vqsv.game.battle

import com.vqsv.repository.PlayerPetRepository
import com.vqsv.util.GameFormula
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Player-vs-player battles. Each side uses its active pet (slot 0). Rounds are
 * simultaneous: both players submit an action, then the round resolves in SPD
 * order. Action 0 = attack, 3 = forfeit (immediate loss).
 *
 * The gateway owns the sockets; this service owns the rules and returns a
 * [RoundResult] once a round is decided (or null while waiting for the opponent).
 */
@Service
class PvpService(
    private val playerPetRepo: PlayerPetRepository,
    private val skillService: SkillService
) {

    class Combatant(
        val playerId: Long, val name: String,
        var hp: Int, val hpMax: Int,
        val atk: Int, val def: Int, val spd: Int,
        val element: String, val spriteId: Short,
        val skillElem: Short, val level: Int,
        var sp: Int, val spMax: Int,
        var action: Int = -1, var skillId: Short = -1
    )

    class Session(val battleId: String, val a: Combatant, val b: Combatant, var status: String = "ONGOING")

    data class RoundResult(
        val battleId: String,
        val aId: Long, val bId: Long,
        val aHp: Int, val bHp: Int,
        val log: List<String>,
        val status: String   // ONGOING | A_WIN | B_WIN
    )

    private val sessions = ConcurrentHashMap<String, Session>()

    fun isPvp(battleId: String): Boolean = sessions.containsKey(battleId)
    fun session(battleId: String): Session? = sessions[battleId]

    /** Build a session from both players' active pets. Returns null if either lacks a pet. */
    fun start(p1Id: Long, p1Name: String, p2Id: Long, p2Name: String): Session? {
        val a = combatant(p1Id, p1Name) ?: return null
        val b = combatant(p2Id, p2Name) ?: return null
        val s = Session(UUID.randomUUID().toString(), a, b)
        sessions[s.battleId] = s
        return s
    }

    private fun combatant(playerId: Long, name: String): Combatant? {
        val pet = playerPetRepo.findByPlayerIdAndSlot(playerId, 0).orElse(null) ?: return null
        val spMax = skillService.spMax(pet.level.toInt())
        return Combatant(
            playerId = playerId, name = name,
            hp = pet.hpMax, hpMax = pet.hpMax,
            atk = pet.atk.toInt(), def = pet.def.toInt(), spd = pet.spd.toInt(),
            element = pet.template.element, spriteId = pet.template.spriteId,
            skillElem = pet.template.skillElem, level = pet.level.toInt(),
            sp = spMax, spMax = spMax
        )
    }

    /**
     * Submit a player's action (0 = attack, 3 = forfeit, 4 = skill with [skillId]).
     * Returns a RoundResult when the round resolves, else null while waiting.
     */
    @Synchronized
    fun submitAction(battleId: String, playerId: Long, action: Int, skillId: Int = -1): RoundResult? {
        val s = sessions[battleId] ?: return null
        if (s.status != "ONGOING") return null
        val me = if (s.a.playerId == playerId) s.a else if (s.b.playerId == playerId) s.b else return null

        // Forfeit resolves immediately.
        if (action == 3) {
            s.status = if (me === s.a) "B_WIN" else "A_WIN"
            return finish(s, listOf("${me.name} đã bỏ cuộc!"))
        }
        me.action = action
        me.skillId = skillId.toShort()
        if (s.a.action < 0 || s.b.action < 0) return null   // wait for the opponent

        // Both acted — both strike this round, faster first; a KO stops the round.
        val log = ArrayList<String>()
        val order = if (s.a.spd >= s.b.spd) listOf(s.a to s.b, s.b to s.a)
                    else listOf(s.b to s.a, s.a to s.b)
        for ((atk, def) in order) {
            if (atk.hp <= 0) continue
            val crit = GameFormula.isCrit(atk.spd)
            // A skill overrides element + power when known and SP allows; else basic hit.
            var power = 100
            var elementName = atk.element
            if (atk.action == 4) {
                val skill = skillService.usableSkill(atk.skillElem, atk.level, atk.skillId)
                if (skill != null && atk.sp >= skill.spCost) {
                    atk.sp -= skill.spCost.toInt()
                    power = if (skill.power.toInt() == 0) 100 else skill.power.toInt()
                    elementName = GameFormula.elementName(skill.element.toInt())
                    log.add("${atk.name} dùng ${skill.name}!")
                }
            }
            val mult = GameFormula.elementMult(elementName, def.element)
            val dmg = GameFormula.calcDamage(atk.atk, def.def, mult, crit, power)
            def.hp -= dmg
            log.add("${atk.name} đánh ${def.name}: $dmg sát thương${if (crit) " (Chí mạng!)" else ""}")
            if (def.hp <= 0) { log.add("${def.name} đã gục!"); break }
        }
        s.a.action = -1; s.b.action = -1
        s.a.skillId = -1; s.b.skillId = -1
        s.status = when {
            s.a.hp <= 0 -> "B_WIN"
            s.b.hp <= 0 -> "A_WIN"
            else -> "ONGOING"
        }
        return finish(s, log)
    }

    private fun finish(s: Session, log: List<String>): RoundResult {
        if (s.status != "ONGOING") sessions.remove(s.battleId)
        return RoundResult(s.battleId, s.a.playerId, s.b.playerId,
            s.a.hp.coerceAtLeast(0), s.b.hp.coerceAtLeast(0), log, s.status)
    }

    /** Clean up if a participant disconnects; returns the opponent id to notify (or null). */
    fun abortFor(playerId: Long): Pair<String, Long>? {
        val s = sessions.values.firstOrNull { it.a.playerId == playerId || it.b.playerId == playerId } ?: return null
        sessions.remove(s.battleId)
        val opp = if (s.a.playerId == playerId) s.b.playerId else s.a.playerId
        return s.battleId to opp
    }
}
