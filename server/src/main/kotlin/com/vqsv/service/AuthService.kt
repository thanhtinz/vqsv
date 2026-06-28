package com.vqsv.service

import com.vqsv.dto.*
import com.vqsv.entity.Account
import com.vqsv.entity.Player
import com.vqsv.entity.PlayerBadge
import com.vqsv.entity.PlayerItem
import com.vqsv.entity.PlayerPet
import com.vqsv.repository.*
import com.vqsv.util.GameFormula
import com.vqsv.util.JwtUtil
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
    private val accountRepo: AccountRepository,
    private val playerRepo: PlayerRepository,
    private val badgeRepo: BadgeRepository,
    private val playerBadgeRepo: PlayerBadgeRepository,
    private val petTemplateRepo: PetTemplateRepository,
    private val playerPetRepo: PlayerPetRepository,
    private val itemRepo: ItemRepository,
    private val playerItemRepo: PlayerItemRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    private companion object {
        const val STARTER_SPECIES: Short = 1   // first species (db.mid id 0 -> pet_templates id 1)
        const val STARTER_LEVEL = 5
        const val BASIC_BALL_ITEM: Short = 1   // Tất Thường
        const val HEAL_ITEM: Short = 5         // Thuốc Hồi HP
    }
    @Transactional
    fun register(req: RegisterRequest): AuthResponse {
        if (accountRepo.existsByUsername(req.username))
            throw IllegalArgumentException("Tên tài khoản đã tồn tại")
        if (req.email != null && accountRepo.existsByEmail(req.email))
            throw IllegalArgumentException("Email đã được sử dụng")
        if (playerRepo.existsByName(req.playerName))
            throw IllegalArgumentException("Tên nhân vật đã tồn tại")

        val account = accountRepo.save(Account(
            username = req.username,
            password = passwordEncoder.encode(req.password),
            email = req.email
        ))

        val player = playerRepo.save(Player(
            account = account,
            name = req.playerName,
            kimTien = 500,
            huyChuong = 0
        ))

        badgeRepo.findById(1).ifPresent { badge ->
            playerBadgeRepo.save(PlayerBadge(player = player, badge = badge))
        }

        // Starter pet (slot 0) so the new player can battle & catch immediately.
        petTemplateRepo.findByIdOrNull(STARTER_SPECIES)?.let { tpl ->
            val lv = STARTER_LEVEL
            playerPetRepo.save(PlayerPet(
                player = player, template = tpl, level = lv.toShort(),
                hp = GameFormula.petHpMax(tpl, lv), hpMax = GameFormula.petHpMax(tpl, lv),
                atk = GameFormula.petAtk(tpl, lv).toShort(),
                def = GameFormula.petDef(tpl, lv).toShort(),
                spd = GameFormula.petSpd(tpl, lv).toShort(),
                slot = 0
            ))
        }
        // Starter items: catch balls + healing potions.
        itemRepo.findByIdOrNull(BASIC_BALL_ITEM)?.let { playerItemRepo.save(PlayerItem(player = player, item = it, quantity = 10)) }
        itemRepo.findByIdOrNull(HEAL_ITEM)?.let { playerItemRepo.save(PlayerItem(player = player, item = it, quantity = 5)) }

        val token = jwtUtil.generateToken(account.username, player.id)
        return AuthResponse(token = token, player = player.toDto())
    }

    @Transactional
    fun login(req: LoginRequest): AuthResponse {
        val account = accountRepo.findByUsername(req.username)
            .orElseThrow { IllegalArgumentException("Tài khoản không tồn tại") }

        if (account.isBanned)
            throw IllegalStateException("Tài khoản bị khóa: ${account.banReason}")

        if (!passwordEncoder.matches(req.password, account.password))
            throw IllegalArgumentException("Mật khẩu không đúng")

        val player = playerRepo.findByAccountId(account.id)
            .orElseThrow { IllegalStateException("Không tìm thấy nhân vật") }

        accountRepo.save(account.copy(lastLogin = Instant.now()))

        val token = jwtUtil.generateToken(account.username, player.id)
        return AuthResponse(token = token, player = player.toDto())
    }
}

fun Player.toDto() = PlayerDto(
    id = id,
    name = name,
    level = level,
    exp = exp,
    expNext = GameFormula.expForLevel(level.toInt()),
    kimTien = kimTien,
    huyChuong = huyChuong,
    mapId = mapId,
    posX = posX,
    posY = posY,
    hp = hp,
    hpMax = hpMax
)
