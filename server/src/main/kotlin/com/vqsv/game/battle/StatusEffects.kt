package com.vqsv.game.battle

import kotlin.random.Random

/**
 * In-battle status effects, ported from the original skill model.
 *
 * Skills carry `behavior_flag` + `effect_id`:
 *   flag 0 = direct damage.
 *   flag 2 = damage + inflict a status on the target. effect_id:
 *       0 = Đốt Cháy (BURN, damage over 3 turns)
 *       1 = Mê Muội (CONFUSE, may lose the turn for 2 turns)
 *       2 = Quấn Quanh (BIND, lose the turn for 3 turns)
 *       3 = Thực Loại (SEED, delayed nuke)            <- still pending
 *   flag 1 = self buff (target_code = 1). effect_id:
 *       0 = tăng phòng ngự (DEF_UP)
 *       1 = tăng công (ATK_UP)
 *       2 = phòng ngự + phản đòn (treated as DEF_UP; reflect not modelled)
 *       3 = hồi máu mỗi hiệp (REGEN)
 *
 * Durations come straight from the descriptions (burn/bind/buff = 3 turns,
 * confuse = 2). The per-turn HP amounts and buff/confuse percentages live only
 * in the Vietnamese descriptions, not the decoded numeric columns, so they are
 * approximated here from ATK / max-HP and documented as constants.
 */
enum class StatusType { BURN, REGEN, DEF_UP, ATK_UP, CONFUSE, BIND }

data class StatusEffect(val type: StatusType, var turns: Int, val magnitude: Int = 0, val pct: Int = 0)

object StatusEffects {
    const val FLAG_DAMAGE = 0
    const val FLAG_SELF_BUFF = 1
    const val FLAG_ENEMY_DEBUFF = 2

    // effect_id inside the enemy-debuff family (flag = 2)
    const val DEBUFF_BURN = 0
    const val DEBUFF_CONFUSE = 1
    const val DEBUFF_BIND = 2

    // effect_id inside the self-buff family (flag = 1)
    const val BUFF_DEF = 0
    const val BUFF_ATK = 1
    const val BUFF_REFLECT = 2
    const val BUFF_REGEN = 3

    const val BURN_TURNS = 3
    const val CONFUSE_TURNS = 2
    const val BIND_TURNS = 3
    const val BUFF_TURNS = 3
    const val DEF_UP_PCT = 30
    const val ATK_UP_PCT = 50
    const val CONFUSE_SKIP_PCT = 50   // chance to lose the turn while confused

    /** The status a behavior_flag=2 skill inflicts on its target, or null. */
    fun debuffFor(effectId: Int?, attackerAtk: Int): StatusEffect? = when (effectId) {
        DEBUFF_BURN -> StatusEffect(StatusType.BURN, BURN_TURNS, magnitude = maxOf(1, attackerAtk / 8))
        DEBUFF_CONFUSE -> StatusEffect(StatusType.CONFUSE, CONFUSE_TURNS)
        DEBUFF_BIND -> StatusEffect(StatusType.BIND, BIND_TURNS)
        else -> null
    }

    /** The status a behavior_flag=1 skill applies to its caster, or null. */
    fun selfBuffFor(effectId: Int?, casterHpMax: Int): StatusEffect? = when (effectId) {
        BUFF_DEF, BUFF_REFLECT -> StatusEffect(StatusType.DEF_UP, BUFF_TURNS, pct = DEF_UP_PCT)
        BUFF_ATK -> StatusEffect(StatusType.ATK_UP, BUFF_TURNS, pct = ATK_UP_PCT)
        BUFF_REGEN -> StatusEffect(StatusType.REGEN, BUFF_TURNS, magnitude = maxOf(1, casterHpMax / 10))
        else -> null
    }

    /** A short Vietnamese label for the moment a status lands (battle log). */
    fun landedLabel(type: StatusType): String = when (type) {
        StatusType.BURN -> "bị Đốt Cháy"
        StatusType.CONFUSE -> "bị Mê Muội"
        StatusType.BIND -> "bị Quấn Quanh"
        StatusType.DEF_UP -> "tăng Phòng Ngự"
        StatusType.ATK_UP -> "tăng Tấn Công"
        StatusType.REGEN -> "được Hồi Phục"
    }

    /** Apply (or refresh — no stacking) an effect on a status list. */
    fun apply(statuses: MutableList<StatusEffect>, eff: StatusEffect) {
        statuses.removeAll { it.type == eff.type }
        statuses.add(eff)
    }

    /** Backwards-compatible helper: does this skill inflict BURN? */
    fun isBurnSkill(behaviorFlag: Int, effectId: Int?): Boolean =
        behaviorFlag == FLAG_ENEMY_DEBUFF && effectId == DEBUFF_BURN

    fun applyBurn(statuses: MutableList<StatusEffect>, attackerAtk: Int) =
        apply(statuses, StatusEffect(StatusType.BURN, BURN_TURNS, magnitude = maxOf(1, attackerAtk / 8)))

    fun effectiveAtk(base: Int, statuses: List<StatusEffect>): Int {
        val pct = statuses.filter { it.type == StatusType.ATK_UP }.sumOf { it.pct }
        return base + base * pct / 100
    }

    fun effectiveDef(base: Int, statuses: List<StatusEffect>): Int {
        val pct = statuses.filter { it.type == StatusType.DEF_UP }.sumOf { it.pct }
        return base + base * pct / 100
    }

    /** Prevented from acting this turn? BIND always; CONFUSE by chance. */
    fun isDisabled(statuses: List<StatusEffect>): Boolean {
        if (statuses.any { it.type == StatusType.BIND }) return true
        if (statuses.any { it.type == StatusType.CONFUSE } && Random.nextInt(100) < CONFUSE_SKIP_PCT) return true
        return false
    }

    fun disableLabel(statuses: List<StatusEffect>): String =
        if (statuses.any { it.type == StatusType.BIND }) "bị Quấn Quanh" else "bị Mê Muội"

    /**
     * Advance every effect one turn: returns the net HP change (heal positive,
     * damage negative), logs each DoT/HoT, and drops expired effects.
     */
    fun tick(statuses: MutableList<StatusEffect>, name: String, log: MutableList<String>): Int {
        if (statuses.isEmpty()) return 0
        var delta = 0
        val it = statuses.iterator()
        while (it.hasNext()) {
            val e = it.next()
            when (e.type) {
                StatusType.BURN -> { delta -= e.magnitude; log.add("$name bị thiêu đốt: ${e.magnitude} sát thương!") }
                StatusType.REGEN -> { delta += e.magnitude; log.add("$name hồi phục ${e.magnitude} HP!") }
                else -> {}
            }
            e.turns -= 1
            if (e.turns <= 0) it.remove()
        }
        return delta
    }
}
