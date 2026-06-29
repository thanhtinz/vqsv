package com.vqsv.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

// ============================================================
// GAME SERVER (multi-server / cross-server / merge)
// ============================================================
@Entity
@Table(name = "game_servers")
data class GameServer(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, unique = true, length = 16)
    var code: String = "",

    @Column(nullable = false, length = 64)
    var name: String = "",

    @Column(length = 128)
    var host: String? = null,

    @Column(name = "tcp_port", nullable = false)
    var tcpPort: Int = 9090,

    @Column(nullable = false, length = 16)
    var status: String = "OPEN",          // OPEN, MAINTENANCE, FULL, HIDDEN, MERGED

    @Column(name = "cross_group", length = 32)
    var crossGroup: String? = null,       // lien-server group key

    @Column(name = "merged_into")
    var mergedInto: Short? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Short = 0,

    @Column(name = "open_at", nullable = false)
    var openAt: Instant = Instant.now(),

    @CreationTimestamp
    val createdAt: Instant = Instant.now()
)

// ============================================================
// GIFTCODE
// ============================================================
@Entity
@Table(name = "giftcodes")
data class Giftcode(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 32)
    var code: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "reward_json", nullable = false, columnDefinition = "TEXT")
    var rewardJson: String = "{}",

    @Column(name = "max_uses", nullable = false)
    var maxUses: Int = 0,

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0,

    @Column(name = "per_account", nullable = false)
    var perAccount: Short = 1,

    @Column(name = "server_id")
    var serverId: Short? = null,

    @Column(name = "starts_at")
    var startsAt: Instant? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(nullable = false, length = 16)
    var status: String = "ACTIVE",

    @CreationTimestamp
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "giftcode_redemptions")
data class GiftcodeRedemption(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "giftcode_id", nullable = false)
    val giftcodeId: Long = 0,

    @Column(name = "account_id", nullable = false)
    val accountId: Long = 0,

    @Column(name = "player_id")
    val playerId: Long? = null,

    @CreationTimestamp
    val redeemedAt: Instant = Instant.now()
)

// ============================================================
// TOP-UP PACKAGE + PAYMENT
// ============================================================
@Entity
@Table(name = "topup_packages")
data class TopupPackage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, length = 64)
    var name: String = "",

    @Column(name = "price_vnd", nullable = false)
    var priceVnd: Int = 0,

    @Column(name = "xu_amount", nullable = false)
    var xuAmount: Int = 0,

    @Column(name = "bonus_xu", nullable = false)
    var bonusXu: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Short = 0
)

@Entity
@Table(name = "payment_transactions")
data class PaymentTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "account_id", nullable = false)
    val accountId: Long = 0,

    @Column(name = "package_id")
    val packageId: Short? = null,

    @Column(name = "amount_vnd", nullable = false)
    var amountVnd: Int = 0,

    @Column(name = "xu_granted", nullable = false)
    var xuGranted: Int = 0,

    @Column(nullable = false, length = 24)
    var provider: String = "MANUAL",

    @Column(name = "provider_ref", length = 128)
    var providerRef: String? = null,

    @Column(nullable = false, length = 16)
    var status: String = "PENDING",

    @Column(columnDefinition = "TEXT")
    var note: String? = null,

    @CreationTimestamp
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)

// ============================================================
// WEB SHOP
// ============================================================
@Entity
@Table(name = "web_shop_products")
data class WebShopProduct(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, length = 64)
    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "icon_id", nullable = false)
    var iconId: Short = 0,

    @Column(name = "price_xu", nullable = false)
    var priceXu: Int = 0,

    @Column(name = "reward_json", nullable = false, columnDefinition = "TEXT")
    var rewardJson: String = "{}",

    @Column(nullable = false)
    var stock: Int = -1,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Short = 0
)

@Entity
@Table(name = "web_shop_orders")
data class WebShopOrder(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "account_id", nullable = false)
    val accountId: Long = 0,

    @Column(name = "product_id", nullable = false)
    val productId: Int = 0,

    @Column(name = "server_id", nullable = false)
    val serverId: Short = 1,

    @Column(name = "player_id")
    val playerId: Long? = null,

    @Column(name = "cost_xu", nullable = false)
    val costXu: Int = 0,

    @Column(nullable = false, length = 16)
    var status: String = "DELIVERED",

    @CreationTimestamp
    val createdAt: Instant = Instant.now()
)

// ============================================================
// NEWS & EVENTS
// ============================================================
@Entity
@Table(name = "news_posts")
data class NewsPost(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(nullable = false, unique = true, length = 220)
    var slug: String = "",

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var body: String = "",

    @Column(name = "banner_url", length = 255)
    var bannerUrl: String? = null,

    @Column(nullable = false, length = 24)
    var category: String = "NEWS",

    @Column(nullable = false)
    var published: Boolean = false,

    @Column(length = 64)
    var author: String? = null,

    @CreationTimestamp
    val createdAt: Instant = Instant.now(),

    @Column(name = "published_at")
    var publishedAt: Instant? = null
)

@Entity
@Table(name = "events")
data class EventPost(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var body: String = "",

    @Column(name = "banner_url", length = 255)
    var bannerUrl: String? = null,

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant = Instant.now(),

    @Column(name = "ends_at")
    var endsAt: Instant? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @CreationTimestamp
    val createdAt: Instant = Instant.now()
)

// ============================================================
// AUDIT LOG
// ============================================================
@Entity
@Table(name = "audit_logs")
data class AuditLog(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "actor_id")
    val actorId: Long? = null,

    @Column(name = "actor_name", length = 64)
    val actorName: String? = null,

    @Column(nullable = false, length = 64)
    val action: String = "",

    @Column(length = 128)
    val target: String? = null,

    @Column(columnDefinition = "TEXT")
    val detail: String? = null,

    @Column(length = 45)
    val ip: String? = null,

    @CreationTimestamp
    val createdAt: Instant = Instant.now()
)

// ============================================================
// PAYMENT SETTINGS (single row id=1; edited from the admin panel)
// ============================================================
@Entity
@Table(name = "payment_settings")
data class PaymentSettings(
    @Id
    val id: Short = 1,

    @Column(nullable = false)
    var enabled: Boolean = false,

    @Column(name = "sepay_api_key", nullable = false, length = 128)
    var sepayApiKey: String = "",

    @Column(name = "bank_account", nullable = false, length = 32)
    var bankAccount: String = "",

    @Column(name = "bank_code", nullable = false, length = 32)
    var bankCode: String = "",

    @Column(name = "account_holder", nullable = false, length = 64)
    var accountHolder: String = "",

    @Column(nullable = false, length = 16)
    var prefix: String = "VQSV",

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
