package com.fitbudget.app

import android.app.Application
import android.util.Log
import com.fitbudget.app.di.AppContainer
import com.fitbudget.app.notifications.NotificationHelper
import com.fitbudget.app.workers.WorkScheduler
import kotlinx.coroutines.launch

class FitBudgetApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.from(this)

        NotificationHelper.createDefaultChannels(this)

        container.applicationScope.launch {
            runCatching { container.initialise() }
                .onFailure { Log.e(TAG, "Startup initialisation failed", it) }
        }

        runCatching { WorkScheduler.ensureDailyMaintenance(this) }
            .onFailure { Log.e(TAG, "Unable to schedule daily maintenance", it) }
    }

    private companion object {
        const val TAG = "FitBudgetApp"
    }
}
