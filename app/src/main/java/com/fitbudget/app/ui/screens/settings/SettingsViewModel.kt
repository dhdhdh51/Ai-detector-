package com.fitbudget.app.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.repository.BackupRepository
import com.fitbudget.app.data.repository.DayRepository
import com.fitbudget.app.data.repository.DietRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.ReminderRepository
import com.fitbudget.app.data.settings.AppSettings
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.ThemeMode
import com.fitbudget.app.domain.model.UnitSystem
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth

data class ShareRequest(
    val file: File,
    val mimeType: String,
    val title: String
)

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val profile: ProfileEntity? = null,
    val busy: Boolean = false,
    val exactAlarmsAllowed: Boolean = true
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val reminderRepository: ReminderRepository,
    private val dayRepository: DayRepository,
    private val dietRepository: DietRepository
) : BaseViewModel() {

    private val busy = MutableStateFlow(false)

    private val _pendingShare = MutableStateFlow<ShareRequest?>(null)
    val pendingShare: StateFlow<ShareRequest?> = _pendingShare.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        profileRepository.profile,
        busy
    ) { settings, profile, isBusy ->
        SettingsUiState(
            loading = false,
            settings = settings,
            profile = profile,
            busy = isBusy,
            exactAlarmsAllowed = reminderRepository.canScheduleExactAlarms()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    // ---------------------------------------------------------------- preferences

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setUnits(units: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(units) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            reminderRepository.rescheduleAll()
            notifyUser(if (enabled) "Reminders switched on." else "All reminders paused.")
        }
    }

    fun setNotificationSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationSound(enabled)
            reminderRepository.getAll().forEach { reminder ->
                reminderRepository.setSound(reminder.type, enabled)
            }
        }
    }

    fun setNotificationVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationVibration(enabled)
            reminderRepository.getAll().forEach { reminder ->
                reminderRepository.setVibration(reminder.type, enabled)
            }
        }
    }

    fun setWaterTarget(raw: String) {
        val parsed = Validators.parseInt(raw)
        val result = Validators.waterTarget(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid target.")
            return
        }
        viewModelScope.launch {
            settingsRepository.setWaterTarget(parsed!!)
            dayRepository.applyWaterTargetToToday(parsed)
            notifyUser("Water target updated.")
        }
    }

    fun setStepGoal(raw: String) {
        val parsed = Validators.parseInt(raw)
        val result = Validators.steps(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid goal.")
            return
        }
        viewModelScope.launch {
            settingsRepository.setStepGoal(parsed!!)
            dayRepository.applyStepGoalToToday(parsed)
            notifyUser("Step goal updated.")
        }
    }

    fun setDailyBudget(raw: String) {
        val parsed = Validators.parseDecimal(raw)
        val result = Validators.budget(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid budget.")
            return
        }
        viewModelScope.launch {
            profileRepository.setDailyBudget(parsed!!)
            dayRepository.applyBudgetToToday(parsed)
            notifyUser("Daily budget updated.")
        }
    }

    fun setTargetWeight(raw: String) {
        val parsed = Validators.parseDecimal(raw)
        val current = uiState.value.profile?.currentWeightKg
        val result = Validators.targetWeight(parsed, current)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid target weight.")
            return
        }
        viewModelScope.launch {
            profileRepository.setTargetWeight(parsed!!)
            notifyUser("Target weight updated.")
        }
    }

    fun setCalorieOverride(raw: String) {
        if (raw.isBlank()) {
            viewModelScope.launch {
                profileRepository.setCalorieOverride(null)
                notifyUser("Using the estimated calorie target again.")
            }
            return
        }
        val parsed = Validators.parseDecimal(raw)
        if (parsed == null || parsed < 1000 || parsed > 5000) {
            notifyUser("Enter a calorie target between 1,000 and 5,000, or leave it blank.")
            return
        }
        viewModelScope.launch {
            profileRepository.setCalorieOverride(parsed)
            notifyUser("Calorie target set to ${parsed.toInt()} kcal.")
        }
    }

    fun setDietPreference(preference: DietPreference) {
        viewModelScope.launch {
            val profile = profileRepository.get() ?: return@launch
            profileRepository.save(profile.copy(dietPreference = preference))
            dietRepository.regeneratePlan(DateTimeUtils.todayEpochDay())
            notifyUser("Plan updated for ${preference.label.lowercase()} preference.")
        }
    }

    // ---------------------------------------------------------------- data actions

    fun exportCsv() = runExport("Export data") {
        ShareRequest(backupRepository.writeCsvExport(), "text/csv", "FitBudget data export")
    }

    fun exportMonthlyReport(month: YearMonth = YearMonth.now()) = runExport("Export report") {
        ShareRequest(
            backupRepository.writeMonthlyReport(month),
            "text/plain",
            "FitBudget monthly report"
        )
    }

    fun createBackup() = runExport("Backup") {
        ShareRequest(
            backupRepository.writeBackup(),
            "application/json",
            "FitBudget backup"
        )
    }

    private fun runExport(label: String, block: suspend () -> ShareRequest) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                _pendingShare.value = block()
            } catch (error: Throwable) {
                notifyError(error, "$label failed.")
            } finally {
                busy.value = false
            }
        }
    }

    fun consumeShare() {
        _pendingShare.value = null
    }

    fun restoreBackup(content: String) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            backupRepository.restoreBackup(content)
                .onSuccess { summary ->
                    notifyUser(
                        "Restored ${summary.weightLogs} weigh-ins, ${summary.mealEntries} meal items, " +
                            "${summary.workouts} workouts."
                    )
                }
                .onFailure {
                    notifyUser("That file could not be restored. Your existing data is untouched.")
                }
            busy.value = false
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                backupRepository.resetAllData()
                notifyUser("All local data has been erased.")
                onComplete()
            } catch (error: Throwable) {
                notifyError(error, "Could not reset your data.")
            } finally {
                busy.value = false
            }
        }
    }
}
