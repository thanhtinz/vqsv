# VQSV Android Client

## Stack
- Kotlin + Jetpack Compose (Android 8.0+)
- Retrofit2 + OkHttp cho REST API
- STOMP over WebSocket cho real-time
- Coroutines + Flow cho async

## Kết nối server
```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("ua.naiksoftware:stomp-protocolandroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
}
```

## API Service
```kotlin
interface VqsvApi {
    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @GET("api/player/map")
    suspend fun getMapState(@Header("Authorization") token: String): MapStateDto

    @POST("api/player/move")
    suspend fun move(
        @Header("Authorization") token: String,
        @Body req: MoveRequest
    ): Map<String, Any>

    @GET("api/pets")
    suspend fun getPets(@Header("Authorization") token: String): List<PetDto>

    @POST("api/battle/action")
    suspend fun battleAction(
        @Header("Authorization") token: String,
        @Body req: BattleAction
    ): BattleTurnResult

    @GET("api/shop")
    suspend fun getShop(): List<ShopItemDto>

    @POST("api/shop/buy")
    suspend fun buy(
        @Header("Authorization") token: String,
        @Body req: BuyRequest
    ): InventoryItemDto
}
```

## WebSocket (STOMP)
```kotlin
val stompClient = Stomp.over(
    Stomp.ConnectionProvider.OKHTTP,
    "ws://YOUR_SERVER:8080/ws/websocket"
)
stompClient.connect()
stompClient.topic("/topic/map/$mapId").subscribe { msg ->
    val event = Gson().fromJson(msg.payload, WsMessage::class.java)
    when (event.type) {
        "PLAYER_MOVE"  -> updatePlayerPosition(event.payload)
        "WILD_ENCOUNTER" -> showBattleScreen(event.payload)
        "CHAT_MSG"     -> appendChatMessage(event.payload)
    }
}
```

## J2ME Mode (TCP binary - tùy chọn)
Để giữ client J2ME gốc hoạt động: dùng đúng port 9090, binary protocol
đã mô tả trong `TcpGateway.kt` (Op codes 0x01-0x07).
