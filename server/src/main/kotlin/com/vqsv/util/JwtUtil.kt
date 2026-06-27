package com.vqsv.util

import io.jsonwebtoken.*
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

    fun generateToken(username: String, playerId: Long): String =
        Jwts.builder()
            .subject(username)
            .claim("pid", playerId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)

    fun getUsername(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject

    fun getPlayerId(token: String): Long =
        (Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload["pid"] as Number).toLong()
}
