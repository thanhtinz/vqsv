package com.vqsv.controller

import com.vqsv.dto.*
import com.vqsv.game.battle.BattleService
import com.vqsv.game.item.ShopService
import com.vqsv.game.map.MapService
import com.vqsv.game.pet.PetService
import com.vqsv.service.AuthService
import com.vqsv.util.JwtUtil
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

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

@RestController
@RequestMapping("/api/player")
class PlayerController(private val jwtUtil: JwtUtil, private val mapService: MapService) {

    @GetMapping("/map")
    fun getMapState(auth: Authentication): ResponseEntity<MapStateDto> =
        ResponseEntity.ok(mapService.getMapState(auth.principal as Long))

    @PostMapping("/move")
    fun move(@Valid @RequestBody req: MoveRequest, auth: Authentication): ResponseEntity<Any> {
        val result = mapService.move(auth.principal as Long, req.direction)
        return if (result.wildEncounter != null) {
            val enc = result.wildEncounter
            ResponseEntity.ok(mapOf(
                "newX" to result.newX, "newY" to result.newY,
                "wildEncounter" to mapOf(
                    "battleId" to enc.battleId, "enemyName" to enc.enemyName,
                    "enemyLevel" to enc.enemyLevel, "enemyHp" to enc.enemyHp,
                    "enemyElement" to enc.enemyElement, "catchable" to enc.catchable
                )
            ))
        } else {
            ResponseEntity.ok(mapOf("newX" to result.newX, "newY" to result.newY))
        }
    }
}

@RestController
@RequestMapping("/api/pets")
class PetController(private val petService: PetService) {

    @GetMapping
    fun getPets(auth: Authentication): ResponseEntity<List<PetDto>> =
        ResponseEntity.ok(petService.getPlayerPets(auth.principal as Long))

    @PostMapping("/{petId}/heal")
    fun healPet(@PathVariable petId: Long, @RequestParam itemId: Short, auth: Authentication): ResponseEntity<PetDto> =
        ResponseEntity.ok(petService.healPet(auth.principal as Long, petId, itemId))

    @PostMapping("/{petId}/evolve")
    fun evolvePet(@PathVariable petId: Long, auth: Authentication): ResponseEntity<PetDto> =
        ResponseEntity.ok(petService.evolvePet(auth.principal as Long, petId))

    @PostMapping("/{petId}/slot")
    fun swapSlot(@PathVariable petId: Long, @RequestParam slot: Short, auth: Authentication): ResponseEntity<Void> {
        petService.swapSlot(auth.principal as Long, petId, slot)
        return ResponseEntity.ok().build()
    }
}

@RestController
@RequestMapping("/api/battle")
class BattleController(private val battleService: BattleService) {

    @PostMapping("/action")
    fun action(@Valid @RequestBody req: BattleAction, auth: Authentication): ResponseEntity<BattleTurnResult> =
        ResponseEntity.ok(battleService.processTurn(auth.principal as Long, req))
}

@RestController
@RequestMapping("/api/shop")
class ShopController(private val shopService: ShopService) {

    @GetMapping
    fun getShop(): ResponseEntity<List<ShopItemDto>> = ResponseEntity.ok(shopService.getShopItems())

    @GetMapping("/inventory")
    fun getInventory(auth: Authentication): ResponseEntity<List<InventoryItemDto>> =
        ResponseEntity.ok(shopService.getInventory(auth.principal as Long))

    @PostMapping("/buy")
    fun buy(@Valid @RequestBody req: BuyRequest, auth: Authentication): ResponseEntity<InventoryItemDto> =
        ResponseEntity.ok(shopService.buyItem(auth.principal as Long, req))
}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException) =
        ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "Bad request")))

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException) =
        ResponseEntity.status(409).body(mapOf("error" to (ex.message ?: "Conflict")))

    @ExceptionHandler(SecurityException::class)
    fun handleForbidden(ex: SecurityException) =
        ResponseEntity.status(403).body(mapOf("error" to (ex.message ?: "Forbidden")))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception) =
        ResponseEntity.status(500).body(mapOf("error" to "Lỗi server: ${ex.message}"))
}
