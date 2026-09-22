package com.fitbudget.app.ui.screens.steps

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.repository.DayRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.data.repository.StepRepository
import com.fitbudget.app.data.sensors.StepSensorManager
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.StepSource
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

data class StepsUiState(
    val loading: Boolean = true,
    val epochDay: Long = DateTimeUtils.todayEpochDay(),
    val sensorSteps: Int = 0,
    val manualSteps: Int = 0,
    val goal: Int = 8_000,
    val weekTotals: List<Int> = emptyList(),
    val sensorAvailable: Boolean = false,
    val sensorPermissionGranted: Boolean = false,
    val streakDays: Int = 0
) {
    val totalSteps: Int get() = sensorSteps + manualSteps
    val fraction: Float get() = if (goal <= 0) 0f else (totalSteps.toFloat() / goal).coerceIn(0f, 1f)
    val goalMet: Boolean get() = goal > 0 && totalSteps >= goal
    val remaining: Int get() = (goal - totalSteps).coerceAtLeast(0)
    val source: StepSource
        get() = when {
            sensorSteps > 0 && manualSteps > 0 -> StepSource.MIXED
            manualSteps > 0 -> StepSource.MANUAL
            else -> StepSource.SENSOR
        }
    val averageWeek: Int get() = if (weekTotals.isEmpty()) 0 else weekTotals.sum() / weekTotals.size
}

class StepsViewModel(
    private val stepRepository: StepRepository,
    private val settingsRepository: SettingsRepository,
    private val dayRepository: DayRepository,
    private val statsRepository: StatsRepository,
    private val stepSensorManager: StepSensorManager
) : BaseViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StepsUiState> = currentDayFlow()
        .flatMapLatest { day ->
            combine(
                stepRepository.observeForDay(day),
                settingsRepository.settings,
                dayRepository.observe(day)
            ) { log, settings, meta ->
                Triple(log, settings, meta?.stepGoal ?: settings.stepGoal)
            }.map { (log, _, goal) ->
                val week = statsRepository.daySummaries(day - 6, day)
                StepsUiState(
                    loading = false,
                    epochDay = day,
                    sensorSteps = log?.sensorSteps ?: 0,
                    manualSteps = log?.manualSteps ?: 0,
                    goal = goal,
                    weekTotals = week.map { it.steps },
                    sensorAvailable = stepSensorManager.isSensorAvailable,
                    sensorPermissionGranted = stepSensorManager.hasPermission,
                    streakDays = statsRepository.streaks(lookBackDays = 120).let { streaks ->
                        // Steps are part of the overall checklist streak rather than their own.
                        streaks.overall
                    }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StepsUiState()
        )

    fun startSensor() = stepSensorManager.start()

    fun stopSensor() = stepSensorManager.stop()

    fun setManualSteps(raw: String) {
        val parsed = Validators.parseInt(raw)
        val result = Validators.steps(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid step count.")
            return
        }
        viewModelScope.launch {
            runCatching {
                stepRepository.setManualSteps(parsed!!, uiState.value.epochDay)
                notifyUser("Manual steps set to ${parsed}.")
            }.onFailure { notifyError(it, "Could not save those steps.") }
        }
    }

    fun addManualSteps(amount: Int) {
        viewModelScope.launch {
            runCatching {
                stepRepository.addManualSteps(amount, uiState.value.epochDay)
            }.onFailure { notifyError(it, "Could not add those steps.") }
        }
    }

    fun setGoal(raw: String) {
        val parsed = Validators.parseInt(raw)
        val result = Validators.steps(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid goal.")
            return
        }
        viewModelScope.launch {
            runCatching {
                settingsRepository.setStepGoal(parsed!!)
                dayRepository.applyStepGoalToToday(parsed)
                notifyUser("Step goal set to ${parsed}.")
            }.onFailure { notifyError(it, "Could not update the goal.") }
        }
    }
}
