package com.vqsv.game.pet

import com.vqsv.dto.PetDto
import com.vqsv.entity.PlayerPet
import com.vqsv.repository.*
import com.vqsv.util.GameFormula
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PetService(
    private val playerPetRepo: PlayerPetRepository,
    private val petTemplateRepo: PetTemplateRepository,
    private val playerItemRepo: PlayerItemRepository
) {
    fun getPlayerPets(playerId: Long): List<PetDto> =
        playerPetRepo.findByPlayerIdOrdered(playerId).map { it.toDto() }

    @Transactional
    fun healPet(playerId: Long, petId: Long, itemId: Short): PetDto {
        val pet = playerPetRepo.findByIdOrNull(petId)
            ?: throw IllegalArgumentException("Không tìm thấy sủng vật")
        if (pet.player.id != playerId) throw SecurityException("Không phải sủng vật của bạn")

        val playerItem = playerItemRepo.findByPlayerIdAndItemId(playerId, itemId)
            .orElseThrow { IllegalArgumentException("Không có vật phẩm này") }
        val item = playerItem.item
        if (item.itemType != "MEDICINE") throw IllegalArgumentException("Đây không phải thuốc")

        val healed = pet.copy(hp = minOf(pet.hpMax, pet.hp + item.effectVal))
        playerPetRepo.save(healed)

        if (playerItem.quantity <= 1) playerItemRepo.delete(playerItem)
        else playerItemRepo.save(playerItem.copy(quantity = playerItem.quantity - 1))

        return healed.toDto()
    }

    @Transactional
    fun evolvePet(playerId: Long, petId: Long): PetDto {
        val pet = playerPetRepo.findByIdOrNull(petId)
            ?: throw IllegalArgumentException("Không tìm thấy sủng vật")
        if (pet.player.id != playerId) throw SecurityException("Không phải sủng vật của bạn")

        val template = pet.template
        val evolveInto = template.evolveInto
            ?: throw IllegalStateException("Sủng vật này không tiến hóa được")
        val evolveLv = template.evolveLv?.toInt() ?: 999

        if (pet.level < evolveLv)
            throw IllegalStateException("Cần đạt cấp $evolveLv để tiến hóa (hiện tại: ${pet.level})")

        val newTemplate = petTemplateRepo.findByIdOrNull(evolveInto)
            ?: throw IllegalStateException("Template tiến hóa không tồn tại")

        val saved = playerPetRepo.save(
            PlayerPet(
                id = pet.id,
                player = pet.player,
                template = newTemplate,
                nickname = pet.nickname,
                level = pet.level,
                exp = pet.exp,
                hp = GameFormula.petHpMax(newTemplate, pet.level.toInt()),
                hpMax = GameFormula.petHpMax(newTemplate, pet.level.toInt()),
                atk = GameFormula.petAtk(newTemplate, pet.level.toInt()).toShort(),
                def = GameFormula.petDef(newTemplate, pet.level.toInt()).toShort(),
                spd = GameFormula.petSpd(newTemplate, pet.level.toInt()).toShort(),
                slot = pet.slot,
                loyalty = pet.loyalty,
                obtainedAt = pet.obtainedAt
            )
        )
        return saved.toDto()
    }

    @Transactional
    fun swapSlot(playerId: Long, petId: Long, newSlot: Short) {
        if (newSlot < 0 || newSlot > 5) throw IllegalArgumentException("Vị trí không hợp lệ (0-5)")
        val pet = playerPetRepo.findByIdOrNull(petId)
            ?: throw IllegalArgumentException("Không tìm thấy sủng vật")
        if (pet.player.id != playerId) throw SecurityException("Không phải sủng vật của bạn")

        playerPetRepo.findByPlayerIdAndSlot(playerId, newSlot).ifPresent { other ->
            playerPetRepo.save(other.copy(slot = pet.slot))
        }
        playerPetRepo.save(pet.copy(slot = newSlot))
    }
}

fun PlayerPet.toDto() = PetDto(
    id = id,
    templateId = template.id,
    name = template.name,
    nickname = nickname,
    element = template.element,
    spriteId = template.spriteId,
    level = level,
    exp = exp,
    expNext = GameFormula.expForLevel(level.toInt()),
    hp = hp,
    hpMax = hpMax,
    atk = atk,
    def = def,
    spd = spd,
    slot = slot,
    loyalty = loyalty,
    canEvolve = template.evolveInto != null && level >= (template.evolveLv?.toInt() ?: 999),
    evolveLv = template.evolveLv
)
