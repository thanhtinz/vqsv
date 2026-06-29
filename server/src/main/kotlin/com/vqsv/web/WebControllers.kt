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

/**
 * Public payment-gateway callbacks (no auth — the gateway calls these; permitted by
 * the public-web rule in SecurityConfig).
 *   - {provider}/return : browser redirected here after paying -> redirect to the
 *     website result page.
 *   - {provider}/ipn    : server-to-server notify -> JSON ack (VNPAY format).
 */
@RestController
@RequestMapping("/api/web/public/topup")
class PublicTopupController(private val topupService: TopupService) {

    @GetMapping("/{provider}/return")
    fun gatewayReturn(
        @PathVariable provider: String,
        @RequestParam params: Map<String, String>
    ): org.springframework.web.servlet.view.RedirectView {
        val ok = runCatching { topupService.handleCallback(provider, params) }.getOrDefault(false)
        return org.springframework.web.servlet.view.RedirectView("/nap/ket-qua?success=$ok")
    }

    @RequestMapping("/{provider}/ipn", method = [RequestMethod.GET, RequestMethod.POST])
    fun gatewayIpn(
        @PathVariable provider: String,
        @RequestParam params: Map<String, String>
    ): ResponseEntity<Map<String, String>> {
        val ok = runCatching { topupService.handleCallback(provider, params) }.getOrDefault(false)
        return ResponseEntity.ok(
            mapOf(
                "RspCode" to if (ok) "00" else "99",
                "Message" to if (ok) "Confirm Success" else "Invalid signature or unconfirmed"
            )
        )
    }
}

@RestController
@RequestMapping("/api/web/shop")
class WebShopController(private val webShopService: WebShopService) {
    @PostMapping("/buy")
    fun buy(@RequestBody req: WebBuyRequest, auth: Authentication): RedeemResult =
        webShopService.buy(auth.principal as Long, req)
}
