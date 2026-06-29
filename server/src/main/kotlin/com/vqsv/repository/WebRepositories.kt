package com.vqsv.repository

import com.vqsv.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface GameServerRepository : JpaRepository<GameServer, Short> {
    fun findByCode(code: String): Optional<GameServer>
    fun existsByCode(code: String): Boolean
    fun findAllByOrderBySortOrderAsc(): List<GameServer>
    fun findByStatusNotOrderBySortOrderAsc(status: String): List<GameServer>
    fun findByCrossGroup(crossGroup: String): List<GameServer>
}

@Repository
interface GiftcodeRepository : JpaRepository<Giftcode, Long> {
    fun findByCode(code: String): Optional<Giftcode>
    fun existsByCode(code: String): Boolean
}

@Repository
interface GiftcodeRedemptionRepository : JpaRepository<GiftcodeRedemption, Long> {
    fun countByGiftcodeIdAndAccountId(giftcodeId: Long, accountId: Long): Long
    fun findByAccountIdOrderByRedeemedAtDesc(accountId: Long): List<GiftcodeRedemption>
}

@Repository
interface TopupPackageRepository : JpaRepository<TopupPackage, Short> {
    fun findByActiveTrueOrderBySortOrderAsc(): List<TopupPackage>
}

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, Long> {
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<PaymentTransaction>
    fun findByStatusOrderByCreatedAtDesc(status: String, pageable: Pageable): Page<PaymentTransaction>
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<PaymentTransaction>
    fun countByStatus(status: String): Long

    @Query("SELECT COALESCE(SUM(t.amountVnd),0) FROM PaymentTransaction t WHERE t.status = 'SUCCESS'")
    fun totalRevenue(): Long
}

@Repository
interface WebShopProductRepository : JpaRepository<WebShopProduct, Int> {
    fun findByActiveTrueOrderBySortOrderAsc(): List<WebShopProduct>
}

@Repository
interface WebShopOrderRepository : JpaRepository<WebShopOrder, Long> {
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<WebShopOrder>
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<WebShopOrder>
}

@Repository
interface NewsPostRepository : JpaRepository<NewsPost, Long> {
    fun findByPublishedTrueOrderByPublishedAtDesc(pageable: Pageable): Page<NewsPost>
    fun findBySlugAndPublishedTrue(slug: String): Optional<NewsPost>
    fun existsBySlug(slug: String): Boolean
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<NewsPost>
}

@Repository
interface EventPostRepository : JpaRepository<EventPost, Long> {
    fun findByActiveTrueOrderByStartsAtDesc(): List<EventPost>
}

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AuditLog>
}

@Repository
interface PaymentSettingsRepository : JpaRepository<PaymentSettings, Short>
