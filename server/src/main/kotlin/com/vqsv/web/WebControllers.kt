package com.vqsv.web

import com.vqsv.entity.EventPost
import com.vqsv.entity.NewsPost
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

// ============================================================
// PUBLIC: auth
// ============================================================
@RestController
@RequestMapping("/api/web/auth")
class WebAuthController(private val webAuthService: WebAuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: WebLoginRequest): ResponseEntity<WebAuthResponse> =
        ResponseEntity.ok(webAuthService.login(req))

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: WebRegisterRequest): ResponseEntity<WebAuthResponse> =
        ResponseEntity.ok(webAuthService.register(req))
}

// ============================================================
// PUBLIC: content (news, events, servers, leaderboard, shop & topup catalogs)
// ============================================================
@RestController
@RequestMapping("/api/web/public")
class PublicContentController(
    private val contentService: ContentService,
    private val leaderboardService: LeaderboardService,
    private val topupService: TopupService,
    private val webShopService: WebShopService
) {
    @GetMapping("/news")
    fun news(@RequestParam(defaultValue = "0") page: Int,
             @RequestParam(defaultValue = "10") size: Int): List<NewsListItem> =
        contentService.news(page, size)

    @GetMapping("/news/{slug}")
    fun newsDetail(@PathVariable slug: String): NewsPost = contentService.newsBySlug(slug)

    @GetMapping("/events")
    fun events(): List<EventPost> = contentService.events()

    @GetMapping("/servers")
    fun servers(): List<ServerDto> = contentService.servers()

    @GetMapping("/leaderboard")
    fun leaderboard(@RequestParam(required = false) serverId: Short?,
                    @RequestParam(required = false) crossGroup: String?,
                    @RequestParam(defaultValue = "50") limit: Int): List<RankRow> =
        leaderboardService.ranking(serverId, crossGroup, limit.coerceAtMost(200))

    @GetMapping("/topup/packages")
    fun topupPackages(): List<TopupPackageDto> = topupService.packages()

    @GetMapping("/shop/products")
    fun shopProducts(): List<WebShopProductDto> = webShopService.products()
}

// ============================================================
// AUTHENTICATED: profile
// ============================================================
@RestController
@RequestMapping("/api/web/profile")
class ProfileController(private val profileService: ProfileService) {

    @GetMapping
    fun me(auth: Authentication): AccountSummary = profileService.me(auth.principal as Long)

    @PostMapping("/password")
    fun changePassword(@Valid @RequestBody req: ChangePasswordRequest, auth: Authentication): ResponseEntity<Map<String, String>> {
        profileService.changePassword(auth.principal as Long, req)
        return ResponseEntity.ok(mapOf("message" to "Đổi mật khẩu thành công"))
    }

    @GetMapping("/characters")
    fun characters(auth: Authentication): List<CharacterSummary> =
        profileService.characters(auth.principal as Long)

    @GetMapping("/transactions")
    fun transactions(auth: Authentication): List<TransactionDto> =
        profileService.transactions(auth.principal as Long)
}

// ============================================================
// AUTHENTICATED: giftcode, topup, webshop
// ============================================================
@RestController
@RequestMapping("/api/web/giftcode")
class GiftcodeController(private val giftcodeService: GiftcodeService) {
    @PostMapping("/redeem")
    fun redeem(@Valid @RequestBody req: RedeemRequest, auth: Authentication): RedeemResult =
        giftcodeService.redeem(auth.principal as Long, req)
}

@RestController
@RequestMapping("/api/web/topup")
class TopupController(private val topupService: TopupService) {
    @PostMapping("/order")
    fun order(@RequestBody req: TopupOrderRequest, auth: Authentication): TopupOrderResponse =
        topupService.createOrder(auth.principal as Long, req)
}

@RestController
@RequestMapping("/api/web/shop")
class WebShopController(private val webShopService: WebShopService) {
    @PostMapping("/buy")
    fun buy(@RequestBody req: WebBuyRequest, auth: Authentication): RedeemResult =
        webShopService.buy(auth.principal as Long, req)
}
