package pl.legnickirynek.app.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

interface RemoteListingService {
    val isConfigured: Boolean
    suspend fun fetchListings(): List<Listing>
    suspend fun upsertListing(listing: Listing)
    suspend fun deleteListing(id: String)
}

class RestRemoteListingService(
    baseUrl: String,
    private val bearerToken: String,
    private val httpClient: JsonHttpClient,
    private val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.STRICT)
        .create()
) : RemoteListingService {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    override val isConfigured: Boolean = isAllowedBaseUrl(normalizedBaseUrl)

    override suspend fun fetchListings(): List<Listing> {
        checkConfigured()
        val response = httpClient.request(
            method = "GET",
            url = "$normalizedBaseUrl/listings",
            bearerToken = bearerToken
        )
        return ListingJsonCodec.decodeList(response.body)
    }

    override suspend fun upsertListing(listing: Listing) {
        checkConfigured()
        httpClient.request(
            method = "PUT",
            url = "$normalizedBaseUrl/listings/${encodePathSegment(listing.id)}",
            body = ListingJsonCodec.encode(listing, gson),
            bearerToken = bearerToken
        )
    }

    override suspend fun deleteListing(id: String) {
        checkConfigured()
        require(id.isNotBlank()) { "Identyfikator ogłoszenia nie może być pusty." }
        httpClient.request(
            method = "DELETE",
            url = "$normalizedBaseUrl/listings/${encodePathSegment(id)}",
            bearerToken = bearerToken
        )
    }

    private fun checkConfigured() {
        check(isConfigured) {
            "API ogłoszeń nie jest skonfigurowane lub używa niedozwolonego adresu."
        }
    }

    companion object {
        fun isAllowedBaseUrl(value: String): Boolean {
            if (value.isBlank()) return false
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            val host = uri.host.orEmpty().lowercase()
            if (uri.scheme.equals("https", ignoreCase = true) && host.isNotBlank()) return true

            return uri.scheme.equals("http", ignoreCase = true) &&
                host in setOf("localhost", "127.0.0.1", "10.0.2.2")
        }

        private fun encodePathSegment(value: String): String = URLEncoder.encode(
            value,
            StandardCharsets.UTF_8.name()
        ).replace("+", "%20")
    }
}

object ListingJsonCodec {
    fun decodeList(json: String): List<Listing> {
        if (json.isBlank()) return emptyList()
        val root = JsonParser.parseString(json)
        val array = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> root.asJsonObject.get("listings")
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
            else -> null
        } ?: throw IllegalArgumentException("Odpowiedź API nie zawiera listy ogłoszeń.")

        return array.mapNotNull { element ->
            runCatching { decodeListing(element.asJsonObject) }.getOrNull()
        }.distinctBy(Listing::id)
    }

    fun encode(listing: Listing, gson: Gson = Gson()): String {
        require(listing.id.isNotBlank()) { "Identyfikator ogłoszenia nie może być pusty." }
        val json = JsonObject().apply {
            addProperty("apiVersion", 1)
            addProperty("id", listing.id)
            addProperty("title", listing.title)
            addProperty("price", listing.price)
            addProperty("location", listing.location)
            addProperty("categoryId", listing.categoryId)
            addProperty("description", listing.description)
            add("imageUrls", JsonArray().apply {
                listing.imageUris
                    .filter(::isRemoteImageUrl)
                    .distinct()
                    .take(12)
                    .forEach { imageUrl -> add(imageUrl) }
            })
            addProperty("ownerId", listing.ownerId)
            addProperty("sellerName", listing.sellerName)
            addProperty("createdAt", listing.createdAt)
            addProperty("updatedAt", listing.updatedAt)
            addProperty("status", listing.status.name)
            listing.latitude?.takeIf { it in -90.0..90.0 }?.let {
                addProperty("latitude", it)
            }
            listing.longitude?.takeIf { it in -180.0..180.0 }?.let {
                addProperty("longitude", it)
            }
        }
        return gson.toJson(json)
    }

    private fun decodeListing(json: JsonObject): Listing {
        val id = json.requiredString("id", maxLength = 160)
        val title = json.requiredString("title", maxLength = 140)
        val price = json.intOrDefault("price", 0).coerceIn(0, 100_000_000)
        val location = json.requiredString("location", maxLength = 180)
        val categoryId = json.requiredString("categoryId", maxLength = 80)
        val description = json.requiredString("description", maxLength = 5_000)
        val createdAt = json.longOrDefault("createdAt", System.currentTimeMillis())
            .coerceAtLeast(1L)
        val updatedAt = json.longOrDefault("updatedAt", createdAt)
            .coerceAtLeast(createdAt)
        val status = runCatching {
            ListingStatus.valueOf(json.stringOrNull("status").orEmpty().uppercase())
        }.getOrDefault(ListingStatus.ACTIVE)
        val latitude = json.doubleOrNull("latitude")?.takeIf { it in -90.0..90.0 }
        val longitude = json.doubleOrNull("longitude")?.takeIf { it in -180.0..180.0 }
        val imageArray = sequenceOf("imageUrls", "imageUris")
            .mapNotNull { key ->
                json.get(key)
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray
            }
            .firstOrNull()
        val imageUrls = imageArray
            ?.mapNotNull { item -> item.takeIf { it.isJsonPrimitive }?.asString }
            ?.filter(::isRemoteImageUrl)
            ?.distinct()
            ?.take(12)
            .orEmpty()

        return Listing(
            id = id,
            title = title,
            price = price,
            location = location,
            categoryId = categoryId,
            description = description,
            imageUris = imageUrls,
            ownerId = json.stringOrNull("ownerId").orEmpty().take(160),
            sellerName = json.stringOrNull("sellerName").orEmpty().take(120)
                .ifBlank { "Użytkownik" },
            createdAt = createdAt,
            updatedAt = updatedAt,
            status = status,
            isFavorite = false,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun JsonObject.requiredString(key: String, maxLength: Int): String =
        stringOrNull(key)
            ?.trim()
            ?.take(maxLength)
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Brak wymaganego pola $key.")

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString

    private fun JsonObject.intOrDefault(key: String, default: Int): Int =
        runCatching { get(key)?.asInt }.getOrNull() ?: default

    private fun JsonObject.longOrDefault(key: String, default: Long): Long =
        runCatching { get(key)?.asLong }.getOrNull() ?: default

    private fun JsonObject.doubleOrNull(key: String): Double? =
        runCatching { get(key)?.asDouble }
            .getOrNull()
            ?.takeIf { it.isFinite() }

    private fun isRemoteImageUrl(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)
}
