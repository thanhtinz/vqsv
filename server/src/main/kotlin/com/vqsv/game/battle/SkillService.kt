package com.vqsv.game.battle

import com.vqsv.entity.Skill
import com.vqsv.repository.SkillRepository
import org.springframework.stereotype.Service

/**
 * Faithful port of the original skill model:
 *  - A pet draws skills from its element block (skill_elem*10 .. +9, 10 per element).
 *  - It "learns" up to min(5, level/10 + 1) of them, gated by each skill's required_level.
 *  - In battle each skill costs SP; the SP pool refills every battle (the original
 *    restores HP/SP on victory). Damage scales by the skill's power% (0 = a basic hit).
 *
 * Status effects (burn / confuse / buffs from behavior_flag + effect_id) are not yet
 * modelled — skills currently resolve their damage only.
 */
@Service
class SkillService(private val skillRepo: SkillRepository) {

    /** Per-battle SP pool for a pet of the given level. */
    fun spMax(level: Int): Int = 40 + level

    /**
     * The skills a pet has learned: its element block, required_level met, that actually
     * do something (power or an effect), capped by level, in id order.
     */
    fun learnedSkills(skillElem: Short, level: Int): List<Skill> =
        skillRepo.findByElement(skillElem)
            .filter { it.requiredLevel <= level && (it.power > 0 || it.behaviorFlag > 0) }
            .sortedBy { it.id }
            .take(minOf(5, level / 10 + 1))

    /** The learned skill with this id, or null if the pet hasn't learned it. */
    fun usableSkill(skillElem: Short, level: Int, skillId: Short): Skill? =
        learnedSkills(skillElem, level).firstOrNull { it.id == skillId }
}
