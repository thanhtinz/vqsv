package com.vqsv.config

import com.vqsv.entity.Account
import com.vqsv.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Ensures an admin account exists on startup, created from environment variables
 * ADMIN_USERNAME / ADMIN_PASSWORD. If the account already exists it is promoted to
 * ADMIN (password is NOT overwritten). Does nothing if the env vars are unset.
 */
@Configuration
class AdminBootstrap(
    @Value("\${vqsv.admin.username:}") private val adminUsername: String,
    @Value("\${vqsv.admin.password:}") private val adminPassword: String
) {
    private val log = LoggerFactory.getLogger(AdminBootstrap::class.java)

    @Bean
    fun ensureAdmin(accountRepo: AccountRepository, encoder: PasswordEncoder) = ApplicationRunner {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.info("[ADMIN] ADMIN_USERNAME/ADMIN_PASSWORD not set — skipping admin bootstrap")
            return@ApplicationRunner
        }
        val existing = accountRepo.findByUsername(adminUsername)
        if (existing.isPresent) {
            val acc = existing.get()
            if (acc.role != "ADMIN") {
                acc.role = "ADMIN"
                accountRepo.save(acc)
                log.info("[ADMIN] Promoted existing account '{}' to ADMIN", adminUsername)
            }
        } else {
            accountRepo.save(Account(
                username = adminUsername,
                password = encoder.encode(adminPassword),
                role = "ADMIN"
            ))
            log.info("[ADMIN] Created admin account '{}'", adminUsername)
        }
    }
}
