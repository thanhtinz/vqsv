package com.vqsv.web

import com.vqsv.entity.Account
import com.vqsv.entity.GameServer
import com.vqsv.repository.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val accountRepo: AccountRepository,
    private val playerRepo: PlayerRepository,
    private val paymentRepo: PaymentTransactionRepository,
    private val serverRepo: GameServerRepository,
    private val passwordEncoder: PasswordEncoder,
    private val topupService: TopupService,
    private val rewardService: RewardService,
    private val auditService: AuditService
) {
    fun dashboard(): AdminDashboard = AdminDashboard(
        totalAccounts = accountRepo.count(),
        totalCharacters = playerRepo.count(),
        totalRevenueVnd = paymentRepo.totalRevenue(),
        pendingPayments = paymentRepo.countByStatus("PENDING"),
        serverCount = serverRepo.count(),
        adminCount = accountRepo.countByRole("ADMIN")
    )

    // ---------- Users ----------
    @Transactional
    fun updateAccount(actorId: Long, accountId: Long, req: AdminUpdateAccountRequest): AccountSummary {
        val acc = accountRepo.findById(accountId).orElseThrow { IllegalArgumentException("Không tìm thấy tài khoản") }
        req.role?.let { acc.role = it }
        req.status?.let { acc.status = it }
        req.isBanned?.let { acc.isBanned = it }
        req.banReason?.let { acc.banReason = it }
        req.balanceXuDelta?.let { acc.balanceXu = (acc.balanceXu + it).coerceAtLeast(0) }
        accountRepo.save(acc)
        auditService.log(actorId, null, "UPDATE_ACCOUNT", "account:$accountId", req.toString())
        return acc.summary()
    }

    @Transactional
    fun resetPassword(actorId: Long, accountId: Long, newPassword: String) {
        val acc = accountRepo.findById(accountId).orElseThrow { IllegalArgumentException("Không tìm thấy tài khoản") }
        acc.password = passwordEncoder.encode(newPassword)
        accountRepo.save(acc)
        auditService.log(actorId, null, "RESET_PASSWORD", "account:$accountId", null)
    }

    // ---------- Payments ----------
    @Transactional
    fun approvePayment(actorId: Long, txId: Long) {
        topupService.confirm(txId, "ADMIN_APPROVED")
        auditService.log(actorId, null, "APPROVE_PAYMENT", "tx:$txId", null)
    }

    @Transactional
    fun rejectPayment(actorId: Long, txId: Long) {
        val tx = paymentRepo.findById(txId).orElseThrow { IllegalArgumentException("Giao dịch không tồn tại") }
        if (tx.status == "PENDING") { tx.status = "FAILED"; paymentRepo.save(tx) }
        auditService.log(actorId, null, "REJECT_PAYMENT", "tx:$txId", null)
    }

    // ---------- Grant reward directly to a character ----------
    @Transactional
    fun grant(actorId: Long, req: GrantRewardRequest): RedeemResult {
        val player = playerRepo.findById(req.playerId).orElseThrow { IllegalArgumentException("Nhân vật không tồn tại") }
        val account = accountRepo.findById(player.account.id).orElseThrow { IllegalArgumentException("Tài khoản không tồn tại") }
        val text = rewardService.grant(req.rewardJson, account, player)
        auditService.log(actorId, null, "GRANT_REWARD", "player:${req.playerId}", req.rewardJson)
        return RedeemResult(true, "Đã tặng: $text", text)
    }

    // ---------- Servers: create / merge / cross-server ----------
    @Transactional
    fun createServer(actorId: Long, server: GameServer): GameServer {
        if (serverRepo.existsByCode(server.code)) throw IllegalArgumentException("Mã máy chủ đã tồn tại")
        val saved = serverRepo.save(server)
        auditService.log(actorId, null, "CREATE_SERVER", "server:${saved.id}", server.code)
        return saved
    }

    /** Merge: move all characters from source server into target, then mark source MERGED. */
    @Transactional
    fun mergeServers(actorId: Long, req: MergeServersRequest): Map<String, Any> {
        if (req.sourceId == req.targetId) throw IllegalArgumentException("Máy chủ nguồn và đích phải khác nhau")
        val source = serverRepo.findById(req.sourceId).orElseThrow { IllegalArgumentException("Máy chủ nguồn không tồn tại") }
        val target = serverRepo.findById(req.targetId).orElseThrow { IllegalArgumentException("Máy chủ đích không tồn tại") }
        // Resolve duplicate character names across the two servers before moving.
        val targetNames = playerRepo.findByServerIdOrderByLevelDescExpDesc(target.id, org.springframework.data.domain.Pageable.unpaged())
            .map { it.name }.toMutableSet()
        var renamed = 0
        playerRepo.findByServerIdOrderByLevelDescExpDesc(source.id, org.springframework.data.domain.Pageable.unpaged()).forEach { p ->
            if (!targetNames.add(p.name)) {
                var suffix = 1
                var newName = "${p.name}_${source.code}"
                while (!targetNames.add(newName)) { suffix++; newName = "${p.name}_${source.code}$suffix" }
                p.name = newName.take(32)
                playerRepo.save(p)
                renamed++
            }
        }
        val moved = playerRepo.reassignServer(source.id, target.id)
        source.status = "MERGED"
        source.mergedInto = target.id
        serverRepo.save(source)
        auditService.log(actorId, null, "MERGE_SERVERS", "server:${source.id}->${target.id}",
            "moved=$moved renamed=$renamed")
        return mapOf("moved" to moved, "renamed" to renamed, "into" to target.code)
    }

    /** Cross-server (lien-server): assign a set of servers to the same crossGroup key. */
    @Transactional
    fun setCrossGroup(actorId: Long, req: CrossServerRequest): List<GameServer> {
        val updated = req.serverIds.map { id ->
            val s = serverRepo.findById(id).orElseThrow { IllegalArgumentException("Máy chủ #$id không tồn tại") }
            s.crossGroup = req.crossGroup
            serverRepo.save(s)
        }
        auditService.log(actorId, null, "SET_CROSS_GROUP", "group:${req.crossGroup}", req.serverIds.toString())
        return updated
    }

    fun searchAccounts(q: String?, page: Int, size: Int): List<AccountSummary> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val result = if (q.isNullOrBlank()) accountRepo.findAll(pageable).content
        else accountRepo.findByUsernameContainingIgnoreCase(q, pageable).content
        return result.map { it.summary() }
    }
}
