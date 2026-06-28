package com.vqsv.web

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

// ---------- Auth ----------
data class WebLoginRequest(
    @field:NotBlank val username: String = "",
    @field:NotBlank val password: String = ""
)

data class WebRegisterRequest(
    @field:Size(min = 4, max = 32) val username: String = "",
    @field:Size(min = 6, max = 64) val password: String = "",
    val email: String? = null
)

data class AccountSummary(
    val id: Long,
    val username: String,
    val email: String?,
    val role: String,
    val balanceXu: Long,
    val totalTopup: Long,
    val status: String
)

data class WebAuthResponse(val token: String, val account: AccountSummary)

// ---------- Profile ----------
data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String = "",
    @field:Size(min = 6, max = 64) val newPassword: String = ""
)

data class CharacterSummary(
    val id: Long,
    val serverId: Short,
    val serverName: String,
    val name: String,
    val level: Short,
    val kimTien: Int,
    val huyChuong: Int
)

data class TransactionDto(
    val id: Long,
    val kind: String,          // TOPUP, GIFTCODE, WEBSHOP
    val amountVnd: Int?,
    val xu: Int?,
    val status: String,
    val provider: String?,
    val detail: String?,
    val createdAt: Instant
)

// ---------- Giftcode ----------
data class RedeemRequest(
    @field:NotBlank val code: String = "",
    val serverId: Short? = null,
    val playerId: Long? = null
)

data class RedeemResult(val success: Boolean, val message: String, val reward: String? = null)

// ---------- Top-up ----------
data class TopupPackageDto(
    val id: Short, val name: String, val priceVnd: Int,
    val xuAmount: Int, val bonusXu: Int, val totalXu: Int
)

data class TopupOrderRequest(val packageId: Short = 0, val provider: String = "MANUAL")

data class TopupOrderResponse(
    val transactionId: Long, val amountVnd: Int, val payUrl: String?, val status: String
)

// ---------- Web shop ----------
data class WebShopProductDto(
    val id: Int, val name: String, val description: String?,
    val iconId: Short, val priceXu: Int, val stock: Int
)

data class WebBuyRequest(val productId: Int = 0, val serverId: Short = 1, val playerId: Long = 0)

// ---------- Leaderboard ----------
data class RankRow(
    val rank: Int, val name: String, val level: Short,
    val exp: Int, val serverCode: String
)

// ---------- Public content ----------
data class ServerDto(
    val id: Short, val code: String, val name: String,
    val status: String, val crossGroup: String?, val playerCount: Long
)

data class NewsListItem(
    val id: Long, val title: String, val slug: String, val summary: String?,
    val bannerUrl: String?, val category: String, val publishedAt: Instant?
)

// ---------- Admin ----------
data class AdminDashboard(
    val totalAccounts: Long,
    val totalCharacters: Long,
    val totalRevenueVnd: Long,
    val pendingPayments: Long,
    val serverCount: Long,
    val adminCount: Long
)

data class AdminUpdateAccountRequest(
    val role: String? = null,
    val status: String? = null,
    val isBanned: Boolean? = null,
    val banReason: String? = null,
    val balanceXuDelta: Long? = null   // grant/deduct xu
)

data class AdminResetPasswordRequest(@field:Size(min = 6, max = 64) val newPassword: String = "")

data class MergeServersRequest(val sourceId: Short = 0, val targetId: Short = 0)

data class CrossServerRequest(val crossGroup: String? = null, val serverIds: List<Short> = emptyList())

data class GrantRewardRequest(
    val serverId: Short = 1,
    val playerId: Long = 0,
    val rewardJson: String = "{}"
)

data class ShopListingDto(
    val id: Int = 0,
    val itemId: Short = 0,
    val priceGold: Int? = null,
    val priceMedal: Int? = null,
    val sortOrder: Short = 0
)
