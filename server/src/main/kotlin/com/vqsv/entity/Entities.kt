package com.vqsv.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

// ============================================================
// ACCOUNT
// ============================================================
@Entity
@Table(name = "accounts")
data class Account(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 32)
    val username: String = "",

    @Column(nullable = false, length = 128)
    var password: String = "",

    @Column(unique = true, length = 128)
    var email: String? = null,

    @Column(length = 20)
    var phone: String? = null,

    @Column(nullable = false, length = 16)
    var role: String = "PLAYER",

    @Column(nullable = false)
    var isBanned: Boolean = false,

    @Column(columnDefinition = "TEXT")
    var banReason: String? = null,

    @Column(name = "balance_xu", nullable = false)
    var balanceXu: Long = 0,

    @Column(name = "total_topup", nullable = false)
    var totalTopup: Long = 0,

    @Column(nullable = false, length = 16)
    var status: String = "ACTIVE",

    @CreationTimestamp
    val createdAt: Instant = Instant.now(),

    var lastLogin: Instant? = null
)

// ============================================================
// PLAYER
// ============================================================
@Entity
@Table(
    name = "players",
    uniqueConstraints = [UniqueConstraint(name = "players_server_name_uq", columnNames = ["server_id", "name"])]
)
data class Player(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account = Account(),

    @Column(name = "server_id", nullable = false)
    var serverId: Short = 1,

    @Column(nullable = false, length = 32)
    var name: String = "",

    @Column(nullable = false)
    var level: Short = 1,

    @Column(nullable = false)
    var exp: Int = 0,

    @Column(name = "kim_tien", nullable = false)
    var kimTien: Int = 0,

    @Column(name = "huy_chuong", nullable = false)
    var huyChuong: Int = 0,

    @Column(name = "map_id", nullable = false)
    var mapId: Short = 1,

    @Column(name = "pos_x", nullable = false)
    var posX: Short = 5,

    @Column(name = "pos_y", nullable = false)
    var posY: Short = 5,

    @Column(nullable = false)
    var hp: Int = 100,

    @Column(name = "hp_max", nullable = false)
    var hpMax: Int = 100,

    @Column(name = "is_online", nullable = false)
    var isOnline: Boolean = false,

    @CreationTimestamp
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)

// ============================================================
// PET TEMPLATE
// ============================================================
@Entity
@Table(name = "pet_templates")
data class PetTemplate(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 32)
    val name: String = "",

    @Column(name = "sprite_id", nullable = false)
    val spriteId: Short = 0,

    @Column(nullable = false, length = 16)
    val element: String = "FIRE",

    @Column(name = "base_hp", nullable = false)
    val baseHp: Short = 50,

    @Column(name = "base_atk", nullable = false)
    val baseAtk: Short = 10,

    @Column(name = "base_def", nullable = false)
    val baseDef: Short = 5,

    @Column(name = "base_spd", nullable = false)
    val baseSpd: Short = 5,

    @Column(name = "growth_hp", nullable = false)
    val growthHp: Double = 1.2,

    @Column(name = "growth_atk", nullable = false)
    val growthAtk: Double = 1.15,

    @Column(name = "growth_def", nullable = false)
    val growthDef: Double = 1.1,

    @Column(name = "growth_spd", nullable = false)
    val growthSpd: Double = 1.05,

    @Column(name = "catch_rate", nullable = false)
    val catchRate: Short = 30,

    @Column(name = "evolve_into")
    val evolveInto: Short? = null,

    @Column(name = "evolve_lv")
    val evolveLv: Short? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)

// ============================================================
// PLAYER PET
// ============================================================
@Entity
@Table(name = "player_pets")
data class PlayerPet(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player = Player(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    val template: PetTemplate = PetTemplate(),

    @Column(length = 32)
    var nickname: String? = null,

    @Column(nullable = false)
    var level: Short = 1,

    @Column(nullable = false)
    var exp: Int = 0,

    @Column(nullable = false)
    var hp: Int = 50,

    @Column(name = "hp_max", nullable = false)
    var hpMax: Int = 50,

    @Column(nullable = false)
    var atk: Short = 10,

    @Column(nullable = false)
    var def: Short = 5,

    @Column(nullable = false)
    var spd: Short = 5,

    @Column(nullable = false)
    var slot: Short = 0,

    @Column(nullable = false)
    var loyalty: Short = 50,

    @CreationTimestamp
    val obtainedAt: Instant = Instant.now()
)

// ============================================================
// MAP
// ============================================================
@Entity
@Table(name = "maps")
data class GameMap(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 64)
    val name: String = "",

    @Column(nullable = false)
    val width: Short = 20,

    @Column(nullable = false)
    val height: Short = 20,

    @Column(name = "tileset_id", nullable = false)
    val tilesetId: Short = 0,

    @Column(name = "bgm_id", nullable = false)
    val bgmId: Short = 0,

    @Column(name = "is_pvp", nullable = false)
    val isPvp: Boolean = false,

    @Column(name = "min_level", nullable = false)
    val minLevel: Short = 1
)

// ============================================================
// ITEM
// ============================================================
@Entity
@Table(name = "items")
data class Item(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 64)
    val name: String = "",

    @Column(name = "item_type", nullable = false, length = 16)
    val itemType: String = "",

    @Column(name = "effect_val", nullable = false)
    val effectVal: Int = 0,

    @Column(name = "icon_id", nullable = false)
    val iconId: Short = 0,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)

// ============================================================
// PLAYER ITEM
// ============================================================
@Entity
@Table(name = "player_items")
@IdClass(PlayerItemId::class)
data class PlayerItem(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    val player: Player = Player(),

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    val item: Item = Item(),

    @Column(nullable = false)
    var quantity: Int = 1
)

data class PlayerItemId(
    val player: Long = 0,
    val item: Short = 0
) : java.io.Serializable

// ============================================================
// BADGE
// ============================================================
@Entity
@Table(name = "badges")
data class Badge(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 64)
    val name: String = "",

    @Column(name = "icon_id", nullable = false)
    val iconId: Short = 0,

    @Column(nullable = false, length = 128)
    val condition: String = "",

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)

@Entity
@Table(name = "player_badges")
@IdClass(PlayerBadgeId::class)
data class PlayerBadge(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    val player: Player = Player(),

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id")
    val badge: Badge = Badge(),

    @CreationTimestamp
    val earnedAt: Instant = Instant.now()
)

data class PlayerBadgeId(
    val player: Long = 0,
    val badge: Short = 0
) : java.io.Serializable

// ============================================================
// BATTLE LOG
// ============================================================
@Entity
@Table(name = "battle_logs")
data class BattleLog(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "attacker_id", nullable = false)
    val attackerId: Long = 0,

    @Column(name = "defender_id")
    val defenderId: Long? = null,

    @Column(name = "winner_id")
    var winnerId: Long? = null,

    @Column(name = "battle_type", nullable = false, length = 16)
    val battleType: String = "PVE",

    @Column(nullable = false)
    var turns: Short = 0,

    @Column(name = "exp_gained", nullable = false)
    var expGained: Int = 0,

    @Column(name = "gold_gained", nullable = false)
    var goldGained: Int = 0,

    @CreationTimestamp
    val createdAt: Instant = Instant.now()
)

// ============================================================
// MAP WILD PET
// ============================================================
@Entity
@Table(name = "map_wild_pets")
data class MapWildPet(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "map_id", nullable = false)
    val mapId: Short = 0,

    @Column(name = "template_id", nullable = false)
    val templateId: Short = 0,

    @Column(name = "min_level", nullable = false)
    val minLevel: Short = 1,

    @Column(name = "max_level", nullable = false)
    val maxLevel: Short = 5,

    @Column(name = "spawn_rate", nullable = false)
    val spawnRate: Short = 10
)

// ============================================================
// SHOP LISTING
// ============================================================
@Entity
@Table(name = "shop_listings")
data class ShopListing(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: Item = Item(),

    @Column(name = "price_gold")
    val priceGold: Int? = null,

    @Column(name = "price_medal")
    val priceMedal: Int? = null,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Short = 0
)
