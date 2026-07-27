package pl.legnickirynek.app.data.local

import androidx.room.TypeConverter
import java.nio.charset.StandardCharsets
import java.util.Base64
import pl.legnickirynek.app.model.ListingStatus

class ListingConverters {
    @TypeConverter
    fun imageUrisToStorage(imageUris: List<String>): String = imageUris.joinToString(".") { uri ->
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(uri.toByteArray(StandardCharsets.UTF_8))
    }

    @TypeConverter
    fun storageToImageUris(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        return raw.split('.')
            .mapNotNull { encoded ->
                runCatching {
                    String(
                        Base64.getUrlDecoder().decode(encoded),
                        StandardCharsets.UTF_8
                    )
                }.getOrNull()
            }
            .filter(String::isNotBlank)
    }

    @TypeConverter
    fun statusToString(status: ListingStatus): String = status.name

    @TypeConverter
    fun stringToStatus(raw: String): ListingStatus = runCatching {
        ListingStatus.valueOf(raw)
    }.getOrDefault(ListingStatus.ACTIVE)
}
