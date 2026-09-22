package com.fitbudget.app.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    /** Keeps the day snapshot, meal plan and alarms fresh without a foreground service. */
    fun ensureDailyMaintenance(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyMaintenanceWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun syncRemindersNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ReminderSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(DailyMaintenanceWorker.UNIQUE_NAME)
            cancelUniqueWork(ReminderSyncWorker.UNIQUE_NAME)
        }
    }
}
