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
    private const val LISTING_MIGRATION_COMPLETE_KEY = "room_listing_migration_complete"
    private const val MESSAGE_INITIALIZATION_COMPLETE_KEY = "room_message_initialization_complete"

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

    fun isListingMigrationComplete(context: Context): Boolean =
        preferences(context).getBoolean(LISTING_MIGRATION_COMPLETE_KEY, false)

    fun markListingMigrationComplete(context: Context) {
        preferences(context)
            .edit()
            .putBoolean(LISTING_MIGRATION_COMPLETE_KEY, true)
            .apply()
    }

    fun isMessageInitializationComplete(context: Context): Boolean =
        preferences(context).getBoolean(MESSAGE_INITIALIZATION_COMPLETE_KEY, false)

    fun markMessageInitializationComplete(context: Context) {
        preferences(context)
            .edit()
            .putBoolean(MESSAGE_INITIALIZATION_COMPLETE_KEY, true)
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
