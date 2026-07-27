package pl.legnickirynek.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.legnickirynek.app.model.ChatMessage
import pl.legnickirynek.app.model.Conversation

class MessageEntityTest {
    @Test
    fun `rozmowa zachowuje właściciela konta po mapowaniu`() {
        val conversation = Conversation(
            id = "conversation-1",
            accountId = "account-123",
            person = "Anna",
            listingId = "listing-1",
            listingTitle = "Rower miejski",
            lastMessage = "Czy oferta jest aktualna?",
            updatedAt = 1234L,
            unreadCount = 2
        )

        assertEquals(conversation, conversation.toEntity().toModel())
        assertEquals("account-123", conversation.toEntity().accountId)
    }

    @Test
    fun `wiadomość zachowuje wszystkie dane po mapowaniu`() {
        val message = ChatMessage(
            id = "message-1",
            conversationId = "conversation-1",
            senderName = "Jan",
            body = "Tak, oferta jest aktualna.",
            sentAt = 5678L,
            sentByCurrentUser = true,
            isRead = true
        )

        assertEquals(message, message.toEntity().toModel())
    }
}
