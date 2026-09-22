package com.fitbudget.app.ui.screens.progress

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.data.repository.WeightRepository
import com.fitbudget.app.domain.DailyChecklist
import com.fitbudget.app.domain.DaySummary
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.Streaks
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ProgressRange(val label: String, val days: Int?) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    QUARTER("90 days", 90),
    ALL("All time", null)
}

data class ProgressUiState(
    val loading: Boolean = true,
    val range: ProgressRange = ProgressRange.MONTH,
    val profile: ProfileEntity? = null,
    val weightPoints: List<Pair<Long, Double>> = emptyList(),
    val summaries: List<DaySummary> = emptyList(),
    val streaks: Streaks = Streaks(),
    val totalLostKg: Double = 0.0,
    val averageWeeklyChangeKg: Double = 0.0,
    val weightLogCount: Int = 0
) {
    val spendSeries: List<Pair<Long, Double>> get() = summaries.map { it.epochDay to it.spent }
    val stepSeries: List<Pair<Long, Double>>
        get() = summaries.map { it.epochDay to it.steps.toDouble() }
    val completionSeries: List<Pair<Long, Double>>
        get() = summaries.map { it.epochDay to DailyChecklist.completionPercent(it).toDouble() }
    val averageCompletion: Int
        get() = summaries.filter { it.hasAnyActivity }
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list.sumOf { DailyChecklist.completionPercent(it) } / list.size }
            ?: 0
    val averageSteps: Int
        get() = summaries.filter { it.steps > 0 }
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list.sumOf { it.steps } / list.size }
            ?: 0
    val averageSpend: Double
        get() = summaries.filter { it.spent > 0 }
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list.sumOf { it.spent } / list.size }
            ?: 0.0
    val workoutsInRange: Int get() = summaries.sumOf { it.workoutsCompleted }
    val hasWeightData: Boolean get() = weightPoints.isNotEmpty()
}

class ProgressViewModel(
    private val statsRepository: StatsRepository,
    weightRepository: WeightRepository,
    profileRepository: ProfileRepository
) : BaseViewModel() {

    private val range = MutableStateFlow(ProgressRange.MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProgressUiState> = combine(
        range,
        currentDayFlow()
    ) { selected, today -> selected to today }
        .flatMapLatest { (selected, today) ->
            combine(
                weightRepository.logs,
                profileRepository.profile
            ) { logs, profile -> logs to profile }
                .map { (logs, profile) ->
                    val from = when (val days = selected.days) {
                        null -> logs.minOfOrNull { it.epochDay } ?: (today - 29)
                        else -> today - days + 1
                    }
                    val points = logs
                        .filter { it.epochDay in from..today }
                        .sortedBy { it.epochDay }
                        .map { it.epochDay to it.weightKg }

                    val summaries = statsRepository.daySummaries(from, today)
                    val first = points.firstOrNull()
                    val last = points.lastOrNull()

                    ProgressUiState(
                        loading = false,
                        range = selected,
                        profile = profile,
                        weightPoints = points,
                        summaries = summaries,
                        streaks = statsRepository.streaks(lookBackDays = 180),
                        totalLostKg = if (profile != null) {
                            profile.startWeightKg - profile.currentWeightKg
                        } else {
                            0.0
                        },
                        averageWeeklyChangeKg = if (first != null && last != null && points.size > 1) {
                            HealthCalculator.averageWeeklyChangeKg(
                                firstWeightKg = first.second,
                                lastWeightKg = last.second,
                                daysBetween = last.first - first.first
                            )
                        } else {
                            0.0
                        },
                        weightLogCount = logs.size
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgressUiState()
        )

    fun setRange(selected: ProgressRange) {
        range.value = selected
    }

    fun label(epochDay: Long): String = DateTimeUtils.formatShortDate(DateTimeUtils.date(epochDay))
}
