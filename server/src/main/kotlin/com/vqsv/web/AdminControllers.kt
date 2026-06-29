package com.vqsv.web

import com.vqsv.entity.*
import com.vqsv.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.text.Normalizer
import java.time.Instant

private fun aid(auth: Authentication) = auth.principal as Long

// ============================================================
// ADMIN: dashboard + users + payments + servers + grant
// ============================================================
@RestController
@RequestMapping("/api/admin")
class AdminCoreController(
    private val adminService: AdminService,
    private val playerRepo: PlayerRepository,
    private val paymentRepo: PaymentTransactionRepository,
    private val auditRepo: AuditLogRepository,
    private val serverRepo: GameServerRepository
) {
    @GetMapping("/dashboard")
    fun dashboard(): AdminDashboard = adminService.dashboard()

    // ----- Users -----
    @GetMapping("/users")
    fun users(@RequestParam(required = false) q: String?,
              @RequestParam(defaultValue = "0") page: Int,
              @RequestParam(defaultValue = "20") size: Int): List<AccountSummary> =
        adminService.searchAccounts(q, page, size)

    @PatchMapping("/users/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody req: AdminUpdateAccountRequest, auth: Authentication): AccountSummary =
        adminService.updateAccount(aid(auth), id, req)

    @PostMapping("/users/{id}/password")
    fun resetPassword(@PathVariable id: Long, @RequestBody req: AdminResetPasswordRequest, auth: Authentication): ResponseEntity<Map<String, String>> {
        adminService.resetPassword(aid(auth), id, req.newPassword)
        return ResponseEntity.ok(mapOf("message" to "Đã đặt lại mật khẩu"))
    }

    @GetMapping("/users/{id}/characters")
    fun userCharacters(@PathVariable id: Long) = playerRepo.findAllByAccountId(id)

    // ----- Payments -----
    @GetMapping("/payments")
    fun payments(@RequestParam(required = false) status: String?,
                 @RequestParam(defaultValue = "0") page: Int,
                 @RequestParam(defaultValue = "20") size: Int): List<PaymentTransaction> {
        val pageable = PageRequest.of(page, size)
        return if (status.isNullOrBlank()) paymentRepo.findAllByOrderByCreatedAtDesc(pageable).content
        else paymentRepo.findByStatusOrderByCreatedAtDesc(status, pageable).content
    }

    @PostMapping("/payments/{id}/approve")
    fun approve(@PathVariable id: Long, auth: Authentication): ResponseEntity<Map<String, String>> {
        adminService.approvePayment(aid(auth), id)
        return ResponseEntity.ok(mapOf("message" to "Đã duyệt giao dịch"))
    }

    @PostMapping("/payments/{id}/reject")
    fun reject(@PathVariable id: Long, auth: Authentication): ResponseEntity<Map<String, String>> {
        adminService.rejectPayment(aid(auth), id)
        return ResponseEntity.ok(mapOf("message" to "Đã từ chối giao dịch"))
    }

    // ----- Grant reward to a character -----
    @PostMapping("/grant")
    fun grant(@RequestBody req: GrantRewardRequest, auth: Authentication): RedeemResult =
        adminService.grant(aid(auth), req)

    // ----- Servers (create / list / update / merge / cross-server) -----
    @GetMapping("/servers")
    fun servers(): List<GameServer> = serverRepo.findAllByOrderBySortOrderAsc()

    @PostMapping("/servers")
    fun createServer(@RequestBody server: GameServer, auth: Authentication): GameServer =
        adminService.createServer(aid(auth), server)

    @PutMapping("/servers/{id}")
    fun updateServer(@PathVariable id: Short, @RequestBody server: GameServer): GameServer {
        require(serverRepo.existsById(id)) { "Máy chủ không tồn tại" }
        return serverRepo.save(server.copy(id = id))
    }

    @PostMapping("/servers/merge")
    fun merge(@RequestBody req: MergeServersRequest, auth: Authentication): Map<String, Any> =
        adminService.mergeServers(aid(auth), req)

    @PostMapping("/servers/cross")
    fun cross(@RequestBody req: CrossServerRequest, auth: Authentication): List<GameServer> =
        adminService.setCrossGroup(aid(auth), req)

    // ----- Audit log -----
    @GetMapping("/audit")
    fun audit(@RequestParam(defaultValue = "0") page: Int,
              @RequestParam(defaultValue = "50") size: Int): List<AuditLog> =
        auditRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).content
}

// ============================================================
// ADMIN: platform content CRUD (giftcode, topup, webshop, news, events)
// ============================================================
@RestController
@RequestMapping("/api/admin")
class AdminPlatformController(
    private val giftcodeRepo: GiftcodeRepository,
    private val topupRepo: TopupPackageRepository,
    private val webProductRepo: WebShopProductRepository,
    private val newsRepo: NewsPostRepository,
    private val eventRepo: EventPostRepository,
    private val paymentSettingsRepo: PaymentSettingsRepository
) {
    // ----- Payment settings (SePay) — configured here, not in code/env -----
    @GetMapping("/payment-settings")
    fun paymentSettings(): PaymentSettingsDto {
        val s = paymentSettingsRepo.findById(1).orElseGet { PaymentSettings() }
        return PaymentSettingsDto(s.enabled, s.sepayApiKey, s.bankAccount, s.bankCode, s.accountHolder, s.prefix)
    }

    @PutMapping("/payment-settings")
    fun savePaymentSettings(@RequestBody dto: PaymentSettingsDto): PaymentSettingsDto {
        val s = paymentSettingsRepo.findById(1).orElseGet { PaymentSettings() }
        s.enabled = dto.enabled
        s.sepayApiKey = dto.sepayApiKey.trim()
        s.bankAccount = dto.bankAccount.trim()
        s.bankCode = dto.bankCode.trim()
        s.accountHolder = dto.accountHolder.trim()
        s.prefix = dto.prefix.trim().ifBlank { "VQSV" }
        paymentSettingsRepo.save(s)
        return paymentSettings()
    }

    // ----- Giftcodes -----
    @GetMapping("/giftcodes")
    fun giftcodes(): List<Giftcode> = giftcodeRepo.findAll()

    @PostMapping("/giftcodes")
    fun saveGiftcode(@RequestBody gc: Giftcode): Giftcode {
        val normalized = gc.copy(code = gc.code.trim().uppercase())
        if (gc.id == 0L && giftcodeRepo.existsByCode(normalized.code))
            throw IllegalArgumentException("Mã giftcode đã tồn tại")
        return giftcodeRepo.save(normalized)
    }

    @DeleteMapping("/giftcodes/{id}")
    fun deleteGiftcode(@PathVariable id: Long): ResponseEntity<Void> { giftcodeRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Top-up packages -----
    @GetMapping("/topup-packages")
    fun topupPackages(): List<TopupPackage> = topupRepo.findAll()

    @PostMapping("/topup-packages")
    fun saveTopup(@RequestBody p: TopupPackage): TopupPackage = topupRepo.save(p)

    @DeleteMapping("/topup-packages/{id}")
    fun deleteTopup(@PathVariable id: Short): ResponseEntity<Void> { topupRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Web shop products -----
    @GetMapping("/webshop-products")
    fun webProducts(): List<WebShopProduct> = webProductRepo.findAll()

    @PostMapping("/webshop-products")
    fun saveWebProduct(@RequestBody p: WebShopProduct): WebShopProduct = webProductRepo.save(p)

    @DeleteMapping("/webshop-products/{id}")
    fun deleteWebProduct(@PathVariable id: Int): ResponseEntity<Void> { webProductRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- News -----
    @GetMapping("/news")
    fun news(@RequestParam(defaultValue = "0") page: Int,
             @RequestParam(defaultValue = "20") size: Int): List<NewsPost> =
        newsRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).content

    @PostMapping("/news")
    fun saveNews(@RequestBody post: NewsPost): NewsPost {
        var p = post
        if (p.slug.isBlank()) p = p.copy(slug = slugify(p.title))
        if (p.published && p.publishedAt == null) p = p.copy(publishedAt = Instant.now())
        return newsRepo.save(p)
    }

    @DeleteMapping("/news/{id}")
    fun deleteNews(@PathVariable id: Long): ResponseEntity<Void> { newsRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Events -----
    @GetMapping("/events")
    fun events(): List<EventPost> = eventRepo.findAll()

    @PostMapping("/events")
    fun saveEvent(@RequestBody e: EventPost): EventPost = eventRepo.save(e)

    @DeleteMapping("/events/{id}")
    fun deleteEvent(@PathVariable id: Long): ResponseEntity<Void> { eventRepo.deleteById(id); return ResponseEntity.noContent().build() }

    private fun slugify(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d").replace("Đ", "D")
        return normalized.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(200)
    }
}

// ============================================================
// ADMIN: in-game content CRUD (maps, pets/mobs, items, shop, npc, enemies)
// ============================================================
@RestController
@RequestMapping("/api/admin/game")
class AdminGameController(
    private val mapRepo: GameMapRepository,
    private val petRepo: PetTemplateRepository,
    private val itemRepo: ItemRepository,
    private val wildPetRepo: MapWildPetRepository,
    private val shopRepo: ShopListingRepository,
    private val npcRepo: NpcRepository,
    private val enemyRepo: NpcEnemyTemplateRepository,
    private val warpRepo: MapWarpRepository,
    private val questRepo: QuestRepository
) {
    // ----- Maps -----
    @GetMapping("/maps") fun maps(): List<GameMap> = mapRepo.findAll()
    @PostMapping("/maps") fun saveMap(@RequestBody m: GameMap): GameMap = mapRepo.save(m)
    @DeleteMapping("/maps/{id}") fun delMap(@PathVariable id: Short): ResponseEntity<Void> { mapRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Pet templates (pets / mobs) -----
    @GetMapping("/pets") fun pets(): List<PetTemplate> = petRepo.findAll()
    @PostMapping("/pets") fun savePet(@RequestBody p: PetTemplate): PetTemplate = petRepo.save(p)
    @DeleteMapping("/pets/{id}") fun delPet(@PathVariable id: Short): ResponseEntity<Void> { petRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Items -----
    @GetMapping("/items") fun items(): List<Item> = itemRepo.findAll()
    @PostMapping("/items") fun saveItem(@RequestBody i: Item): Item = itemRepo.save(i)
    @DeleteMapping("/items/{id}") fun delItem(@PathVariable id: Short): ResponseEntity<Void> { itemRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Wild pets (spawns on maps) -----
    @GetMapping("/wild-pets") fun wildPets(): List<MapWildPet> = wildPetRepo.findAll()
    @PostMapping("/wild-pets") fun saveWild(@RequestBody w: MapWildPet): MapWildPet = wildPetRepo.save(w)
    @DeleteMapping("/wild-pets/{id}") fun delWild(@PathVariable id: Int): ResponseEntity<Void> { wildPetRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- In-game shop listings -----
    @GetMapping("/shop") fun shop(): List<ShopListing> = shopRepo.findAllByOrderBySortOrderAsc()
    @PostMapping("/shop") fun saveShop(@RequestBody dto: ShopListingDto): ShopListing {
        val item = itemRepo.findById(dto.itemId).orElseThrow { IllegalArgumentException("Vật phẩm không tồn tại") }
        return shopRepo.save(ShopListing(id = dto.id, item = item, priceGold = dto.priceGold,
            priceMedal = dto.priceMedal, sortOrder = dto.sortOrder))
    }
    @DeleteMapping("/shop/{id}") fun delShop(@PathVariable id: Int): ResponseEntity<Void> { shopRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- NPCs -----
    @GetMapping("/npcs") fun npcs(): List<Npc> = npcRepo.findAll()
    @PostMapping("/npcs") fun saveNpc(@RequestBody n: Npc): Npc = npcRepo.save(n)
    @DeleteMapping("/npcs/{id}") fun delNpc(@PathVariable id: Short): ResponseEntity<Void> { npcRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Enemy templates (bosses / trainers) -----
    @GetMapping("/enemies") fun enemies(): List<NpcEnemyTemplate> = enemyRepo.findAll()
    @PostMapping("/enemies") fun saveEnemy(@RequestBody e: NpcEnemyTemplate): NpcEnemyTemplate = enemyRepo.save(e)
    @DeleteMapping("/enemies/{id}") fun delEnemy(@PathVariable id: Short): ResponseEntity<Void> { enemyRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Map warps -----
    @GetMapping("/warps") fun warps(): List<MapWarp> = warpRepo.findAll()
    @PostMapping("/warps") fun saveWarp(@RequestBody w: MapWarp): MapWarp = warpRepo.save(w)
    @DeleteMapping("/warps/{id}") fun delWarp(@PathVariable id: Int): ResponseEntity<Void> { warpRepo.deleteById(id); return ResponseEntity.noContent().build() }

    // ----- Quests -----
    @GetMapping("/quests") fun quests(): List<Quest> = questRepo.findAll()
    @PostMapping("/quests") fun saveQuest(@RequestBody q: Quest): Quest = questRepo.save(q)
    @DeleteMapping("/quests/{id}") fun delQuest(@PathVariable id: Short): ResponseEntity<Void> { questRepo.deleteById(id); return ResponseEntity.noContent().build() }
}
