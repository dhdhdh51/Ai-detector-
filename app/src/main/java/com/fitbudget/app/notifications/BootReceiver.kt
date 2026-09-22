package com.fitbudget.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fitbudget.app.workers.WorkScheduler

/**
 * Alarms do not survive a reboot, an app update or a clock change, so we rebuild the whole
 * schedule from the database whenever one of those happens. The actual work runs in a WorkManager
 * job because a receiver must return quickly.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        Log.i(TAG, "Rebuilding reminder schedule after $action")
        runCatching {
            WorkScheduler.syncRemindersNow(context.applicationContext)
            WorkScheduler.ensureDailyMaintenance(context.applicationContext)
        }.onFailure { Log.e(TAG, "Unable to enqueue reminder sync", it) }
    }

    private companion object {
        const val TAG = "BootReceiver"
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
