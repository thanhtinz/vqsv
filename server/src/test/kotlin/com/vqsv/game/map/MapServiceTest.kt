package com.vqsv.game.map

import com.vqsv.entity.GameMap
import com.vqsv.entity.MapWarp
import com.vqsv.entity.Player
import com.vqsv.game.battle.BattleService
import com.vqsv.repository.GameMapRepository
import com.vqsv.repository.MapWarpRepository
import com.vqsv.repository.MapWildPetRepository
import com.vqsv.repository.PetTemplateRepository
import com.vqsv.repository.PlayerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

/**
 * Movement + warp rules with mocked repositories. The warp branch returns before any
 * wild-encounter roll, so these assertions are deterministic.
 */
class MapServiceTest {

    private val playerRepo = mock(PlayerRepository::class.java)
    private val mapRepo = mock(GameMapRepository::class.java)
    private val wildRepo = mock(MapWildPetRepository::class.java)
    private val tmplRepo = mock(PetTemplateRepository::class.java)
    private val battleService = mock(BattleService::class.java)
    private val warpRepo = mock(MapWarpRepository::class.java)
    private val svc = MapService(playerRepo, mapRepo, wildRepo, tmplRepo, battleService, warpRepo)

    private fun setup(player: Player, warps: List<MapWarp> = emptyList()) {
        `when`(playerRepo.findById(1L)).thenReturn(Optional.of(player))
        `when`(mapRepo.findById(1)).thenReturn(Optional.of(GameMap(id = 1, width = 20, height = 20)))
        `when`(warpRepo.findByFromMap(1)).thenReturn(warps)
        `when`(wildRepo.findByMapId(1)).thenReturn(emptyList())
    }

    @Test
    fun `stepping onto a warp tile teleports to the destination map`() {
        setup(
            Player(id = 1, mapId = 1, posX = 18, posY = 10),
            warps = listOf(MapWarp(fromMap = 1, fromX = 19, fromY = 10, toMap = 2, toX = 1, toY = 10))
        )

        val r = svc.move(1L, "RIGHT")  // 18 -> 19 lands on the warp tile

        assertTrue(r.warped, "the move should warp")
        assertEquals(2.toShort(), r.newMapId)
        assertEquals(1.toShort(), r.newX)
        assertEquals(10.toShort(), r.newY)
    }

    @Test
    fun `a normal step does not warp`() {
        setup(
            Player(id = 1, mapId = 1, posX = 5, posY = 5),
            warps = listOf(MapWarp(fromMap = 1, fromX = 19, fromY = 10, toMap = 2, toX = 1, toY = 10))
        )

        val r = svc.move(1L, "RIGHT")  // 5 -> 6, not a warp tile

        assertFalse(r.warped)
        assertEquals(6.toShort(), r.newX)
        assertEquals(5.toShort(), r.newY)
    }
}
