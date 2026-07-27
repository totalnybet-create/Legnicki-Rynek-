package pl.legnickirynek.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import pl.legnickirynek.app.model.ListingStatus

class ListingConverters {
    @TypeConverter
    fun imageUrisToJson(imageUris: List<String>): String = JSONArray(imageUris).toString()

    @TypeConverter
    fun jsonToImageUris(raw: String): List<String> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index)
                    .takeIf(String::isNotBlank)
                    ?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    @TypeConverter
    fun statusToString(status: ListingStatus): String = status.name

    @TypeConverter
    fun stringToStatus(raw: String): ListingStatus = runCatching {
        ListingStatus.valueOf(raw)
    }.getOrDefault(ListingStatus.ACTIVE)
}
