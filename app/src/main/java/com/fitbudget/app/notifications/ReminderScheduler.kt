package com.fitbudget.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.util.DateTimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules the local reminder alarms.
 *
 * * One-shot daily reminders use an exact alarm for the next matching day, and the receiver
 *   re-arms the following one after it fires. That keeps exact timing without relying on
 *   `setRepeating` (which is inexact on modern Android).
 * * Interval reminders (water) fire on a fixed cadence between the user's wake and sleep times.
 * * If the app is not allowed to schedule exact alarms, it degrades gracefully to an inexact
 *   alarm instead of crashing or silently doing nothing.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean = when {
        alarmManager == null -> false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
        else -> true
    }

    fun scheduleAll(
        reminders: List<ReminderEntity>,
        wakeMinutes: Int,
        sleepMinutes: Int,
        masterEnabled: Boolean
    ) {
        reminders.forEach { reminder ->
            if (masterEnabled && reminder.enabled) {
                schedule(reminder, wakeMinutes, sleepMinutes)
            } else {
                cancel(reminder.type)
            }
        }
    }

    fun schedule(reminder: ReminderEntity, wakeMinutes: Int, sleepMinutes: Int) {
        val manager = alarmManager ?: return
        val triggerAt = nextTriggerMillis(reminder, wakeMinutes, sleepMinutes) ?: run {
            cancel(reminder.type)
            return
        }
        val pendingIntent = pendingIntent(reminder.type)
        try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                // Inexact but still delivered; the user sees a hint in Reminder settings.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (security: SecurityException) {
            Log.w(TAG, "Exact alarm denied for ${reminder.type}, falling back", security)
            runCatching {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancel(type: ReminderType) {
        alarmManager?.cancel(pendingIntent(type))
    }

    fun cancelAll() = ReminderType.entries.forEach(::cancel)

    private fun pendingIntent(type: ReminderType): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "${ACTION_FIRE}.${type.name}"
            putExtra(ReminderReceiver.EXTRA_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next fire time in epoch millis, or null when the reminder can never fire (no days chosen). */
    fun nextTriggerMillis(
        reminder: ReminderEntity,
        wakeMinutes: Int,
        sleepMinutes: Int,
        from: LocalDateTime = LocalDateTime.now()
    ): Long? {
        val next = nextTriggerDateTime(reminder, wakeMinutes, sleepMinutes, from) ?: return null
        return DateTimeUtils.toMillis(next)
    }

    fun nextTriggerDateTime(
        reminder: ReminderEntity,
        wakeMinutes: Int,
        sleepMinutes: Int,
        from: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        if (reminder.daysMask and DateTimeUtils.ALL_DAYS_MASK == 0) return null

        var date: LocalDate = from.toLocalDate()
        var daysChecked = 0
        while (daysChecked <= 8) {
            if (DateTimeUtils.maskContains(reminder.daysMask, date.dayOfWeek)) {
                val slots = slotsFor(reminder, wakeMinutes, sleepMinutes)
                val candidate = slots
                    .map { LocalDateTime.of(date, it) }
                    .firstOrNull { it.isAfter(from) }
                if (candidate != null) return candidate
            }
            date = date.plusDays(1)
            daysChecked += 1
        }
        return null
    }

    /** All times of day this reminder can fire, ascending. */
    private fun slotsFor(
        reminder: ReminderEntity,
        wakeMinutes: Int,
        sleepMinutes: Int
    ): List<LocalTime> {
        if (!reminder.type.isInterval || reminder.intervalMinutes <= 0) {
            return listOf(
                LocalTime.of(reminder.hour.coerceIn(0, 23), reminder.minute.coerceIn(0, 59))
            )
        }

        val interval = reminder.intervalMinutes.coerceIn(15, 720)
        val windowStart = minOf(wakeMinutes.coerceIn(0, 1439), 23 * 60)
        // A sleep time before the wake time means "after midnight"; clamp to end of day.
        val windowEnd = if (sleepMinutes <= windowStart) 23 * 60 + 59 else sleepMinutes.coerceAtMost(1439)
        val firstSlot = maxOf(reminder.minutesOfDay, windowStart)

        val slots = mutableListOf<LocalTime>()
        var minute = firstSlot
        var guard = 0
        while (minute <= windowEnd && guard++ < 96) {
            slots += LocalTime.of(minute / 60, minute % 60)
            minute += interval
        }
        if (slots.isEmpty()) {
            slots += LocalTime.of(windowStart / 60, windowStart % 60)
        }
        return slots
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val REQUEST_CODE_BASE = 7100
        const val ACTION_FIRE = "com.fitbudget.app.action.FIRE_REMINDER"
    }
}
