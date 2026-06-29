package com.vqsv.game.battle

/**
 * In-battle status effects, ported from the original skill model.
 *
 * Skills carry `behavior_flag` + `effect_id`. The decoded enemy-debuff family
 * (behavior_flag = 2) maps effect_id to a status:
 *   0 = Đốt Cháy (BURN, damage over 3 turns)   <- implemented
 *   1 = Mê Muội (CONFUSE)                       <- pending
 *   2 = Quấn Quanh (BIND)                       <- pending
 *   3 = Thực Loại (SEED, delayed damage)        <- pending
 *
 * BURN is deterministic in the original ("mỗi hiệp sẽ giảm bớt trị số sinh mạng,
 * duy trì liên tục 3 hiệp"). The exact per-turn amount isn't in the decoded
 * numeric columns, so it is approximated from the attacker's ATK and documented
 * here; everything else (which skills burn, how long) comes straight from the data.
 */
enum class StatusType { BURN }

data class StatusEffect(val type: StatusType, var turns: Int, val magnitude: Int)

object StatusEffects {
    const val FLAG_ENEMY_DEBUFF = 2   // behavior_flag: inflicts a status on the target
    const val EFFECT_BURN = 0         // effect_id within the enemy-debuff family
    const val BURN_TURNS = 3

    /** Does this skill inflict BURN? (behavior_flag = 2, effect_id = 0). */
    fun isBurnSkill(behaviorFlag: Int, effectId: Int?): Boolean =
        behaviorFlag == FLAG_ENEMY_DEBUFF && effectId == EFFECT_BURN

    /** A BURN scaled off the attacker's power (approximated, see file header). */
    fun burnFrom(attackerAtk: Int): StatusEffect =
        StatusEffect(StatusType.BURN, BURN_TURNS, maxOf(1, attackerAtk / 8))

    /** Apply (or refresh) a BURN on a target's status list. */
    fun applyBurn(statuses: MutableList<StatusEffect>, attackerAtk: Int) {
        statuses.removeAll { it.type == StatusType.BURN }
        statuses.add(burnFrom(attackerAtk))
    }

    /**
     * Advance all effects one turn for a target: returns the total DoT damage,
     * appends a log line per effect, and drops effects whose duration ran out.
     */
    fun tick(statuses: MutableList<StatusEffect>, targetName: String, log: MutableList<String>): Int {
        if (statuses.isEmpty()) return 0
        var total = 0
        val it = statuses.iterator()
        while (it.hasNext()) {
            val e = it.next()
            when (e.type) {
                StatusType.BURN -> {
                    total += e.magnitude
                    log.add("$targetName bị thiêu đốt: ${e.magnitude} sát thương!")
                }
            }
            e.turns -= 1
            if (e.turns <= 0) it.remove()
        }
        return total
    }
}
