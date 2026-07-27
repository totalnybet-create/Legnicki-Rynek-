package pl.legnickirynek.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Conversation

class ConversationAccessPolicyTest {
    private val conversation = Conversation(
        id = "conversation-1",
        accountId = "account-a",
        person = "Anna",
        listingId = "listing-1",
        listingTitle = "Rower miejski",
        lastMessage = "Czy oferta jest aktualna?",
        updatedAt = 1_000L
    )

    @Test
    fun `właściciel konta ma dostęp do rozmowy`() {
        assertTrue(ConversationAccessPolicy.canAccess(conversation, "account-a"))
        assertSame(conversation, ConversationAccessPolicy.requireAccess(conversation, "account-a"))
    }

    @Test
    fun `inne konto nie ma dostępu do rozmowy`() {
        assertFalse(ConversationAccessPolicy.canAccess(conversation, "account-b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `próba użycia rozmowy innego konta jest odrzucana`() {
        ConversationAccessPolicy.requireAccess(conversation, "account-b")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `pusty identyfikator konta jest odrzucany`() {
        ConversationAccessPolicy.requireAccess(conversation, "")
    }
}
