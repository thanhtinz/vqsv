package com.vqsv.config

import com.vqsv.util.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtAuthFilter,
    // Comma-separated allowed origins for CORS. Defaults to "*" (convenient for
    // local dev). In production set CORS_ALLOWED_ORIGINS to your real domains,
    // e.g. https://play.example.com,https://admin.play.example.com
    @org.springframework.beans.factory.annotation.Value("\${vqsv.cors.allowed-origins:*}")
    private val corsAllowedOrigins: String
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/auth/**",
                    "/ws/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/api/web/auth/**",
                    "/api/web/public/**"
                ).permitAll()
                it.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "GM")
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = corsAllowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

@org.springframework.stereotype.Component
class JwtAuthFilter(private val jwtUtil: JwtUtil) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null && jwtUtil.validateToken(token)) {
            val accountId = jwtUtil.getAccountId(token)
            val auth = if (accountId != null) {
                // Web / admin token: principal = accountId, with role authority.
                val role = jwtUtil.getRole(token) ?: "PLAYER"
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    accountId, null,
                    listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_$role"))
                )
            } else {
                // Game token: principal = playerId.
                val playerId = jwtUtil.getPlayerId(token)
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    playerId, null, listOf()
                )
            }
            org.springframework.security.core.context.SecurityContextHolder.getContext().authentication = auth
        }
        chain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7) else null
    }
}
