package com.fitbudget.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.di.AppContainer
import com.fitbudget.app.domain.model.ReminderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager. Posts the notification and immediately arms the next occurrence, which is
 * what makes the daily/interval repetition survive reboots, app updates and Doze.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val typeName = intent.getStringExtra(EXTRA_TYPE) ?: return
        val type = ReminderType.fromName(typeName)
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        scope.launch {
            try {
                val container = AppContainer.from(appContext)
                container.reminderRepository.ensureDefaults()
                val settings = container.settingsRepository.current()
                val reminder = container.reminderRepository.get(type) ?: ReminderEntity(type)

                if (settings.remindersEnabled && reminder.enabled) {
                    NotificationHelper.show(appContext, reminder)
                }
                // Arm the next occurrence even when this one was suppressed.
                container.reminderRepository.rescheduleOne(type)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to handle reminder $type", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        const val EXTRA_TYPE = "com.fitbudget.app.extra.REMINDER_TYPE"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
