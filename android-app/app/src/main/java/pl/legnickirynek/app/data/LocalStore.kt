package pl.legnickirynek.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pl.legnickirynek.app.model.Listing
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
                    add(
                        Listing(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            price = item.getInt("price"),
                            location = item.getString("location"),
                            categoryId = item.getString("categoryId"),
                            description = item.getString("description"),
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
                    .put("isFavorite", listing.isFavorite)
            )
        }

        preferences(context)
            .edit()
            .putString(LISTINGS_KEY, array.toString())
            .apply()
    }

    fun clearListings(context: Context) {
        preferences(context)
            .edit()
            .remove(LISTINGS_KEY)
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
}
