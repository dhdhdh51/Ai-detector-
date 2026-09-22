package com.fitbudget.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitbudget.app.di.AppContainer
import com.fitbudget.app.util.DateTimeUtils

/**
 * Runs a few times a day and performs the "new day" housekeeping:
 *  * creates today's goal snapshot (budget / water target / step goal),
 *  * generates today's meal plan if it does not exist yet,
 *  * re-arms every reminder so the schedule can never silently drift.
 *
 * All of it is idempotent, so running it more often than needed is harmless.
 */
class DailyMaintenanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val container = AppContainer.from(applicationContext)
        val profile = container.profileRepository.get()

        if (profile?.onboardingComplete == true) {
            val today = DateTimeUtils.todayEpochDay()
            container.dayRepository.ensureDay(today)
            container.dietRepository.ensurePlanForDay(today)
            container.settingsRepository.setLastRollOverDay(today)
        }
        container.reminderRepository.rescheduleAll()
        Result.success()
    } catch (error: Throwable) {
        Log.e(TAG, "Daily maintenance failed", error)
        // Transient database/alarm problems are worth one retry.
        if (runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "fitbudget_daily_maintenance"
        private const val TAG = "DailyMaintenance"
    }
}
