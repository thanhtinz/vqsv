package com.vqsv.game.map

import com.vqsv.dto.MapStateDto
import com.vqsv.dto.PlayerPositionDto
import com.vqsv.entity.MapWildPet
import com.vqsv.game.battle.BattleService
import com.vqsv.repository.*
import com.vqsv.util.GameFormula
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class MapService(
    private val playerRepo: PlayerRepository,
    private val mapRepo: GameMapRepository,
    private val mapWildPetRepo: MapWildPetRepository,
    private val petTemplateRepo: PetTemplateRepository,
    private val battleService: BattleService,
    private val mapWarpRepo: MapWarpRepository
) {
    data class MoveResult(
        val newX: Short,
        val newY: Short,
        val warped: Boolean = false,
        val newMapId: Short? = null,
        val wildEncounter: com.vqsv.game.battle.BattleSession? = null
    )

    /** Mark a player online/offline so REST presence (getMapState) reflects reality. */
    @Transactional
    fun setOnline(playerId: Long, online: Boolean) {
        val p = playerRepo.findByIdOrNull(playerId) ?: return
        if (p.isOnline != online) playerRepo.save(p.copy(isOnline = online))
    }

    /** Clear stale online flags at startup (any flag left over from a previous run). */
    @Transactional
    fun resetAllOnline() = playerRepo.resetAllOnline()

    @Transactional
    fun move(playerId: Long, direction: String): MoveResult {
        val player = playerRepo.findByIdOrNull(playerId)
            ?: throw IllegalArgumentException("Player không tồn tại")
        val map = mapRepo.findByIdOrNull(player.mapId)
            ?: throw IllegalStateException("Map không tồn tại")

        val (dx, dy) = when (direction) {
            "UP"    -> Pair(0, -1)
            "DOWN"  -> Pair(0, 1)
            "LEFT"  -> Pair(-1, 0)
            "RIGHT" -> Pair(1, 0)
            else    -> throw IllegalArgumentException("Hướng không hợp lệ")
        }

        val newX = (player.posX + dx).toShort()
        val newY = (player.posY + dy).toShort()

        if (newX < 0 || newX >= map.width || newY < 0 || newY >= map.height)
            return MoveResult(player.posX, player.posY)

        // Warp tile: stepping onto it teleports to another map (no wild encounter that step).
        val warp = mapWarpRepo.findByFromMap(player.mapId).firstOrNull { it.fromX == newX && it.fromY == newY }
        if (warp != null) {
            playerRepo.save(player.copy(mapId = warp.toMap, posX = warp.toX, posY = warp.toY))
            return MoveResult(warp.toX, warp.toY, warped = true, newMapId = warp.toMap)
        }

        playerRepo.save(player.copy(posX = newX, posY = newY))

        val wildPets = mapWildPetRepo.findByMapId(player.mapId)
        if (wildPets.isNotEmpty()) {
            val totalRate = wildPets.sumOf { it.spawnRate.toInt() }
            if (Random.nextInt(100) < totalRate / wildPets.size) {
                val wildPet = pickWildPet(wildPets)
                if (wildPet != null) {
                    val template = petTemplateRepo.findByIdOrNull(wildPet.templateId)
                    if (template != null) {
                        val level = GameFormula.wildPetLevel(
                            wildPet.minLevel.toInt(), wildPet.maxLevel.toInt()
                        ).toShort()
                        val session = battleService.startPveBattle(playerId, template.id, level, wildPet.id)
                        return MoveResult(newX, newY, wildEncounter = session)
                    }
                }
            }
        }

        return MoveResult(newX, newY)
    }

    private fun pickWildPet(options: List<MapWildPet>): MapWildPet? {
        if (options.isEmpty()) return null
        val roll = Random.nextInt(options.sumOf { it.spawnRate.toInt() })
        var cumulative = 0
        for (opt in options) {
            cumulative += opt.spawnRate
            if (roll < cumulative) return opt
        }
        return options.last()
    }

    fun getMapState(playerId: Long): MapStateDto {
        val player = playerRepo.findByIdOrNull(playerId)
            ?: throw IllegalArgumentException("Player không tồn tại")
        val map = mapRepo.findByIdOrNull(player.mapId)
            ?: throw IllegalStateException("Map không tồn tại")

        val online = playerRepo.findOnlinePlayersInMap(player.mapId)
            .filter { it.id != playerId }
            .map { PlayerPositionDto(it.id, it.name, it.posX, it.posY, it.level) }

        return MapStateDto(
            mapId = player.mapId,
            mapName = map.name,
            posX = player.posX,
            posY = player.posY,
            onlinePlayers = online,
            bgmId = map.bgmId
        )
    }
}
