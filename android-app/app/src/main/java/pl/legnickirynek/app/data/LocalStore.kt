package pl.legnickirynek.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus
import pl.legnickirynek.app.model.UserProfile

object LocalStore {
    private const val PREFERENCES_NAME = "legnicki_rynek_local_store"
    private const val LISTINGS_KEY = "listings"
    private const val PROFILE_KEY = "profile"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadListings(context: Context): List<Listing> {
        val raw = preferences(context).getString(LISTINGS_KEY, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val createdAt = item.optLong("createdAt", 0L)
                        .takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                    val status = runCatching {
                        ListingStatus.valueOf(
                            item.optString("status", ListingStatus.ACTIVE.name)
                        )
                    }.getOrDefault(ListingStatus.ACTIVE)

                    add(
                        Listing(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            price = item.getInt("price"),
                            location = item.getString("location"),
                            categoryId = item.getString("categoryId"),
                            description = item.getString("description"),
                            imageUris = item.optJSONArray("imageUris").toStringList(),
                            sellerName = item.optString("sellerName", "Użytkownik"),
                            createdAt = createdAt,
                            updatedAt = item.optLong("updatedAt", createdAt),
                            status = status,
                            isFavorite = item.optBoolean("isFavorite", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveListings(context: Context, listings: List<Listing>) {
        val array = JSONArray()
        listings.forEach { listing ->
            array.put(
                JSONObject()
                    .put("id", listing.id)
                    .put("title", listing.title)
                    .put("price", listing.price)
                    .put("location", listing.location)
                    .put("categoryId", listing.categoryId)
                    .put("description", listing.description)
                    .put("imageUris", JSONArray(listing.imageUris))
                    .put("sellerName", listing.sellerName)
                    .put("createdAt", listing.createdAt)
                    .put("updatedAt", listing.updatedAt)
                    .put("status", listing.status.name)
                    .put("isFavorite", listing.isFavorite)
            )
        }

        preferences(context)
            .edit()
            .putString(LISTINGS_KEY, array.toString())
            .apply()
    }

    fun loadProfile(context: Context): UserProfile {
        val raw = preferences(context).getString(PROFILE_KEY, null) ?: return UserProfile()

        return runCatching {
            val item = JSONObject(raw)
            UserProfile(
                name = item.optString("name"),
                email = item.optString("email"),
                loggedIn = item.optBoolean("loggedIn", false)
            )
        }.getOrDefault(UserProfile())
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val item = JSONObject()
            .put("name", profile.name)
            .put("email", profile.email)
            .put("loggedIn", profile.loggedIn)

        preferences(context)
            .edit()
            .putString(PROFILE_KEY, item.toString())
            .apply()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                optString(index)
                    .takeIf(String::isNotBlank)
                    ?.let(::add)
            }
        }
    }
}
