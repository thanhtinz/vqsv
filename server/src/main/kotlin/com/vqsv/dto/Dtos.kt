package com.vqsv.dto

import jakarta.validation.constraints.*

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 4, max = 32)
    val username: String,

    @field:NotBlank @field:Size(min = 6, max = 64)
    val password: String,

    @field:Email
    val email: String? = null,

    @field:NotBlank @field:Size(min = 4, max = 32)
    val playerName: String
)

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class AuthResponse(
    val token: String,
    val player: PlayerDto
)

data class PlayerDto(
    val id: Long,
    val name: String,
    val level: Short,
    val exp: Int,
    val expNext: Int,
    val kimTien: Int,
    val huyChuong: Int,
    val mapId: Short,
    val posX: Short,
    val posY: Short,
    val hp: Int,
    val hpMax: Int
)

data class PetDto(
    val id: Long,
    val templateId: Short,
    val name: String,
    val nickname: String?,
    val element: String,
    val spriteId: Short,
    val level: Short,
    val exp: Int,
    val expNext: Int,
    val hp: Int,
    val hpMax: Int,
    val atk: Short,
    val def: Short,
    val spd: Short,
    val slot: Short,
    val loyalty: Short,
    val canEvolve: Boolean,
    val evolveLv: Short?
)

data class SkillInfoDto(
    val id: Int,
    val name: String,
    val element: Int,
    val power: Int,
    val spCost: Int,
    val requiredLevel: Int,
    val description: String?
)

data class BattleStartRequest(
    val targetType: String,
    val targetId: Long
)

data class BattleAction(
    val battleId: String,
    val action: String,
    val itemId: Short? = null,
    val petSlot: Short? = null
)

data class BattleTurnResult(
    val battleId: String,
    val turn: Int,
    val playerPetHp: Int,
    val enemyHp: Int,
    val log: List<String>,
    val status: String
)

data class MoveRequest(
    val direction: String
)

data class MapStateDto(
    val mapId: Short,
    val mapName: String,
    val posX: Short,
    val posY: Short,
    val onlinePlayers: List<PlayerPositionDto>,
    val bgmId: Short
)

data class PlayerPositionDto(
    val playerId: Long,
    val name: String,
    val posX: Short,
    val posY: Short,
    val level: Short
)

data class NpcDto(
    val id: Short,
    val name: String,
    val spriteId: Short,
    val npcType: String,
    val posX: Short,
    val posY: Short,
    val enemyTemplateId: Short?
)

data class ShopItemDto(
    val id: Int,
    val itemId: Short,
    val itemName: String,
    val itemType: String,
    val effectVal: Int,
    val iconId: Short,
    val priceGold: Int?,
    val priceMedal: Int?,
    val description: String?
)

data class BuyRequest(
    val shopListingId: Int,
    val quantity: Int = 1
)

data class InventoryItemDto(
    val itemId: Short,
    val name: String,
    val itemType: String,
    val effectVal: Int,
    val iconId: Short,
    val quantity: Int,
    val description: String?
)

data class WsMessage(
    val type: String,
    val payload: Any? = null
)

data class ChatRequest(
    @field:NotBlank @field:Size(max = 128)
    val text: String
)
