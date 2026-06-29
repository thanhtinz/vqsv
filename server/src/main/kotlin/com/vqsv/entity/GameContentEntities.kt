package com.vqsv.entity

import jakarta.persistence.*

// NPC (npcs table from V1) — dialog / shop / trainer NPCs placed on maps.
@Entity
@Table(name = "npcs")
data class Npc(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 32)
    var name: String = "",

    @Column(name = "sprite_id", nullable = false)
    var spriteId: Short = 0,

    @Column(name = "npc_type", nullable = false, length = 16)
    var npcType: String = "DIALOG",   // DIALOG, SHOP, BATTLE_TRAINER

    @Column(name = "map_id", nullable = false)
    var mapId: Short = 1,

    @Column(name = "pos_x", nullable = false)
    var posX: Short = 0,

    @Column(name = "pos_y", nullable = false)
    var posY: Short = 0,

    @Column(name = "dialog_key", length = 64)
    var dialogKey: String? = null,

    // Free-text lines shown when a player talks to this NPC (lines split by '\n').
    @Column(name = "dialog", columnDefinition = "TEXT")
    var dialog: String? = null,

    // For BATTLE_TRAINER NPCs: the enemy template this trainer fights with.
    @Column(name = "enemy_template_id")
    var enemyTemplateId: Short? = null
)

// Enemy template (npc_enemy_templates from V1) — boss/trainer enemy stats.
@Entity
@Table(name = "npc_enemy_templates")
data class NpcEnemyTemplate(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 32)
    var name: String = "",

    @Column(name = "sprite_id", nullable = false)
    var spriteId: Short = 0,

    @Column(nullable = false)
    var level: Short = 1,

    @Column(nullable = false)
    var hp: Int = 50,

    @Column(nullable = false)
    var atk: Short = 8,

    @Column(nullable = false)
    var def: Short = 3,

    @Column(nullable = false)
    var spd: Short = 5,

    @Column(name = "exp_reward", nullable = false)
    var expReward: Int = 20,

    @Column(name = "gold_reward", nullable = false)
    var goldReward: Int = 10,

    @Column(nullable = false, length = 16)
    var element: String = "FIRE",

    @Column(name = "map_id")
    var mapId: Short? = null
)

// Map warp (map_warps from V1) — teleport links between map tiles.
@Entity
@Table(name = "map_warps")
data class MapWarp(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "from_map", nullable = false)
    var fromMap: Short = 1,

    @Column(name = "from_x", nullable = false)
    var fromX: Short = 0,

    @Column(name = "from_y", nullable = false)
    var fromY: Short = 0,

    @Column(name = "to_map", nullable = false)
    var toMap: Short = 1,

    @Column(name = "to_x", nullable = false)
    var toX: Short = 0,

    @Column(name = "to_y", nullable = false)
    var toY: Short = 0
)

// Trainer party: a BATTLE_TRAINER npc's ordered team of enemy templates.
@Entity
@Table(name = "trainer_party")
data class TrainerPartyMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "npc_id", nullable = false)
    var npcId: Short = 0,

    @Column(name = "enemy_template_id", nullable = false)
    var enemyTemplateId: Short = 0,

    @Column(nullable = false)
    var slot: Short = 0
)

// Quest given by an NPC. Faithful "săn quái / thu thập / đạt cấp" objectives.
@Entity
@Table(name = "quests")
data class Quest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 64)
    var name: String = "",

    // Which NPC hands this quest out.
    @Column(name = "giver_npc_id", nullable = false)
    var giverNpcId: Short = 0,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    // KILL_MOB (target = pet template id) | COLLECT_ITEM (target = item id) | REACH_LEVEL (target = level)
    @Column(name = "objective_type", nullable = false, length = 16)
    var objectiveType: String = "KILL_MOB",

    @Column(name = "objective_target", nullable = false)
    var objectiveTarget: Short = 0,

    @Column(name = "objective_count", nullable = false)
    var objectiveCount: Int = 1,

    @Column(name = "reward_gold", nullable = false)
    var rewardGold: Int = 0,

    @Column(name = "reward_exp", nullable = false)
    var rewardExp: Int = 0,

    @Column(name = "reward_item_id")
    var rewardItemId: Short? = null,

    @Column(name = "required_level", nullable = false)
    var requiredLevel: Short = 1,

    // Must have CLAIMED this quest first (null = no prerequisite).
    @Column(name = "prerequisite_quest_id")
    var prerequisiteQuestId: Short? = null
)

// A player's progress on a quest. Status: IN_PROGRESS | COMPLETED | CLAIMED.
@Entity
@Table(name = "player_quests")
@IdClass(PlayerQuestId::class)
data class PlayerQuest(
    @Id
    @Column(name = "player_id")
    val playerId: Long = 0,

    @Id
    @Column(name = "quest_id")
    val questId: Short = 0,

    @Column(nullable = false)
    var progress: Int = 0,

    @Column(nullable = false, length = 16)
    var status: String = "IN_PROGRESS"
)

data class PlayerQuestId(
    val playerId: Long = 0,
    val questId: Short = 0
) : java.io.Serializable
