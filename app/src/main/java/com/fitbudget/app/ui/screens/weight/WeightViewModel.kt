package com.fitbudget.app.ui.screens.weight

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.database.entity.WeightLogEntity
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.WeightRepository
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeightEntryForm(
    val weight: String = "",
    val note: String = "",
    val epochDay: Long = DateTimeUtils.todayEpochDay(),
    val error: String? = null,
    val saving: Boolean = false
)

data class WeightUiState(
    val loading: Boolean = true,
    val logs: List<WeightLogEntity> = emptyList(),
    val profile: ProfileEntity? = null
) {
    val latest: WeightLogEntity? get() = logs.maxByOrNull { it.epochDay }
    val earliest: WeightLogEntity? get() = logs.minByOrNull { it.epochDay }
    val totalChangeKg: Double?
        get() {
            val first = earliest ?: return null
            val last = latest ?: return null
            if (first.epochDay == last.epochDay) return 0.0
            return last.weightKg - first.weightKg
        }
    val averageWeeklyChangeKg: Double?
        get() {
            val first = earliest ?: return null
            val last = latest ?: return null
            if (first.epochDay == last.epochDay) return null
            return HealthCalculator.averageWeeklyChangeKg(
                firstWeightKg = first.weightKg,
                lastWeightKg = last.weightKg,
                daysBetween = last.epochDay - first.epochDay
            )
        }
    val chartPoints: List<Pair<Long, Double>>
        get() = logs.sortedBy { it.epochDay }.map { it.epochDay to it.weightKg }
}

class WeightViewModel(
    private val weightRepository: WeightRepository,
    profileRepository: ProfileRepository
) : BaseViewModel() {

    private val _form = MutableStateFlow(WeightEntryForm())
    val form: StateFlow<WeightEntryForm> = _form.asStateFlow()

    val uiState: StateFlow<WeightUiState> = combine(
        weightRepository.logs,
        profileRepository.profile
    ) { logs, profile ->
        WeightUiState(loading = false, logs = logs, profile = profile)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUiState()
    )

    init {
        viewModelScope.launch {
            // Pre-fill with today's existing entry, or the last known weight.
            val today = DateTimeUtils.todayEpochDay()
            val existing = weightRepository.getForDay(today)
            val fallback = profileRepository.get()?.currentWeightKg
            val value = existing?.weightKg ?: fallback
            _form.update {
                it.copy(
                    weight = value?.let { weight -> "%.1f".format(weight) } ?: "",
                    note = existing?.note.orEmpty(),
                    epochDay = today
                )
            }
        }
    }

    fun onWeightChange(value: String) {
        _form.update { it.copy(weight = value, error = null) }
    }

    fun onNoteChange(value: String) {
        _form.update { it.copy(note = value) }
    }

    fun onDateChange(epochDay: Long) {
        if (epochDay > DateTimeUtils.todayEpochDay()) {
            notifyUser("You cannot log a weight for a future date.")
            return
        }
        _form.update { it.copy(epochDay = epochDay) }
        viewModelScope.launch {
            val existing = weightRepository.getForDay(epochDay)
            if (existing != null) {
                _form.update {
                    it.copy(weight = "%.1f".format(existing.weightKg), note = existing.note.orEmpty())
                }
            }
        }
    }

    fun save(onSaved: () -> Unit = {}) {
        val current = _form.value
        val parsed = Validators.parseDecimal(current.weight)
        val result = Validators.weight(parsed)
        if (!result.isValid) {
            _form.update { it.copy(error = result.message) }
            return
        }
        _form.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                weightRepository.log(
                    weightKg = parsed!!,
                    epochDay = current.epochDay,
                    note = current.note
                )
                _form.update { it.copy(saving = false) }
                notifyUser("Weight logged for ${DateTimeUtils.relativeDayLabel(current.epochDay).lowercase()}.")
                onSaved()
            } catch (error: Throwable) {
                _form.update { it.copy(saving = false) }
                notifyError(error, "Could not save that weight.")
            }
        }
    }

    fun delete(log: WeightLogEntity) {
        viewModelScope.launch {
            runCatching {
                weightRepository.delete(log)
                notifyUser("Entry removed.")
            }.onFailure { notifyError(it, "Could not remove that entry.") }
        }
    }
}
