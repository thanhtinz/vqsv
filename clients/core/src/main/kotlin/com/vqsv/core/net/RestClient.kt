package com.vqsv.core.net

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RestClient(private val baseUrl: String) {

    data class AuthResponse(val token: String, val player: PlayerInfo)

    data class PlayerInfo(
        val id: Any? = null,
        val name: String = "",
        val level: Int = 1,
        val exp: Int = 0,
        val expNext: Int = 100,
        val kimTien: Int = 0,
        val huyChuong: Int = 0,
        val mapId: Int = 0,
        val posX: Int = 0,
        val posY: Int = 0,
        val hp: Int = 100,
        val hpMax: Int = 100
    )

    data class PetInfo(
        val id: Any? = null,
        val name: String = "",
        val level: Int = 1,
        val element: String = "",
        val spriteId: Int = 0,
        val slot: Int = 0,
        val hpMax: Int = 100,
        val hp: Int = 100,
        val atk: Int = 10,
        val def: Int = 10,
        val spd: Int = 10
    )

    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun login(username: String, password: String, cb: (AuthResponse?, String?) -> Unit) {
        val body = gson.toJson(mapOf("username" to username, "password" to password))
        postRaw("$baseUrl/api/auth/login", body, null) { s, err -> parse(s, err, AuthResponse::class.java, cb) }
    }

    fun register(
        username: String,
        password: String,
        playerName: String,
        email: String? = null,
        cb: (AuthResponse?, String?) -> Unit
    ) {
        val map = mutableMapOf("username" to username, "password" to password, "playerName" to playerName)
        if (email != null) map["email"] = email
        val body = gson.toJson(map)
        postRaw("$baseUrl/api/auth/register", body, null) { s, err -> parse(s, err, AuthResponse::class.java, cb) }
    }

    fun getMyPlayer(token: String, cb: (PlayerInfo?, String?) -> Unit) {
        get("$baseUrl/api/player/me", token) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try { cb(gson.fromJson(body, PlayerInfo::class.java), null) }
            catch (e: Exception) { cb(null, e.message) }
        }
    }

    fun getMyPets(token: String, cb: (List<PetInfo>?, String?) -> Unit) {
        get("$baseUrl/api/pets", token) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try {
                val type = object : TypeToken<List<PetInfo>>() {}.type
                cb(gson.fromJson(body, type), null)
            } catch (e: Exception) { cb(null, e.message) }
        }
    }

    fun getShop(cb: (List<Any>?, String?) -> Unit) {
        get("$baseUrl/api/shop", null) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try {
                val type = object : TypeToken<List<Any>>() {}.type
                cb(gson.fromJson(body, type), null)
            } catch (e: Exception) { cb(null, e.message) }
        }
    }

    fun buyItem(token: String, itemId: Int, qty: Int, cb: (PlayerInfo?, String?) -> Unit) {
        val body = gson.toJson(mapOf("itemId" to itemId, "qty" to qty))
        postRaw("$baseUrl/api/shop/buy", body, token) { s, err -> parse(s, err, PlayerInfo::class.java, cb) }
    }

    /** Parse a raw body into [T] and invoke the typed callback. */
    private fun <T> parse(body: String?, err: String?, cls: Class<T>, cb: (T?, String?) -> Unit) {
        if (err != null) { cb(null, err); return }
        try { cb(gson.fromJson(body, cls), null) } catch (e: Exception) { cb(null, e.message) }
    }

    private fun postRaw(url: String, bodyJson: String, token: String?, cb: (String?, String?) -> Unit) {
        val reqBody = bodyJson.toRequestBody(JSON)
        val reqBuilder = Request.Builder().url(url).post(reqBody)
        if (token != null) reqBuilder.addHeader("Authorization", "Bearer $token")
        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null, e.message) }
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (!response.isSuccessful) { cb(null, "HTTP ${response.code}: $bodyStr"); return }
                cb(bodyStr, null)
            }
        })
    }

    private fun get(url: String, token: String?, cb: (String?, String?) -> Unit) {
        val reqBuilder = Request.Builder().url(url).get()
        if (token != null) reqBuilder.addHeader("Authorization", "Bearer $token")
        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null, e.message) }
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (!response.isSuccessful) { cb(null, "HTTP ${response.code}: $bodyStr"); return }
                cb(bodyStr, null)
            }
        })
    }
}
