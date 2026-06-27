package com.vqsv.game.item

import com.vqsv.dto.BuyRequest
import com.vqsv.dto.InventoryItemDto
import com.vqsv.dto.ShopItemDto
import com.vqsv.entity.PlayerItem
import com.vqsv.repository.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ShopService(
    private val playerRepo: PlayerRepository,
    private val shopListingRepo: ShopListingRepository,
    private val playerItemRepo: PlayerItemRepository,
    private val itemRepo: ItemRepository
) {
    fun getShopItems(): List<ShopItemDto> =
        shopListingRepo.findAllByOrderBySortOrderAsc().map {
            ShopItemDto(
                id = it.id,
                itemId = it.item.id,
                itemName = it.item.name,
                itemType = it.item.itemType,
                effectVal = it.item.effectVal,
                iconId = it.item.iconId,
                priceGold = it.priceGold,
                priceMedal = it.priceMedal,
                description = it.item.description
            )
        }

    fun getInventory(playerId: Long): List<InventoryItemDto> =
        playerItemRepo.findByPlayerId(playerId).map {
            InventoryItemDto(
                itemId = it.item.id,
                name = it.item.name,
                itemType = it.item.itemType,
                effectVal = it.item.effectVal,
                iconId = it.item.iconId,
                quantity = it.quantity,
                description = it.item.description
            )
        }

    @Transactional
    fun buyItem(playerId: Long, req: BuyRequest): InventoryItemDto {
        val player = playerRepo.findByIdOrNull(playerId)
            ?: throw IllegalArgumentException("Player không tồn tại")
        val listing = shopListingRepo.findByIdOrNull(req.shopListingId)
            ?: throw IllegalArgumentException("Vật phẩm không có trong cửa hàng")

        val totalGold = listing.priceGold?.let { it * req.quantity }
        val totalMedal = listing.priceMedal?.let { it * req.quantity }

        if (totalGold != null && player.kimTien < totalGold)
            throw IllegalStateException("Không đủ kim tiền (cần $totalGold, có ${player.kimTien})")
        if (totalMedal != null && player.huyChuong < totalMedal)
            throw IllegalStateException("Không đủ huy chương (cần $totalMedal, có ${player.huyChuong})")

        playerRepo.save(player.copy(
            kimTien = player.kimTien - (totalGold ?: 0),
            huyChuong = player.huyChuong - (totalMedal ?: 0)
        ))

        val existing = playerItemRepo.findByPlayerIdAndItemId(playerId, listing.item.id)
        val playerItem = if (existing.isPresent) {
            playerItemRepo.save(existing.get().copy(quantity = existing.get().quantity + req.quantity))
        } else {
            playerItemRepo.save(PlayerItem(player = player, item = listing.item, quantity = req.quantity))
        }

        return InventoryItemDto(
            itemId = listing.item.id,
            name = listing.item.name,
            itemType = listing.item.itemType,
            effectVal = listing.item.effectVal,
            iconId = listing.item.iconId,
            quantity = playerItem.quantity,
            description = listing.item.description
        )
    }
}
