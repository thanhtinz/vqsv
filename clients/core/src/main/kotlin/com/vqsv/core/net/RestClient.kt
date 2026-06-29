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

    data class ShopItem(
        val id: Int = 0,            // shop listing id
        val itemId: Int = 0,
        val itemName: String = "",
        val itemType: String = "",
        val effectVal: Int = 0,
        val iconId: Int = 0,
        val priceGold: Int? = null,
        val priceMedal: Int? = null,
        val description: String? = null
    )

    data class InventoryItem(
        val itemId: Int = 0,
        val name: String = "",
        val itemType: String = "",
        val effectVal: Int = 0,
        val iconId: Int = 0,
        val quantity: Int = 0,
        val description: String? = null
    )

    data class SkillInfo(
        val id: Int = 0,
        val name: String = "",
        val element: Int = 0,
        val power: Int = 0,
        val spCost: Int = 0,
        val requiredLevel: Int = 1,
        val description: String? = null
    )

    data class NpcInfo(
        val id: Int = 0,
        val name: String = "",
        val spriteId: Int = 0,
        val npcType: String = "",
        val posX: Int = 0,
        val posY: Int = 0,
        val enemyTemplateId: Int? = null
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

    fun getShop(cb: (List<ShopItem>?, String?) -> Unit) {
        get("$baseUrl/api/shop", null) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try {
                val type = object : TypeToken<List<ShopItem>>() {}.type
                cb(gson.fromJson(body, type), null)
            } catch (e: Exception) { cb(null, e.message) }
        }
    }

    fun getPetSkills(token: String, petId: Long, cb: (List<SkillInfo>?, String?) -> Unit) {
        get("$baseUrl/api/pets/$petId/skills", token) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try {
                val type = object : TypeToken<List<SkillInfo>>() {}.type
                cb(gson.fromJson(body, type), null)
            } catch (e: Exception) { cb(null, e.message) }
        }
    }

    fun getNpcs(token: String, mapId: Int, cb: (List<NpcInfo>?, String?) -> Unit) {
        get("$baseUrl/api/player/npcs/$mapId", token) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try {
                val type = object : TypeToken<List<NpcInfo>>() {}.type
                cb(gson.fromJson(body, type), null)
            } catch (e: Exception) { cb(null, e.message) }
        }
    }

    fun getInventory(token: String, cb: (List<InventoryItem>?, String?) -> Unit) {
        get("$baseUrl/api/shop/inventory", token) { body, err ->
            if (err != null) { cb(null, err); return@get }
            try {
                val type = object : TypeToken<List<InventoryItem>>() {}.type
                cb(gson.fromJson(body, type), null)
            } catch (e: Exception) { cb(null, e.message) }
        }
    }

    /** Buy a shop listing. Server expects {shopListingId, quantity}. */
    fun buyItem(token: String, shopListingId: Int, quantity: Int, cb: (Boolean, String?) -> Unit) {
        val body = gson.toJson(mapOf("shopListingId" to shopListingId, "quantity" to quantity))
        postRaw("$baseUrl/api/shop/buy", body, token) { _, err -> cb(err == null, err) }
    }

    fun healPet(token: String, petId: Long, itemId: Int, cb: (PetInfo?, String?) -> Unit) {
        postRaw("$baseUrl/api/pets/$petId/heal?itemId=$itemId", "{}", token) { s, err -> parse(s, err, PetInfo::class.java, cb) }
    }

    fun evolvePet(token: String, petId: Long, cb: (PetInfo?, String?) -> Unit) {
        postRaw("$baseUrl/api/pets/$petId/evolve", "{}", token) { s, err -> parse(s, err, PetInfo::class.java, cb) }
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
