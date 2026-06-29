package com.vqsv.controller

import com.vqsv.dto.*
import com.vqsv.game.battle.BattleService
import com.vqsv.game.item.ShopService
import com.vqsv.game.map.MapService
import com.vqsv.game.pet.PetService
import com.vqsv.service.AuthService
import com.vqsv.util.JwtUtil
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

// ============================================================
// AUTH CONTROLLER
// ============================================================
@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.register(req))

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(req))
}

// ============================================================
// PLAYER CONTROLLER
// ============================================================
@RestController
@RequestMapping("/api/player")
class PlayerController(
    private val jwtUtil: JwtUtil,
    private val mapService: MapService,
    private val npcRepo: com.vqsv.repository.NpcRepository
) {
    @GetMapping("/map")
    fun getMapState(auth: Authentication): ResponseEntity<MapStateDto> {
        val playerId = (auth.principal as Long)
        return ResponseEntity.ok(mapService.getMapState(playerId))
    }

    @GetMapping("/npcs/{mapId}")
    fun getNpcs(@PathVariable mapId: Short): ResponseEntity<List<NpcDto>> {
        val npcs = npcRepo.findByMapId(mapId).map {
            NpcDto(it.id, it.name, it.spriteId, it.npcType, it.posX, it.posY, it.enemyTemplateId)
        }
        return ResponseEntity.ok(npcs)
    }

    @PostMapping("/move")
    fun move(@Valid @RequestBody req: MoveRequest, auth: Authentication): ResponseEntity<Any> {
        val playerId = (auth.principal as Long)
        val result = mapService.move(playerId, req.direction)
        return if (result.wildEncounter != null) {
            ResponseEntity.ok(mapOf(
                "newX" to result.newX,
                "newY" to result.newY,
                "wildEncounter" to mapOf(
                    "battleId" to result.wildEncounter.battleId,
                    "enemyName" to result.wildEncounter.enemyName,
                    "enemyLevel" to result.wildEncounter.enemyLevel,
                    "enemyHp" to result.wildEncounter.enemyHp,
                    "enemyElement" to result.wildEncounter.enemyElement,
                    "enemySpriteId" to result.wildEncounter.enemySpriteId,
                    "catchable" to result.wildEncounter.catchable
                )
            ))
        } else {
            ResponseEntity.ok(mapOf("newX" to result.newX, "newY" to result.newY))
        }
    }
}

// ============================================================
// PET CONTROLLER
// ============================================================
@RestController
@RequestMapping("/api/pets")
class PetController(private val petService: PetService) {

    @GetMapping
    fun getPets(auth: Authentication): ResponseEntity<List<PetDto>> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(petService.getPlayerPets(playerId))
    }

    @GetMapping("/{petId}/skills")
    fun getPetSkills(@PathVariable petId: Long, auth: Authentication): ResponseEntity<List<SkillInfoDto>> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(petService.petSkills(playerId, petId))
    }

    @PostMapping("/{petId}/heal")
    fun healPet(
        @PathVariable petId: Long,
        @RequestParam itemId: Short,
        auth: Authentication
    ): ResponseEntity<PetDto> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(petService.healPet(playerId, petId, itemId))
    }

    @PostMapping("/{petId}/evolve")
    fun evolvePet(@PathVariable petId: Long, auth: Authentication): ResponseEntity<PetDto> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(petService.evolvePet(playerId, petId))
    }

    @PostMapping("/{petId}/slot")
    fun swapSlot(
        @PathVariable petId: Long,
        @RequestParam slot: Short,
        auth: Authentication
    ): ResponseEntity<Void> {
        val playerId = auth.principal as Long
        petService.swapSlot(playerId, petId, slot)
        return ResponseEntity.ok().build()
    }
}

// ============================================================
// BATTLE CONTROLLER
// ============================================================
@RestController
@RequestMapping("/api/battle")
class BattleController(private val battleService: BattleService) {

    @PostMapping("/action")
    fun action(@Valid @RequestBody req: BattleAction, auth: Authentication): ResponseEntity<BattleTurnResult> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(battleService.processTurn(playerId, req))
    }
}

// ============================================================
// SHOP CONTROLLER
// ============================================================
@RestController
@RequestMapping("/api/shop")
class ShopController(private val shopService: ShopService) {

    @GetMapping
    fun getShop(): ResponseEntity<List<ShopItemDto>> =
        ResponseEntity.ok(shopService.getShopItems())

    @GetMapping("/inventory")
    fun getInventory(auth: Authentication): ResponseEntity<List<InventoryItemDto>> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(shopService.getInventory(playerId))
    }

    @PostMapping("/buy")
    fun buy(@Valid @RequestBody req: BuyRequest, auth: Authentication): ResponseEntity<InventoryItemDto> {
        val playerId = auth.principal as Long
        return ResponseEntity.ok(shopService.buyItem(playerId, req))
    }
}

// ============================================================
// GLOBAL ERROR HANDLER
// ============================================================
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Bean-validation failures (@Valid) -> 400 with the first clean field message.
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val msg = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Dữ liệu không hợp lệ"
        return ResponseEntity.badRequest().body(mapOf("error" to msg))
    }

    // Malformed / unreadable JSON body -> 400.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException) =
        ResponseEntity.badRequest().body(mapOf("error" to "Dữ liệu gửi lên không đọc được"))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException) =
        ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "Bad request")))

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException) =
        ResponseEntity.status(409).body(mapOf("error" to (ex.message ?: "Conflict")))

    @ExceptionHandler(SecurityException::class)
    fun handleForbidden(ex: SecurityException) =
        ResponseEntity.status(403).body(mapOf("error" to (ex.message ?: "Forbidden")))

    // Catch-all: log the real cause server-side, return a generic message so we
    // never leak stack traces / internals to clients.
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<Map<String, String>> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(500).body(mapOf("error" to "Đã xảy ra lỗi, vui lòng thử lại sau"))
    }
}
