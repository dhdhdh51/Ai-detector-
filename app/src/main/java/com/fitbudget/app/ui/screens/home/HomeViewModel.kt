package com.fitbudget.app.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.repository.DietRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.ReminderRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.data.repository.WaterRepository
import com.fitbudget.app.data.settings.AppSettings
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.ChecklistItem
import com.fitbudget.app.domain.DailyChecklist
import com.fitbudget.app.domain.DaySummary
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.Streaks
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val greeting: String = "",
    val profile: ProfileEntity? = null,
    val settings: AppSettings = AppSettings(),
    val summary: DaySummary = DaySummary(DateTimeUtils.todayEpochDay()),
    val checklist: List<ChecklistItem> = emptyList(),
    val streaks: Streaks = Streaks(),
    val weekSteps: List<Int> = emptyList(),
    val deficitRange: IntRange = 0..0,
    val weeklyTrendKg: Double = 0.0,
    val weeksToTarget: Int? = null,
    val exactAlarmsAllowed: Boolean = true
) {
    val completionPercent: Int
        get() = (DailyChecklist.completionFraction(checklist) * 100).toInt()
}

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository,
    private val waterRepository: WaterRepository,
    private val dietRepository: DietRepository,
    private val reminderRepository: ReminderRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        currentDayFlow(),
        profileRepository.profile,
        settingsRepository.settings
    ) { day, profile, settings -> Triple(day, profile, settings) }
        .flatMapLatest { (day, profile, settings) ->
            // Make sure the day exists and has a plan before the dashboard reads it.
            ensureDay(day)
            statsRepository.observeSummary(day).map { summary ->
                val weekSummaries = statsRepository.daySummaries(day - 6, day)
                val tdee = profile?.tdee ?: 0.0
                val target = profile?.estimatedCalorieTarget ?: 0.0
                HomeUiState(
                    loading = false,
                    greeting = DateTimeUtils.greeting(),
                    profile = profile,
                    settings = settings,
                    summary = summary,
                    checklist = DailyChecklist.build(summary),
                    streaks = statsRepository.streaks(lookBackDays = 120),
                    weekSteps = weekSummaries.map { it.steps },
                    deficitRange = HealthCalculator.deficitRange(tdee),
                    weeklyTrendKg = HealthCalculator.weeklyWeightChangeKg(tdee, target),
                    weeksToTarget = profile?.let {
                        HealthCalculator.weeksToTarget(
                            currentKg = it.currentWeightKg,
                            targetKg = it.targetWeightKg,
                            weeklyLossKg = HealthCalculator.weeklyWeightChangeKg(tdee, target)
                        )
                    },
                    exactAlarmsAllowed = reminderRepository.canScheduleExactAlarms()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    private suspend fun ensureDay(epochDay: Long) {
        runCatching {
            if (profileRepository.get()?.onboardingComplete == true) {
                dietRepository.ensurePlanForDay(epochDay)
            }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            try {
                waterRepository.add(amountMl)
                notifyUser("Added $amountMl ml of water.")
            } catch (error: Throwable) {
                notifyError(error, "Could not log water.")
            }
        }
    }
}
