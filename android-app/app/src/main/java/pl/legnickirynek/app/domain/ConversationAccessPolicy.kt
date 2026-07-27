package pl.legnickirynek.app.domain

import pl.legnickirynek.app.model.Conversation

object ConversationAccessPolicy {
    fun canAccess(conversation: Conversation, accountId: String): Boolean =
        accountId.isNotBlank() &&
            conversation.id.isNotBlank() &&
            conversation.accountId == accountId

    fun requireAccess(conversation: Conversation, accountId: String): Conversation {
        require(accountId.isNotBlank()) { "Identyfikator konta nie może być pusty." }
        require(conversation.id.isNotBlank()) { "Identyfikator rozmowy nie może być pusty." }
        require(conversation.accountId == accountId) {
            "Rozmowa nie należy do aktywnego konta."
        }
        return conversation
    }
}
