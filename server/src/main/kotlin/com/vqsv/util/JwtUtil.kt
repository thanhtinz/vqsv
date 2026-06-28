package com.vqsv.util

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${vqsv.jwt.secret}") private val secret: String,
    @Value("\${vqsv.jwt.expiration-ms}") private val expirationMs: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    // Game token (J2ME / game clients): principal = playerId.
    fun generateToken(username: String, playerId: Long): String =
        Jwts.builder()
            .subject(username)
            .claim("pid", playerId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    // Web token (website / admin): principal = accountId, carries role.
    fun generateWebToken(username: String, accountId: Long, role: String): String =
        Jwts.builder()
            .subject(username)
            .claim("aid", accountId)
            .claim("role", role)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)

    private fun claims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    fun getUsername(token: String): String = claims(token).subject

    fun getPlayerId(token: String): Long = (claims(token)["pid"] as Number).toLong()

    fun getAccountId(token: String): Long? = (claims(token)["aid"] as? Number)?.toLong()

    fun getRole(token: String): String? = claims(token)["role"] as? String
}
