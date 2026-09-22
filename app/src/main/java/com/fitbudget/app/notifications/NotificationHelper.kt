package com.fitbudget.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fitbudget.app.MainActivity
import com.fitbudget.app.R
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.domain.model.NotificationChannels
import com.fitbudget.app.domain.model.ReminderType

/**
 * Builds and posts the local reminder notifications.
 *
 * Android does not allow a channel's sound/vibration to change after creation, so each category
 * has up to three channel variants and the per-reminder toggles pick the right one. Variants are
 * created lazily, so a user who never turns sound off never sees an extra channel.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val NOTIFICATION_ID_BASE = 4200

    fun notificationId(type: ReminderType): Int = NOTIFICATION_ID_BASE + type.ordinal

    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /** Creates the four default channels so they are visible in system settings from day one. */
    fun createDefaultChannels(context: Context) {
        listOf(
            NotificationChannels.MEALS to (R.string.channel_meals_name to R.string.channel_meals_desc),
            NotificationChannels.WATER to (R.string.channel_water_name to R.string.channel_water_desc),
            NotificationChannels.ACTIVITY to (R.string.channel_activity_name to R.string.channel_activity_desc),
            NotificationChannels.GENERAL to (R.string.channel_general_name to R.string.channel_general_desc)
        ).forEach { (baseId, labels) ->
            ensureChannel(
                context = context,
                baseId = baseId,
                name = context.getString(labels.first),
                description = context.getString(labels.second),
                sound = true,
                vibrate = true
            )
        }
    }

    private fun channelName(baseId: String): Int = when (baseId) {
        NotificationChannels.MEALS -> R.string.channel_meals_name
        NotificationChannels.WATER -> R.string.channel_water_name
        NotificationChannels.ACTIVITY -> R.string.channel_activity_name
        else -> R.string.channel_general_name
    }

    private fun channelDescription(baseId: String): Int = when (baseId) {
        NotificationChannels.MEALS -> R.string.channel_meals_desc
        NotificationChannels.WATER -> R.string.channel_water_desc
        NotificationChannels.ACTIVITY -> R.string.channel_activity_desc
        else -> R.string.channel_general_desc
    }

    private fun variantId(baseId: String, sound: Boolean, vibrate: Boolean): String = when {
        sound -> baseId
        vibrate -> "${baseId}_vibrate"
        else -> "${baseId}_silent"
    }

    private fun variantSuffix(sound: Boolean, vibrate: Boolean): String = when {
        sound -> ""
        vibrate -> " (vibrate only)"
        else -> " (silent)"
    }

    private fun ensureChannel(
        context: Context,
        baseId: String,
        name: String,
        description: String,
        sound: Boolean,
        vibrate: Boolean
    ): String {
        val id = variantId(baseId, sound, vibrate)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return id
        if (manager.getNotificationChannel(id) != null) return id
        val channel = NotificationChannel(
            id,
            name + variantSuffix(sound, vibrate),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            this.description = description
            enableVibration(vibrate)
            if (!sound) setSound(null, null)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
        return id
    }

    private fun resolveChannel(context: Context, reminder: ReminderEntity): String = ensureChannel(
        context = context,
        baseId = reminder.type.channelId,
        name = context.getString(channelName(reminder.type.channelId)),
        description = context.getString(channelDescription(reminder.type.channelId)),
        sound = reminder.soundEnabled,
        vibrate = reminder.vibrationEnabled
    )

    /** Deep-link intent: tapping the notification opens the screen the reminder is about. */
    fun contentIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "${ACTION_OPEN_ROUTE}.$route"
            putExtra(EXTRA_ROUTE, route)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun show(context: Context, reminder: ReminderEntity) {
        if (!hasPermission(context)) {
            Log.i(TAG, "Notification permission not granted; skipping ${reminder.type}")
            return
        }
        val channelId = resolveChannel(context, reminder)
        val type = reminder.type
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(type.notificationTitle)
            .setContentText(type.notificationMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(type.notificationMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentIntent(context, type.route, notificationId(type)))
            .setDefaults(
                if (reminder.vibrationEnabled) Notification.DEFAULT_VIBRATE else 0
            )

        addActions(context, builder, type)

        try {
            NotificationManagerCompat.from(context).notify(notificationId(type), builder.build())
        } catch (security: SecurityException) {
            // Permission revoked between the check and the post.
            Log.w(TAG, "Unable to post notification for $type", security)
        }
    }

    private fun addActions(
        context: Context,
        builder: NotificationCompat.Builder,
        type: ReminderType
    ) {
        val mealType = type.mealType
        when {
            mealType != null -> builder.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "Mark eaten",
                    NotificationActionReceiver.completeMealIntent(context, mealType)
                ).build()
            )

            type == ReminderType.WATER -> {
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        0,
                        "+250 ml",
                        NotificationActionReceiver.addWaterIntent(context, 250)
                    ).build()
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        0,
                        "+500 ml",
                        NotificationActionReceiver.addWaterIntent(context, 500)
                    ).build()
                )
            }
        }
    }

    fun cancel(context: Context, type: ReminderType) {
        NotificationManagerCompat.from(context).cancel(notificationId(type))
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    const val EXTRA_ROUTE = "com.fitbudget.app.extra.ROUTE"
    const val ACTION_OPEN_ROUTE = "com.fitbudget.app.action.OPEN_ROUTE"
}
