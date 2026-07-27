package pl.legnickirynek.app.feature.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import pl.legnickirynek.app.MainActivity

enum class NotificationKind {
    MESSAGE,
    LISTING,
    FAVORITE,
    WEATHER_ALERT,
    NEWS,
    EVENT
}

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val referenceId: String? = null
)

data class NotificationPreferences(
    val enabled: Boolean = true,
    val enabledKinds: Set<NotificationKind> = NotificationKind.entries.toSet(),
    val quietHoursStart: LocalTime? = null,
    val quietHoursEnd: LocalTime? = null
)

sealed interface NotificationPostResult {
    data object Posted : NotificationPostResult
    data object Disabled : NotificationPostResult
    data object QuietHours : NotificationPostResult
    data object PermissionRequired : NotificationPostResult
    data class Failure(val cause: Throwable) : NotificationPostResult
}

class NotificationPolicy @Inject constructor() {

    fun canPost(
        notification: AppNotification,
        preferences: NotificationPreferences,
        currentTime: LocalTime = LocalTime.now()
    ): NotificationPostResult? {
        if (!preferences.enabled || notification.kind !in preferences.enabledKinds) {
            return NotificationPostResult.Disabled
        }
        val start = preferences.quietHoursStart
        val end = preferences.quietHoursEnd
        if (start != null && end != null && isWithinQuietHours(currentTime, start, end)) {
            return NotificationPostResult.QuietHours
        }
        return null
    }

    private fun isWithinQuietHours(time: LocalTime, start: LocalTime, end: LocalTime): Boolean =
        if (start <= end) {
            time >= start && time < end
        } else {
            time >= start || time < end
        }
}

@Singleton
class LocalNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val policy: NotificationPolicy
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    fun post(
        notification: AppNotification,
        preferences: NotificationPreferences = NotificationPreferences(),
        currentTime: LocalTime = LocalTime.now()
    ): NotificationPostResult {
        policy.canPost(notification, preferences, currentTime)?.let { return it }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return NotificationPostResult.PermissionRequired
        }

        return runCatching {
            ensureChannel(notification.kind)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NOTIFICATION_KIND, notification.kind.name)
                putExtra(EXTRA_REFERENCE_ID, notification.referenceId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val channelId = channelId(notification.kind)
            val systemNotification = Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notification.title.trim())
                .setContentText(notification.body.trim())
                .setStyle(Notification.BigTextStyle().bigText(notification.body.trim()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(category(notification.kind))
                .build()
            notificationManager.notify(notification.id.hashCode(), systemNotification)
            NotificationPostResult.Posted
        }.getOrElse(NotificationPostResult::Failure)
    }

    fun cancel(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
    }

    private fun ensureChannel(kind: NotificationKind) {
        val channel = NotificationChannel(
            channelId(kind),
            channelName(kind),
            importance(kind)
        ).apply {
            description = channelDescription(kind)
            enableVibration(kind == NotificationKind.MESSAGE || kind == NotificationKind.WEATHER_ALERT)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun channelId(kind: NotificationKind): String = when (kind) {
        NotificationKind.MESSAGE -> "messages"
        NotificationKind.LISTING -> "listings"
        NotificationKind.FAVORITE -> "favorites"
        NotificationKind.WEATHER_ALERT -> "weather_alerts"
        NotificationKind.NEWS -> "local_news"
        NotificationKind.EVENT -> "local_events"
    }

    private fun channelName(kind: NotificationKind): String = when (kind) {
        NotificationKind.MESSAGE -> "Wiadomości"
        NotificationKind.LISTING -> "Ogłoszenia"
        NotificationKind.FAVORITE -> "Ulubione"
        NotificationKind.WEATHER_ALERT -> "Ostrzeżenia pogodowe"
        NotificationKind.NEWS -> "Aktualności lokalne"
        NotificationKind.EVENT -> "Wydarzenia lokalne"
    }

    private fun channelDescription(kind: NotificationKind): String = when (kind) {
        NotificationKind.MESSAGE -> "Nowe wiadomości od użytkowników."
        NotificationKind.LISTING -> "Zmiany i odpowiedzi dotyczące ogłoszeń."
        NotificationKind.FAVORITE -> "Zmiany w obserwowanych ogłoszeniach."
        NotificationKind.WEATHER_ALERT -> "Ważne informacje pogodowe dla Legnicy."
        NotificationKind.NEWS -> "Nowe aktualności z lokalnych źródeł."
        NotificationKind.EVENT -> "Przypomnienia o lokalnych wydarzeniach."
    }

    private fun importance(kind: NotificationKind): Int = when (kind) {
        NotificationKind.WEATHER_ALERT -> NotificationManager.IMPORTANCE_HIGH
        NotificationKind.MESSAGE -> NotificationManager.IMPORTANCE_DEFAULT
        else -> NotificationManager.IMPORTANCE_LOW
    }

    private fun category(kind: NotificationKind): String = when (kind) {
        NotificationKind.MESSAGE -> Notification.CATEGORY_MESSAGE
        NotificationKind.WEATHER_ALERT -> Notification.CATEGORY_ALARM
        NotificationKind.EVENT -> Notification.CATEGORY_EVENT
        else -> Notification.CATEGORY_STATUS
    }

    companion object {
        const val EXTRA_NOTIFICATION_KIND = "notification_kind"
        const val EXTRA_REFERENCE_ID = "notification_reference_id"
    }
}
