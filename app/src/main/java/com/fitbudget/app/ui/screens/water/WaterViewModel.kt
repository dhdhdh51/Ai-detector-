package com.fitbudget.app.ui.screens.water

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.WaterLogEntity
import com.fitbudget.app.data.repository.DayRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.data.repository.WaterRepository
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.WaterCalculator
import com.fitbudget.app.domain.WaterSnapshot
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

data class WaterUiState(
    val loading: Boolean = true,
    val epochDay: Long = DateTimeUtils.todayEpochDay(),
    val consumedMl: Int = 0,
    val targetMl: Int = WaterCalculator.DEFAULT_TARGET_ML,
    val logs: List<WaterLogEntity> = emptyList(),
    val weekTotals: List<Int> = emptyList(),
    val streakDays: Int = 0
) {
    val snapshot: WaterSnapshot get() = WaterCalculator.snapshot(consumedMl, targetMl)
    val averageWeekMl: Int get() = WaterCalculator.averageMl(weekTotals)
}

class WaterViewModel(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val dayRepository: DayRepository,
    private val statsRepository: StatsRepository
) : BaseViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WaterUiState> = currentDayFlow()
        .flatMapLatest { day ->
            combine(
                waterRepository.observeTotal(day),
                waterRepository.observeLogs(day),
                settingsRepository.settings,
                dayRepository.observe(day)
            ) { total, logs, settings, meta ->
                Triple(total, logs, meta?.waterTargetMl ?: settings.waterTargetMl)
            }.map { (total, logs, target) ->
                val week = statsRepository.daySummaries(day - 6, day)
                WaterUiState(
                    loading = false,
                    epochDay = day,
                    consumedMl = total,
                    targetMl = target,
                    logs = logs,
                    weekTotals = week.map { it.waterMl },
                    streakDays = statsRepository.streaks(lookBackDays = 120).water
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WaterUiState()
        )

    fun add(amountMl: Int) {
        val result = Validators.waterEntry(amountMl)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid amount.")
            return
        }
        viewModelScope.launch {
            runCatching { waterRepository.add(amountMl) }
                .onFailure { notifyError(it, "Could not log that water.") }
        }
    }

    fun undoLast() {
        viewModelScope.launch {
            runCatching {
                if (waterRepository.undoLast()) {
                    notifyUser("Last entry removed.")
                } else {
                    notifyUser("Nothing logged yet today.")
                }
            }.onFailure { notifyError(it, "Could not undo that entry.") }
        }
    }

    fun delete(log: WaterLogEntity) {
        viewModelScope.launch {
            runCatching { waterRepository.delete(log) }
                .onFailure { notifyError(it, "Could not remove that entry.") }
        }
    }

    fun setTarget(rawValue: String) {
        val parsed = Validators.parseInt(rawValue)
        val result = Validators.waterTarget(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid target.")
            return
        }
        viewModelScope.launch {
            runCatching {
                settingsRepository.setWaterTarget(parsed!!)
                dayRepository.applyWaterTargetToToday(parsed)
                notifyUser("Water target set to ${parsed} ml.")
            }.onFailure { notifyError(it, "Could not update the target.") }
        }
    }

    fun clearToday() {
        viewModelScope.launch {
            runCatching {
                waterRepository.clearDay(uiState.value.epochDay)
                notifyUser("Today's water log cleared.")
            }.onFailure { notifyError(it, "Could not clear the log.") }
        }
    }
}
