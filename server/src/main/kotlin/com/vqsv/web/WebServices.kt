package com.vqsv.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.vqsv.entity.*
import com.vqsv.repository.*
import com.vqsv.util.JwtUtil
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// ============================================================
// REWARD GRANTING — shared by giftcode + web shop + admin grant
// reward JSON: {"xu":100,"gold":1000,"medal":5,"items":[{"itemId":1,"qty":2}]}
// ============================================================
@Service
class RewardService(
    private val accountRepo: AccountRepository,
    private val playerRepo: PlayerRepository,
    private val itemRepo: ItemRepository,
    private val playerItemRepo: PlayerItemRepository,
    private val mapper: ObjectMapper
) {
    /** Grants a reward. xu -> account wallet; gold/medal/items -> the given player (if any). */
    @Transactional
    fun grant(rewardJson: String, account: Account, player: Player?): String {
        val node = mapper.readTree(if (rewardJson.isBlank()) "{}" else rewardJson)
        val parts = mutableListOf<String>()

        node["xu"]?.asInt()?.takeIf { it != 0 }?.let {
            account.balanceXu += it
            accountRepo.save(account)
            parts.add("$it xu")
        }
        if (player != null) {
            node["gold"]?.asInt()?.takeIf { it != 0 }?.let {
                player.kimTien += it; parts.add("$it kim tiền")
            }
            node["medal"]?.asInt()?.takeIf { it != 0 }?.let {
                player.huyChuong += it; parts.add("$it huy chương")
            }
            playerRepo.save(player)
            node["items"]?.forEach { itemNode ->
                val itemId = itemNode["itemId"].asInt().toShort()
                val qty = (itemNode["qty"]?.asInt() ?: 1)
                addItem(player, itemId, qty)
                parts.add("vật phẩm #$itemId x$qty")
            }
        }
        return if (parts.isEmpty()) "không có phần thưởng" else parts.joinToString(", ")
    }

    private fun addItem(player: Player, itemId: Short, qty: Int) {
        val existing = playerItemRepo.findByPlayerIdAndItemId(player.id, itemId)
        if (existing.isPresent) {
            val pi = existing.get(); pi.quantity += qty; playerItemRepo.save(pi)
        } else {
            val item = itemRepo.findById(itemId).orElseThrow { IllegalArgumentException("Vật phẩm #$itemId không tồn tại") }
            playerItemRepo.save(PlayerItem(player = player, item = item, quantity = qty))
        }
    }
}

// ============================================================
// WEB AUTH — login uses the in-game account credentials
// ============================================================
@Service
class WebAuthService(
    private val accountRepo: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    @Transactional
    fun login(req: WebLoginRequest): WebAuthResponse {
        val account = accountRepo.findByUsername(req.username)
            .orElseThrow { IllegalArgumentException("Tài khoản không tồn tại") }
        if (account.isBanned || account.status == "LOCKED")
            throw IllegalStateException("Tài khoản bị khóa: ${account.banReason ?: ""}")
        if (!passwordEncoder.matches(req.password, account.password))
            throw IllegalArgumentException("Mật khẩu không đúng")
        account.lastLogin = Instant.now()
        accountRepo.save(account)
        val token = jwtUtil.generateWebToken(account.username, account.id, account.role)
        return WebAuthResponse(token, account.summary())
    }

    @Transactional
    fun register(req: WebRegisterRequest): WebAuthResponse {
        if (accountRepo.existsByUsername(req.username))
            throw IllegalArgumentException("Tên tài khoản đã tồn tại")
        if (req.email != null && req.email.isNotBlank() && accountRepo.existsByEmail(req.email))
            throw IllegalArgumentException("Email đã được sử dụng")
        val account = accountRepo.save(Account(
            username = req.username,
            password = passwordEncoder.encode(req.password),
            email = req.email?.ifBlank { null },
            role = "PLAYER"
        ))
        val token = jwtUtil.generateWebToken(account.username, account.id, account.role)
        return WebAuthResponse(token, account.summary())
    }
}

fun Account.summary() = AccountSummary(id, username, email, role, balanceXu, totalTopup, status)

// ============================================================
// PROFILE — change password, characters, transactions
// ============================================================
@Service
class ProfileService(
    private val accountRepo: AccountRepository,
    private val playerRepo: PlayerRepository,
    private val serverRepo: GameServerRepository,
    private val paymentRepo: PaymentTransactionRepository,
    private val redemptionRepo: GiftcodeRedemptionRepository,
    private val orderRepo: WebShopOrderRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun me(accountId: Long): AccountSummary =
        accountRepo.findById(accountId).orElseThrow { IllegalArgumentException("Không tìm thấy tài khoản") }.summary()

    @Transactional
    fun changePassword(accountId: Long, req: ChangePasswordRequest) {
        val account = accountRepo.findById(accountId).orElseThrow { IllegalArgumentException("Không tìm thấy tài khoản") }
        if (!passwordEncoder.matches(req.oldPassword, account.password))
            throw IllegalArgumentException("Mật khẩu cũ không đúng")
        account.password = passwordEncoder.encode(req.newPassword)
        accountRepo.save(account)
    }

    fun characters(accountId: Long): List<CharacterSummary> {
        val servers = serverRepo.findAll().associateBy { it.id }
        return playerRepo.findAllByAccountId(accountId).map {
            CharacterSummary(it.id, it.serverId, servers[it.serverId]?.name ?: "?",
                it.name, it.level, it.kimTien, it.huyChuong)
        }
    }

    fun transactions(accountId: Long): List<TransactionDto> {
        val pays = paymentRepo.findByAccountIdOrderByCreatedAtDesc(accountId).map {
            TransactionDto(it.id, "TOPUP", it.amountVnd, it.xuGranted, it.status, it.provider, it.note, it.createdAt)
        }
        val orders = orderRepo.findByAccountIdOrderByCreatedAtDesc(accountId).map {
            TransactionDto(it.id, "WEBSHOP", null, -it.costXu, it.status, null, "Sản phẩm #${it.productId}", it.createdAt)
        }
        return (pays + orders).sortedByDescending { it.createdAt }
    }
}

// ============================================================
// GIFTCODE REDEEM
// ============================================================
@Service
class GiftcodeService(
    private val giftcodeRepo: GiftcodeRepository,
    private val redemptionRepo: GiftcodeRedemptionRepository,
    private val accountRepo: AccountRepository,
    private val playerRepo: PlayerRepository,
    private val rewardService: RewardService
) {
    @Transactional
    fun redeem(accountId: Long, req: RedeemRequest): RedeemResult {
        val gc = giftcodeRepo.findByCode(req.code.trim().uppercase())
            .orElseThrow { IllegalArgumentException("Giftcode không tồn tại") }
        val now = Instant.now()
        if (gc.status != "ACTIVE") throw IllegalStateException("Giftcode đã bị vô hiệu")
        gc.startsAt?.let { if (now.isBefore(it)) throw IllegalStateException("Giftcode chưa đến thời gian sử dụng") }
        gc.expiresAt?.let { if (now.isAfter(it)) throw IllegalStateException("Giftcode đã hết hạn") }
        if (gc.maxUses > 0 && gc.usedCount >= gc.maxUses)
            throw IllegalStateException("Giftcode đã hết lượt sử dụng")
        if (redemptionRepo.countByGiftcodeIdAndAccountId(gc.id, accountId) >= gc.perAccount)
            throw IllegalStateException("Bạn đã dùng giftcode này rồi")

        val account = accountRepo.findById(accountId).orElseThrow { IllegalArgumentException("Không tìm thấy tài khoản") }
        val player = req.playerId?.let { pid ->
            val p = playerRepo.findById(pid).orElseThrow { IllegalArgumentException("Nhân vật không tồn tại") }
            if (p.account.id != accountId) throw SecurityException("Nhân vật không thuộc tài khoản này")
            gc.serverId?.let { sid -> if (p.serverId != sid) throw IllegalStateException("Giftcode chỉ dùng cho máy chủ khác") }
            p
        }
        val rewardText = rewardService.grant(gc.rewardJson, account, player)
        gc.usedCount += 1
        giftcodeRepo.save(gc)
        redemptionRepo.save(GiftcodeRedemption(giftcodeId = gc.id, accountId = accountId, playerId = player?.id))
        return RedeemResult(true, "Nhận thưởng thành công: $rewardText", rewardText)
    }
}

// ============================================================
// TOP-UP
// ============================================================
@Service
class TopupService(
    private val packageRepo: TopupPackageRepository,
    private val paymentRepo: PaymentTransactionRepository,
    private val accountRepo: AccountRepository,
    private val providers: List<com.vqsv.web.payment.PaymentProvider>
) {
    private val providerMap by lazy { providers.associateBy { it.name.uppercase() } }
    fun providerByName(name: String) = providerMap[name.uppercase()]

    fun packages(): List<TopupPackageDto> =
        packageRepo.findByActiveTrueOrderBySortOrderAsc().map {
            TopupPackageDto(it.id, it.name, it.priceVnd, it.xuAmount, it.bonusXu, it.xuAmount + it.bonusXu)
        }

    @Transactional
    fun createOrder(accountId: Long, req: TopupOrderRequest): TopupOrderResponse {
        val pkg = packageRepo.findById(req.packageId).orElseThrow { IllegalArgumentException("Gói nạp không tồn tại") }
        if (!pkg.active) throw IllegalStateException("Gói nạp đã ngừng bán")
        val tx = paymentRepo.save(PaymentTransaction(
            accountId = accountId,
            packageId = pkg.id,
            amountVnd = pkg.priceVnd,
            xuGranted = pkg.xuAmount + pkg.bonusXu,
            provider = req.provider,
            status = "PENDING",
            note = "Nạp gói ${pkg.name}"
        ))
        // Ask the chosen gateway for a real checkout URL; fall back to the manual
        // transfer page when the provider has no redirect (e.g. MANUAL / not enabled).
        val payUrl = providerByName(req.provider)?.createCheckoutUrl(tx)
            ?: "/nap/thanh-toan?txid=${tx.id}"
        return TopupOrderResponse(tx.id, tx.amountVnd, payUrl, tx.status)
    }

    /**
     * Handle a gateway callback (return URL or IPN). Verifies the signature via the
     * provider and credits xu on success. Idempotent (confirm() ignores a tx that is
     * already SUCCESS), so the browser return and the server IPN can both fire safely.
     */
    @Transactional
    fun handleCallback(providerName: String, params: Map<String, String>): Boolean {
        val provider = providerByName(providerName) ?: return false
        val result = provider.verifyCallback(params)
        if (result.success && result.txId != null) {
            confirm(result.txId, result.providerRef)
            return true
        }
        return false
    }

    /** Marks a transaction SUCCESS and grants xu. Called by the provider webhook or an admin approval. */
    @Transactional
    fun confirm(txId: Long, providerRef: String?): PaymentTransaction {
        val tx = paymentRepo.findById(txId).orElseThrow { IllegalArgumentException("Giao dịch không tồn tại") }
        if (tx.status == "SUCCESS") return tx
        if (tx.status != "PENDING") throw IllegalStateException("Giao dịch không ở trạng thái chờ")
        val account = accountRepo.findById(tx.accountId).orElseThrow { IllegalArgumentException("Tài khoản không tồn tại") }
        account.balanceXu += tx.xuGranted
        account.totalTopup += tx.amountVnd
        accountRepo.save(account)
        tx.status = "SUCCESS"
        tx.providerRef = providerRef
        return paymentRepo.save(tx)
    }
}

// ============================================================
// WEB SHOP — buy with xu, deliver to a character
// ============================================================
@Service
class WebShopService(
    private val productRepo: WebShopProductRepository,
    private val orderRepo: WebShopOrderRepository,
    private val accountRepo: AccountRepository,
    private val playerRepo: PlayerRepository,
    private val rewardService: RewardService
) {
    fun products(): List<WebShopProductDto> =
        productRepo.findByActiveTrueOrderBySortOrderAsc().map {
            WebShopProductDto(it.id, it.name, it.description, it.iconId, it.priceXu, it.stock)
        }

    @Transactional
    fun buy(accountId: Long, req: WebBuyRequest): RedeemResult {
        val product = productRepo.findById(req.productId).orElseThrow { IllegalArgumentException("Sản phẩm không tồn tại") }
        if (!product.active) throw IllegalStateException("Sản phẩm đã ngừng bán")
        if (product.stock == 0) throw IllegalStateException("Sản phẩm đã hết hàng")
        val account = accountRepo.findById(accountId).orElseThrow { IllegalArgumentException("Tài khoản không tồn tại") }
        if (account.balanceXu < product.priceXu)
            throw IllegalStateException("Số dư xu không đủ (cần ${product.priceXu}, có ${account.balanceXu})")
        val player = playerRepo.findById(req.playerId).orElseThrow { IllegalArgumentException("Nhân vật không tồn tại") }
        if (player.account.id != accountId) throw SecurityException("Nhân vật không thuộc tài khoản này")

        account.balanceXu -= product.priceXu
        accountRepo.save(account)
        if (product.stock > 0) { product.stock -= 1; productRepo.save(product) }
        val rewardText = rewardService.grant(product.rewardJson, account, player)
        orderRepo.save(WebShopOrder(
            accountId = accountId, productId = product.id, serverId = player.serverId,
            playerId = player.id, costXu = product.priceXu, status = "DELIVERED"
        ))
        return RedeemResult(true, "Mua thành công: $rewardText", rewardText)
    }
}

// ============================================================
// LEADERBOARD (single server + cross-server)
// ============================================================
@Service
class LeaderboardService(
    private val playerRepo: PlayerRepository,
    private val serverRepo: GameServerRepository
) {
    fun ranking(serverId: Short?, crossGroup: String?, limit: Int): List<RankRow> {
        val servers = serverRepo.findAll().associateBy { it.id }
        val players = when {
            crossGroup != null -> {
                val ids = serverRepo.findByCrossGroup(crossGroup).map { it.id }
                if (ids.isEmpty()) emptyList()
                else playerRepo.leaderboardAcrossServers(ids, Pageable.ofSize(limit))
            }
            serverId != null -> playerRepo.findByServerIdOrderByLevelDescExpDesc(serverId, Pageable.ofSize(limit))
            else -> playerRepo.findByServerIdOrderByLevelDescExpDesc(1, Pageable.ofSize(limit))
        }
        return players.mapIndexed { i, p ->
            RankRow(i + 1, p.name, p.level, p.exp, servers[p.serverId]?.code ?: "?")
        }
    }
}

// ============================================================
// PUBLIC CONTENT — news, events, servers
// ============================================================
@Service
class ContentService(
    private val newsRepo: NewsPostRepository,
    private val eventRepo: EventPostRepository,
    private val serverRepo: GameServerRepository,
    private val playerRepo: PlayerRepository
) {
    fun news(page: Int, size: Int): List<NewsListItem> =
        newsRepo.findByPublishedTrueOrderByPublishedAtDesc(Pageable.ofSize(size).withPage(page)).content.map {
            NewsListItem(it.id, it.title, it.slug, it.summary, it.bannerUrl, it.category, it.publishedAt)
        }

    fun newsBySlug(slug: String): NewsPost =
        newsRepo.findBySlugAndPublishedTrue(slug).orElseThrow { IllegalArgumentException("Bài viết không tồn tại") }

    fun events(): List<EventPost> = eventRepo.findByActiveTrueOrderByStartsAtDesc()

    fun servers(): List<ServerDto> =
        serverRepo.findByStatusNotOrderBySortOrderAsc("HIDDEN").map {
            ServerDto(it.id, it.code, it.name, it.status, it.crossGroup, playerRepo.countByServerId(it.id))
        }
}

// ============================================================
// AUDIT
// ============================================================
@Service
class AuditService(private val auditRepo: AuditLogRepository) {
    fun log(actorId: Long?, actorName: String?, action: String, target: String?, detail: String?, ip: String? = null) {
        auditRepo.save(AuditLog(
            actorId = actorId, actorName = actorName, action = action,
            target = target, detail = detail, ip = ip
        ))
    }
}
