package com.fitbudget.app.ui.screens.workout

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.WorkoutSessionEntity
import com.fitbudget.app.data.repository.WorkoutRepository
import com.fitbudget.app.data.seed.WorkoutTemplate
import com.fitbudget.app.domain.model.WorkoutCategory
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutUiState(
    val loading: Boolean = true,
    val selectedCategory: WorkoutCategory? = null,
    val templates: List<WorkoutTemplate> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
    val todayCount: Int = 0
) {
    val totalSessions: Int get() = history.size
    val totalMinutes: Int get() = history.sumOf { it.durationSeconds } / 60
}

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository
) : BaseViewModel() {

    private val selectedCategory = MutableStateFlow<WorkoutCategory?>(null)

    val uiState: StateFlow<WorkoutUiState> = combine(
        selectedCategory,
        workoutRepository.history,
        workoutRepository.observeCountForDay(DateTimeUtils.todayEpochDay())
    ) { category, history, todayCount ->
        WorkoutUiState(
            loading = false,
            selectedCategory = category,
            templates = workoutRepository.templates()
                .filter { category == null || it.category == category },
            history = history,
            todayCount = todayCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState()
    )

    fun selectCategory(category: WorkoutCategory?) {
        selectedCategory.value = if (selectedCategory.value == category) null else category
    }

    fun deleteSession(session: WorkoutSessionEntity) {
        viewModelScope.launch {
            runCatching {
                workoutRepository.delete(session)
                notifyUser("Session removed from history.")
            }.onFailure { notifyError(it, "Could not delete that session.") }
        }
    }
}
