package pl.legnickirynek.app.feature.notifications

import java.time.LocalTime
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {

    private val policy = NotificationPolicy()
    private val messageNotification = AppNotification(
        id = "message-1",
        kind = NotificationKind.MESSAGE,
        title = "Nowa wiadomość",
        body = "Masz nową wiadomość od użytkownika."
    )

    @Test
    fun globallyDisabledNotificationsAreRejected() {
        val result = policy.canPost(
            messageNotification,
            NotificationPreferences(enabled = false),
            LocalTime.NOON
        )

        assertTrue(result is NotificationPostResult.Disabled)
    }

    @Test
    fun disabledKindIsRejected() {
        val result = policy.canPost(
            messageNotification,
            NotificationPreferences(enabledKinds = setOf(NotificationKind.EVENT)),
            LocalTime.NOON
        )

        assertTrue(result is NotificationPostResult.Disabled)
    }

    @Test
    fun standardQuietHoursAreRespected() {
        val result = policy.canPost(
            messageNotification,
            NotificationPreferences(
                quietHoursStart = LocalTime.of(13, 0),
                quietHoursEnd = LocalTime.of(15, 0)
            ),
            LocalTime.of(14, 0)
        )

        assertTrue(result is NotificationPostResult.QuietHours)
    }

    @Test
    fun overnightQuietHoursAreRespected() {
        val preferences = NotificationPreferences(
            quietHoursStart = LocalTime.of(22, 0),
            quietHoursEnd = LocalTime.of(7, 0)
        )

        assertTrue(
            policy.canPost(messageNotification, preferences, LocalTime.of(23, 30))
                is NotificationPostResult.QuietHours
        )
        assertTrue(
            policy.canPost(messageNotification, preferences, LocalTime.of(6, 30))
                is NotificationPostResult.QuietHours
        )
        assertNull(policy.canPost(messageNotification, preferences, LocalTime.of(12, 0)))
    }
}
