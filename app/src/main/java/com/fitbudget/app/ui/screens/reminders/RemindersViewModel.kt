package com.fitbudget.app.ui.screens.reminders

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.ReminderRepository
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ReminderRow(
    val reminder: ReminderEntity,
    val nextTrigger: LocalDateTime?
) {
    val type: ReminderType get() = reminder.type
    val nextTriggerLabel: String
        get() = nextTrigger?.let { next ->
            val day = when (next.toLocalDate().toEpochDay()) {
                DateTimeUtils.todayEpochDay() -> "today"
                DateTimeUtils.todayEpochDay() + 1 -> "tomorrow"
                else -> DateTimeUtils.formatShortDate(next.toLocalDate())
            }
            "Next: $day at ${DateTimeUtils.formatTime(next.hour, next.minute)}"
        } ?: "Not scheduled"
}

data class RemindersUiState(
    val loading: Boolean = true,
    val rows: List<ReminderRow> = emptyList(),
    val masterEnabled: Boolean = true,
    val exactAlarmsAllowed: Boolean = true,
    val wakeMinutes: Int = 6 * 60 + 30,
    val sleepMinutes: Int = 22 * 60 + 30
)

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository
) : BaseViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.reminders,
        settingsRepository.settings,
        profileRepository.profile,
        refreshTrigger
    ) { reminders, settings, profile, _ ->
        Triple(reminders, settings, profile)
    }.map { (reminders, settings, profile) ->
        RemindersUiState(
            loading = false,
            rows = reminders.map { reminder ->
                ReminderRow(
                    reminder = reminder,
                    nextTrigger = if (settings.remindersEnabled && reminder.enabled) {
                        reminderRepository.nextTrigger(reminder.type)
                    } else {
                        null
                    }
                )
            },
            masterEnabled = settings.remindersEnabled,
            exactAlarmsAllowed = reminderRepository.canScheduleExactAlarms(),
            wakeMinutes = profile?.wakeMinutes ?: ReminderRepository.DEFAULT_WAKE_MINUTES,
            sleepMinutes = profile?.sleepMinutes ?: ReminderRepository.DEFAULT_SLEEP_MINUTES
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersUiState()
    )

    init {
        viewModelScope.launch { reminderRepository.ensureDefaults() }
    }

    private fun refresh() {
        refreshTrigger.value += 1
    }

    fun setEnabled(type: ReminderType, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                reminderRepository.setEnabled(type, enabled)
                refresh()
            }.onFailure { notifyError(it, "Could not update that reminder.") }
        }
    }

    fun setTime(type: ReminderType, hour: Int, minute: Int) {
        val result = Validators.reminderTime(hour, minute)
        if (!result.isValid) {
            notifyUser(result.message ?: "Pick a valid time.")
            return
        }
        viewModelScope.launch {
            runCatching {
                reminderRepository.setTime(type, hour, minute)
                refresh()
                notifyUser("${type.label} reminder set to ${DateTimeUtils.formatTime(hour, minute)}.")
            }.onFailure { notifyError(it, "Could not change that time.") }
        }
    }

    fun setDays(type: ReminderType, daysMask: Int) {
        if (daysMask and DateTimeUtils.ALL_DAYS_MASK == 0) {
            notifyUser("Pick at least one day, or switch the reminder off.")
            return
        }
        viewModelScope.launch {
            runCatching {
                reminderRepository.setDays(type, daysMask)
                refresh()
            }.onFailure { notifyError(it, "Could not change those days.") }
        }
    }

    fun setInterval(type: ReminderType, minutes: Int) {
        val result = Validators.reminderInterval(minutes)
        if (!result.isValid) {
            notifyUser(result.message ?: "Pick a valid interval.")
            return
        }
        viewModelScope.launch {
            runCatching {
                reminderRepository.setInterval(type, minutes)
                refresh()
                notifyUser("Water reminder now repeats every ${minutes / 60}h ${minutes % 60}m.".replace(" 0m", ""))
            }.onFailure { notifyError(it, "Could not change the interval.") }
        }
    }

    fun setSound(type: ReminderType, enabled: Boolean) {
        viewModelScope.launch {
            runCatching { reminderRepository.setSound(type, enabled) }
                .onFailure { notifyError(it, "Could not update the sound setting.") }
        }
    }

    fun setVibration(type: ReminderType, enabled: Boolean) {
        viewModelScope.launch {
            runCatching { reminderRepository.setVibration(type, enabled) }
                .onFailure { notifyError(it, "Could not update the vibration setting.") }
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            reminderRepository.rescheduleAll()
            refresh()
        }
    }

    fun rescheduleAll() {
        viewModelScope.launch {
            runCatching {
                reminderRepository.rescheduleAll()
                refresh()
                notifyUser("All reminders re-scheduled.")
            }.onFailure { notifyError(it, "Could not re-schedule reminders.") }
        }
    }
}
