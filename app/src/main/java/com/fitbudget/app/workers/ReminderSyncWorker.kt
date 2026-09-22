package com.fitbudget.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitbudget.app.di.AppContainer

/** Rebuilds every alarm from the database. Used after boot, app update or a clock change. */
class ReminderSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        AppContainer.from(applicationContext).reminderRepository.rescheduleAll()
        Result.success()
    } catch (error: Throwable) {
        Log.e(TAG, "Reminder sync failed", error)
        if (runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "fitbudget_reminder_sync"
        private const val TAG = "ReminderSyncWorker"
    }
}
