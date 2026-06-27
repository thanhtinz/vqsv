package com.vqsv.config

import com.vqsv.dto.ChatRequest
import com.vqsv.dto.WsMessage
import com.vqsv.repository.PlayerRepository
import com.vqsv.util.JwtUtil
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}

@Service
class GameEventService(
    private val messaging: SimpMessagingTemplate,
    private val playerRepo: PlayerRepository
) {
    fun broadcastPlayerMove(mapId: Short, playerId: Long, name: String, x: Short, y: Short, level: Short) {
        messaging.convertAndSend(
            "/topic/map/$mapId",
            WsMessage("PLAYER_MOVE", mapOf(
                "playerId" to playerId, "name" to name, "posX" to x, "posY" to y, "level" to level
            ))
        )
    }

    fun broadcastPlayerJoin(mapId: Short, playerId: Long, name: String, x: Short, y: Short, level: Short) {
        messaging.convertAndSend(
            "/topic/map/$mapId",
            WsMessage("PLAYER_JOIN", mapOf(
                "playerId" to playerId, "name" to name, "posX" to x, "posY" to y, "level" to level
            ))
        )
    }

    fun broadcastPlayerLeave(mapId: Short, playerId: Long) {
        messaging.convertAndSend(
            "/topic/map/$mapId",
            WsMessage("PLAYER_LEAVE", mapOf("playerId" to playerId))
        )
    }

    fun sendToPlayer(playerId: Long, msg: WsMessage) {
        messaging.convertAndSendToUser(playerId.toString(), "/queue/events", msg)
    }

    fun broadcastChat(mapId: Short, senderName: String, text: String) {
        messaging.convertAndSend(
            "/topic/map/$mapId/chat",
            WsMessage("CHAT_MSG", mapOf("from" to senderName, "text" to text))
        )
    }

    @Scheduled(fixedDelay = 30_000)
    fun pingOnlinePlayers() {
        messaging.convertAndSend("/topic/system", WsMessage("PING", null))
    }
}

@Controller
class GameWsController(
    private val gameEventService: GameEventService,
    private val playerRepo: PlayerRepository,
    private val jwtUtil: JwtUtil
) {
    @MessageMapping("/chat")
    fun handleChat(
        @Payload req: ChatRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val token = headerAccessor.sessionAttributes?.get("token") as? String ?: return
        if (!jwtUtil.validateToken(token)) return
        val playerId = jwtUtil.getPlayerId(token)
        val player = playerRepo.findByIdOrNull(playerId) ?: return

        val cleanText = req.text.take(128)
        gameEventService.broadcastChat(player.mapId, player.name, cleanText)
    }
}
