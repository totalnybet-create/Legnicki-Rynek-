package pl.legnickirynek.app.feature.messages

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingServiceTest {

    @Test
    fun sendReadAndDeleteConversationFormCompleteCycle() = runTest {
        val storage = FakeMessageStorage()
        var now = 1_000L
        var nextId = 0
        val service = MessagingService(
            storage = storage,
            validator = MessageValidator(),
            clock = { now },
            idFactory = { "message-${++nextId}" }
        )
        val command = SendMessageCommand(
            senderId = "user-a",
            recipientId = "user-b",
            body = "Czy ogłoszenie jest aktualne?",
            listingId = "listing-1"
        )

        val sent = service.send(command)
        assertTrue(sent is MessageOperationResult.Success)
        assertEquals(1, storage.messages.size)

        val duplicate = service.send(command)
        assertTrue(duplicate is MessageOperationResult.Success)
        assertEquals(1, storage.messages.size)
        assertEquals(
            (sent as MessageOperationResult.Success).message.id,
            (duplicate as MessageOperationResult.Success).message.id
        )

        val beforeRead = service.conversations("user-b")
        assertEquals(1, beforeRead.size)
        assertEquals(1, beforeRead.single().unreadCount)

        now = 2_000L
        assertTrue(service.markConversationRead(beforeRead.single().conversationId, "user-b"))
        assertEquals(0, service.conversations("user-b").single().unreadCount)

        assertTrue(service.deleteConversation(beforeRead.single().conversationId, "user-a"))
        assertTrue(storage.messages.isEmpty())
    }

    @Test
    fun invalidMessageIsRejectedWithoutStorageWrite() = runTest {
        val storage = FakeMessageStorage()
        val service = MessagingService(
            storage = storage,
            validator = MessageValidator(),
            clock = { 1_000L },
            idFactory = { "message-1" }
        )

        val result = service.send(
            SendMessageCommand(
                senderId = "same-user",
                recipientId = "same-user",
                body = "   "
            )
        )

        assertTrue(result is MessageOperationResult.ValidationFailed)
        assertTrue(storage.messages.isEmpty())
    }

    @Test
    fun outsiderCannotReadOrDeleteConversation() = runTest {
        val storage = FakeMessageStorage()
        val service = MessagingService(
            storage = storage,
            validator = MessageValidator(),
            clock = { 1_000L },
            idFactory = { "message-1" }
        )
        val sent = service.send(
            SendMessageCommand(
                senderId = "user-a",
                recipientId = "user-b",
                body = "Dzień dobry"
            )
        ) as MessageOperationResult.Success

        assertTrue(service.thread(sent.message.conversationId, "outsider").isEmpty())
        assertFalse(service.deleteConversation(sent.message.conversationId, "outsider"))
        assertEquals(1, storage.messages.size)
    }

    private class FakeMessageStorage : MessageStorage {
        var messages: List<UserMessage> = emptyList()

        override suspend fun readAll(): List<UserMessage> = messages

        override suspend fun replaceAll(messages: List<UserMessage>) {
            this.messages = messages
        }
    }
}
