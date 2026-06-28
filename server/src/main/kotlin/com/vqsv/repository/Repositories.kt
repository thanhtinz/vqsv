package com.vqsv.repository

import com.vqsv.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByUsername(username: String): Optional<Account>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByUsernameContainingIgnoreCase(q: String, pageable: Pageable): Page<Account>
    fun countByRole(role: String): Long
}

@Repository
interface PlayerRepository : JpaRepository<Player, Long> {
    fun findByAccountId(accountId: Long): Optional<Player>
    fun findAllByAccountId(accountId: Long): List<Player>
    fun findByAccountIdAndServerId(accountId: Long, serverId: Short): Optional<Player>
    fun findByName(name: String): Optional<Player>
    fun existsByName(name: String): Boolean
    fun existsByServerIdAndName(serverId: Short, name: String): Boolean
    fun countByServerId(serverId: Short): Long

    @Query("SELECT p FROM Player p WHERE p.mapId = :mapId AND p.isOnline = true")
    fun findOnlinePlayersInMap(mapId: Short): List<Player>

    // Leaderboard: single server
    fun findByServerIdOrderByLevelDescExpDesc(serverId: Short, pageable: Pageable): List<Player>

    // Leaderboard: cross-server (lien-server) over a set of server ids
    @Query("SELECT p FROM Player p WHERE p.serverId IN :serverIds ORDER BY p.level DESC, p.exp DESC")
    fun leaderboardAcrossServers(serverIds: List<Short>, pageable: Pageable): List<Player>

    // Server merge: move all characters from one server to another
    @Modifying
    @Query("UPDATE Player p SET p.serverId = :target WHERE p.serverId = :source")
    fun reassignServer(source: Short, target: Short): Int

    @Modifying
    @Query("UPDATE Player p SET p.isOnline = false WHERE p.isOnline = true")
    fun resetAllOnline()
}

@Repository
interface PetTemplateRepository : JpaRepository<PetTemplate, Short> {
    fun findByElement(element: String): List<PetTemplate>
}

@Repository
interface PlayerPetRepository : JpaRepository<PlayerPet, Long> {
    fun findByPlayerId(playerId: Long): List<PlayerPet>
    fun findByPlayerIdAndSlot(playerId: Long, slot: Short): Optional<PlayerPet>

    @Query("SELECT p FROM PlayerPet p WHERE p.player.id = :playerId ORDER BY p.slot")
    fun findByPlayerIdOrdered(playerId: Long): List<PlayerPet>

    fun countByPlayerId(playerId: Long): Long
}

@Repository
interface GameMapRepository : JpaRepository<GameMap, Short> {
    fun findByMinLevelLessThanEqual(level: Short): List<GameMap>
}

@Repository
interface ItemRepository : JpaRepository<Item, Short> {
    fun findByItemType(itemType: String): List<Item>
}

@Repository
interface PlayerItemRepository : JpaRepository<PlayerItem, PlayerItemId> {
    @Query("SELECT pi FROM PlayerItem pi WHERE pi.player.id = :playerId")
    fun findByPlayerId(playerId: Long): List<PlayerItem>

    @Query("SELECT pi FROM PlayerItem pi WHERE pi.player.id = :playerId AND pi.item.id = :itemId")
    fun findByPlayerIdAndItemId(playerId: Long, itemId: Short): Optional<PlayerItem>
}

@Repository
interface BadgeRepository : JpaRepository<Badge, Short>

@Repository
interface PlayerBadgeRepository : JpaRepository<PlayerBadge, PlayerBadgeId> {
    @Query("SELECT pb FROM PlayerBadge pb WHERE pb.player.id = :playerId")
    fun findByPlayerId(playerId: Long): List<PlayerBadge>
}

@Repository
interface BattleLogRepository : JpaRepository<BattleLog, Long> {
    fun findByAttackerIdOrderByCreatedAtDesc(attackerId: Long): List<BattleLog>
    fun countByAttackerIdAndWinnerId(attackerId: Long, winnerId: Long): Long
}

@Repository
interface MapWildPetRepository : JpaRepository<MapWildPet, Int> {
    fun findByMapId(mapId: Short): List<MapWildPet>
}

@Repository
interface ShopListingRepository : JpaRepository<ShopListing, Int> {
    fun findAllByOrderBySortOrderAsc(): List<ShopListing>
}
