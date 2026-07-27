package pl.legnickirynek.app.feature.messages

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface MessageStorage {
    suspend fun readAll(): List<UserMessage>
    suspend fun replaceAll(messages: List<UserMessage>)
}

@Singleton
class MessageFileStore @Inject constructor(
    @ApplicationContext context: Context
) : MessageStorage {

    private val storageFile = File(context.filesDir, FILE_NAME)
    private val temporaryFile = File(context.filesDir, "$FILE_NAME.tmp")
    private val mutex = Mutex()

    override suspend fun readAll(): List<UserMessage> = withContext(Dispatchers.IO) {
        mutex.withLock { readMessages() }
    }

    override suspend fun replaceAll(messages: List<UserMessage>) = withContext(Dispatchers.IO) {
        mutex.withLock { writeMessages(messages) }
    }

    private fun readMessages(): List<UserMessage> {
        if (!storageFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(storageFile.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val conversationId = item.optString("conversationId")
                    val senderId = item.optString("senderId")
                    val recipientId = item.optString("recipientId")
                    val body = item.optString("body")
                    if (
                        id.isBlank() ||
                        conversationId.isBlank() ||
                        senderId.isBlank() ||
                        recipientId.isBlank() ||
                        body.isBlank()
                    ) {
                        continue
                    }
                    add(
                        UserMessage(
                            id = id,
                            conversationId = conversationId,
                            senderId = senderId,
                            recipientId = recipientId,
                            body = body,
                            listingId = item.optString("listingId").takeIf(String::isNotBlank),
                            sentAtEpochMillis = item.optLong("sentAtEpochMillis"),
                            readAtEpochMillis = item.optLong("readAtEpochMillis")
                                .takeIf { item.has("readAtEpochMillis") && !item.isNull("readAtEpochMillis") }
                        )
                    )
                }
            }.sortedBy(UserMessage::sentAtEpochMillis)
        }.getOrDefault(emptyList())
    }

    private fun writeMessages(messages: List<UserMessage>) {
        val array = JSONArray().apply {
            messages.sortedBy(UserMessage::sentAtEpochMillis).forEach { message ->
                put(
                    JSONObject()
                        .put("id", message.id)
                        .put("conversationId", message.conversationId)
                        .put("senderId", message.senderId)
                        .put("recipientId", message.recipientId)
                        .put("body", message.body)
                        .put("listingId", message.listingId ?: JSONObject.NULL)
                        .put("sentAtEpochMillis", message.sentAtEpochMillis)
                        .put("readAtEpochMillis", message.readAtEpochMillis ?: JSONObject.NULL)
                )
            }
        }
        temporaryFile.writeText(array.toString())
        if (storageFile.exists() && !storageFile.delete()) {
            temporaryFile.delete()
            error("Nie udało się zastąpić magazynu wiadomości.")
        }
        if (!temporaryFile.renameTo(storageFile)) {
            temporaryFile.delete()
            error("Nie udało się zapisać magazynu wiadomości.")
        }
    }

    private companion object {
        const val FILE_NAME = "user_messages.json"
    }
}
