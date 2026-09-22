package com.fitbudget.app.di

import android.content.Context
import com.fitbudget.app.data.database.FitBudgetDatabase
import com.fitbudget.app.data.repository.BackupRepository
import com.fitbudget.app.data.repository.BudgetRepository
import com.fitbudget.app.data.repository.DayRepository
import com.fitbudget.app.data.repository.DietRepository
import com.fitbudget.app.data.repository.FoodRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.ReminderRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.data.repository.StepRepository
import com.fitbudget.app.data.repository.WaterRepository
import com.fitbudget.app.data.repository.WeightRepository
import com.fitbudget.app.data.repository.WorkoutRepository
import com.fitbudget.app.data.sensors.StepSensorManager
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container.
 *
 * A hand-rolled container keeps the build free of annotation-processor DI while still giving a
 * single, testable place where every dependency is wired. Reachable from Activities, ViewModels,
 * BroadcastReceivers and Workers through [from].
 */
class AppContainer private constructor(private val appContext: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: FitBudgetDatabase by lazy { FitBudgetDatabase.get(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val profileRepository: ProfileRepository by lazy { ProfileRepository(database.profileDao()) }

    val foodRepository: FoodRepository by lazy { FoodRepository(database.foodDao()) }

    val dayRepository: DayRepository by lazy {
        DayRepository(database.dayMetaDao(), profileRepository, settingsRepository)
    }

    val dietRepository: DietRepository by lazy {
        DietRepository(
            mealDao = database.mealDao(),
            foodRepository = foodRepository,
            profileRepository = profileRepository,
            dayRepository = dayRepository,
            settingsRepository = settingsRepository
        )
    }

    val waterRepository: WaterRepository by lazy {
        WaterRepository(database.waterDao(), dayRepository)
    }

    val weightRepository: WeightRepository by lazy {
        WeightRepository(database.weightDao(), profileRepository)
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), dayRepository)
    }

    val stepRepository: StepRepository by lazy {
        StepRepository(database.stepDao(), settingsRepository, dayRepository)
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.expenseDao(), dayRepository)
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(
            mealDao = database.mealDao(),
            waterDao = database.waterDao(),
            stepDao = database.stepDao(),
            workoutDao = database.workoutDao(),
            weightDao = database.weightDao(),
            expenseDao = database.expenseDao(),
            dayMetaDao = database.dayMetaDao(),
            profileRepository = profileRepository,
            settingsRepository = settingsRepository
        )
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(
            reminderDao = database.reminderDao(),
            profileRepository = profileRepository,
            settingsRepository = settingsRepository,
            scheduler = reminderScheduler
        )
    }

    val stepSensorManager: StepSensorManager by lazy {
        StepSensorManager(appContext, stepRepository, applicationScope)
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            context = appContext,
            database = database,
            settingsRepository = settingsRepository,
            statsRepository = statsRepository,
            foodRepository = foodRepository,
            reminderRepository = reminderRepository
        )
    }

    /**
     * One-time startup work: seed the food database, install default reminders, make sure a
     * profile row exists and today's plan is ready. Safe to call repeatedly.
     */
    suspend fun initialise() {
        profileRepository.ensureExists()
        foodRepository.seedIfEmpty()
        reminderRepository.ensureDefaults()
        val profile = profileRepository.get()
        if (profile?.onboardingComplete == true) {
            dietRepository.ensurePlanForDay()
            reminderRepository.rescheduleAll()
        }
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun from(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
