package com.fitbudget.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitbudget.app.di.AppContainer
import com.fitbudget.app.ui.screens.budget.BudgetViewModel
import com.fitbudget.app.ui.screens.diet.DietViewModel
import com.fitbudget.app.ui.screens.foods.FoodsViewModel
import com.fitbudget.app.ui.screens.home.HomeViewModel
import com.fitbudget.app.ui.screens.onboarding.OnboardingViewModel
import com.fitbudget.app.ui.screens.profile.ProfileViewModel
import com.fitbudget.app.ui.screens.progress.ProgressViewModel
import com.fitbudget.app.ui.screens.reminders.RemindersViewModel
import com.fitbudget.app.ui.screens.report.MonthlyReportViewModel
import com.fitbudget.app.ui.screens.root.RootViewModel
import com.fitbudget.app.ui.screens.settings.SettingsViewModel
import com.fitbudget.app.ui.screens.steps.StepsViewModel
import com.fitbudget.app.ui.screens.water.WaterViewModel
import com.fitbudget.app.ui.screens.weight.WeightViewModel
import com.fitbudget.app.ui.screens.workout.WorkoutSessionViewModel
import com.fitbudget.app.ui.screens.workout.WorkoutViewModel

/**
 * Single factory that wires every ViewModel from the [AppContainer]. Keeps constructor injection
 * explicit without pulling in an annotation-processor DI framework.
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(RootViewModel::class.java) -> RootViewModel(
                profileRepository = container.profileRepository,
                settingsRepository = container.settingsRepository
            )

            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(
                profileRepository = container.profileRepository,
                weightRepository = container.weightRepository,
                settingsRepository = container.settingsRepository,
                dietRepository = container.dietRepository,
                reminderRepository = container.reminderRepository
            )

            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                profileRepository = container.profileRepository,
                statsRepository = container.statsRepository,
                waterRepository = container.waterRepository,
                dietRepository = container.dietRepository,
                reminderRepository = container.reminderRepository,
                settingsRepository = container.settingsRepository
            )

            modelClass.isAssignableFrom(DietViewModel::class.java) -> DietViewModel(
                dietRepository = container.dietRepository,
                foodRepository = container.foodRepository,
                profileRepository = container.profileRepository,
                settingsRepository = container.settingsRepository
            )

            modelClass.isAssignableFrom(WorkoutViewModel::class.java) -> WorkoutViewModel(
                workoutRepository = container.workoutRepository
            )

            modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java) ->
                WorkoutSessionViewModel(workoutRepository = container.workoutRepository)

            modelClass.isAssignableFrom(ProgressViewModel::class.java) -> ProgressViewModel(
                statsRepository = container.statsRepository,
                weightRepository = container.weightRepository,
                profileRepository = container.profileRepository
            )

            modelClass.isAssignableFrom(WeightViewModel::class.java) -> WeightViewModel(
                weightRepository = container.weightRepository,
                profileRepository = container.profileRepository
            )

            modelClass.isAssignableFrom(WaterViewModel::class.java) -> WaterViewModel(
                waterRepository = container.waterRepository,
                settingsRepository = container.settingsRepository,
                dayRepository = container.dayRepository,
                statsRepository = container.statsRepository
            )

            modelClass.isAssignableFrom(StepsViewModel::class.java) -> StepsViewModel(
                stepRepository = container.stepRepository,
                settingsRepository = container.settingsRepository,
                dayRepository = container.dayRepository,
                statsRepository = container.statsRepository,
                stepSensorManager = container.stepSensorManager
            )

            modelClass.isAssignableFrom(BudgetViewModel::class.java) -> BudgetViewModel(
                budgetRepository = container.budgetRepository,
                profileRepository = container.profileRepository,
                dayRepository = container.dayRepository,
                statsRepository = container.statsRepository
            )

            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                profileRepository = container.profileRepository,
                settingsRepository = container.settingsRepository,
                dietRepository = container.dietRepository,
                reminderRepository = container.reminderRepository,
                statsRepository = container.statsRepository
            )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                profileRepository = container.profileRepository,
                backupRepository = container.backupRepository,
                reminderRepository = container.reminderRepository,
                dayRepository = container.dayRepository,
                dietRepository = container.dietRepository
            )

            modelClass.isAssignableFrom(RemindersViewModel::class.java) -> RemindersViewModel(
                reminderRepository = container.reminderRepository,
                settingsRepository = container.settingsRepository,
                profileRepository = container.profileRepository
            )

            modelClass.isAssignableFrom(FoodsViewModel::class.java) -> FoodsViewModel(
                foodRepository = container.foodRepository,
                settingsRepository = container.settingsRepository
            )

            modelClass.isAssignableFrom(MonthlyReportViewModel::class.java) -> MonthlyReportViewModel(
                statsRepository = container.statsRepository,
                backupRepository = container.backupRepository
            )

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return viewModel as T
    }
}

val LocalAppViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("AppViewModelFactory was not provided")
}
