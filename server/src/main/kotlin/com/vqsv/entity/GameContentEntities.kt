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
