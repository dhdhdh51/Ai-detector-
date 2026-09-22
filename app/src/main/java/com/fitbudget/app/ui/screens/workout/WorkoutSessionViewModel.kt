package com.fitbudget.app.ui.screens.workout

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.repository.WorkoutRepository
import com.fitbudget.app.data.seed.Exercise
import com.fitbudget.app.data.seed.WorkoutTemplate
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExerciseStatus { PENDING, DONE, SKIPPED }

data class SessionUiState(
    val loading: Boolean = true,
    val template: WorkoutTemplate? = null,
    val currentIndex: Int = 0,
    val statuses: List<ExerciseStatus> = emptyList(),
    val elapsedSeconds: Int = 0,
    val running: Boolean = false,
    val finished: Boolean = false,
    /** Countdown for a timed exercise; null when the current exercise is rep based. */
    val exerciseTimerSeconds: Int? = null,
    val savedSessionId: Long? = null
) {
    val currentExercise: Exercise?
        get() = template?.exercises?.getOrNull(currentIndex)

    val completedCount: Int get() = statuses.count { it == ExerciseStatus.DONE }
    val skippedCount: Int get() = statuses.count { it == ExerciseStatus.SKIPPED }
    val totalCount: Int get() = template?.exercises?.size ?: 0
    val progressFraction: Float
        get() = if (totalCount == 0) 0f
        else (statuses.count { it != ExerciseStatus.PENDING }.toFloat() / totalCount)
    val isLastExercise: Boolean get() = totalCount > 0 && currentIndex >= totalCount - 1
    val allHandled: Boolean
        get() = totalCount > 0 && statuses.none { it == ExerciseStatus.PENDING }
}

/**
 * Drives a live workout: start / pause / resume, per-exercise complete or skip, a countdown for
 * timed exercises, and saving the finished session to history.
 */
class WorkoutSessionViewModel(
    private val workoutRepository: WorkoutRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var startedAtMillis: Long = 0L

    fun load(templateId: String) {
        if (_uiState.value.template?.id == templateId) return
        val template = workoutRepository.template(templateId)
        if (template == null) {
            notifyUser("That workout could not be found.")
            _uiState.value = SessionUiState(loading = false)
            return
        }
        startedAtMillis = DateTimeUtils.nowMillis()
        _uiState.value = SessionUiState(
            loading = false,
            template = template,
            statuses = List(template.exercises.size) { ExerciseStatus.PENDING },
            exerciseTimerSeconds = template.exercises.firstOrNull()?.durationSeconds
        )
        start()
    }

    fun start() {
        if (_uiState.value.template == null || _uiState.value.finished) return
        _uiState.update { it.copy(running = true) }
        startTicker()
    }

    fun pause() {
        _uiState.update { it.copy(running = false) }
        tickerJob?.cancel()
        tickerJob = null
    }

    fun toggleRunning() {
        if (_uiState.value.running) pause() else start()
    }

    private fun startTicker() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val state = _uiState.value
                if (!state.running || state.finished) break
                val nextTimer = state.exerciseTimerSeconds?.let { (it - 1).coerceAtLeast(0) }
                _uiState.update {
                    it.copy(
                        elapsedSeconds = it.elapsedSeconds + 1,
                        exerciseTimerSeconds = nextTimer
                    )
                }
            }
            tickerJob = null
        }
    }

    fun resetExerciseTimer() {
        val duration = _uiState.value.currentExercise?.durationSeconds ?: return
        _uiState.update { it.copy(exerciseTimerSeconds = duration) }
    }

    fun completeCurrent() = markCurrent(ExerciseStatus.DONE)

    fun skipCurrent() = markCurrent(ExerciseStatus.SKIPPED)

    private fun markCurrent(status: ExerciseStatus) {
        val state = _uiState.value
        val template = state.template ?: return
        if (state.currentIndex !in template.exercises.indices) return

        val statuses = state.statuses.toMutableList()
        statuses[state.currentIndex] = status

        val nextPending = statuses.indexOfFirst { it == ExerciseStatus.PENDING }
        val nextIndex = if (nextPending >= 0) nextPending else state.currentIndex

        _uiState.update {
            it.copy(
                statuses = statuses,
                currentIndex = nextIndex,
                exerciseTimerSeconds = template.exercises.getOrNull(nextIndex)?.durationSeconds
            )
        }
    }

    fun selectExercise(index: Int) {
        val template = _uiState.value.template ?: return
        if (index !in template.exercises.indices) return
        _uiState.update {
            it.copy(
                currentIndex = index,
                exerciseTimerSeconds = template.exercises[index].durationSeconds
            )
        }
    }

    /** Saves the session (even a partial one) so history always reflects what really happened. */
    fun finish(onSaved: () -> Unit) {
        val state = _uiState.value
        val template = state.template ?: return
        if (state.completedCount == 0 && state.skippedCount == 0) {
            notifyUser("Complete at least one exercise before finishing.")
            return
        }
        pause()
        viewModelScope.launch {
            try {
                val id = workoutRepository.saveSession(
                    template = template,
                    startedAtMillis = startedAtMillis,
                    finishedAtMillis = DateTimeUtils.nowMillis(),
                    durationSeconds = state.elapsedSeconds,
                    completed = state.completedCount,
                    skipped = state.skippedCount
                )
                _uiState.update { it.copy(finished = true, savedSessionId = id) }
                notifyUser("Workout saved — ${state.completedCount} of ${state.totalCount} exercises done.")
                onSaved()
            } catch (error: Throwable) {
                notifyError(error, "Could not save this workout.")
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}
