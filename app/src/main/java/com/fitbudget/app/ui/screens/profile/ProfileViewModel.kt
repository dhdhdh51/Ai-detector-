package com.fitbudget.app.ui.screens.profile

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.repository.DietRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.ReminderRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.data.settings.AppSettings
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.Streaks
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileEditForm(
    val name: String = "",
    val dateOfBirth: LocalDate = LocalDate.now(),
    val gender: Gender = Gender.MALE,
    val heightCm: String = "",
    val currentWeight: String = "",
    val targetWeight: String = "",
    val dailyBudget: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val dietPreference: DietPreference = DietPreference.VEGETARIAN,
    val wakeMinutes: Int = 6 * 60 + 30,
    val sleepMinutes: Int = 22 * 60 + 30,
    val calorieOverride: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val dirty: Boolean = false
)

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: ProfileEntity? = null,
    val settings: AppSettings = AppSettings(),
    val streaks: Streaks = Streaks(),
    val totalWorkouts: Int = 0,
    val weightEntries: Int = 0
) {
    val age: Int get() = profile?.age ?: 0
    val bmi: Double get() = profile?.bmi ?: 0.0
    val bmiCategory: String get() = HealthCalculator.bmiCategory(bmi)
    val healthyRange: ClosedFloatingPointRange<Double>
        get() = HealthCalculator.healthyWeightRange(profile?.heightCm ?: 0.0)
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val dietRepository: DietRepository,
    private val reminderRepository: ReminderRepository,
    statsRepository: StatsRepository
) : BaseViewModel() {

    private val _form = MutableStateFlow(ProfileEditForm())
    val form: StateFlow<ProfileEditForm> = _form.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.profile,
        settingsRepository.settings
    ) { profile, settings -> profile to settings }
        .map { (profile, settings) ->
            ProfileUiState(
                loading = false,
                profile = profile,
                settings = settings,
                streaks = statsRepository.streaks(lookBackDays = 180)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )

    init {
        viewModelScope.launch {
            profileRepository.get()?.let(::fillForm)
        }
    }

    private fun fillForm(profile: ProfileEntity) {
        _form.value = ProfileEditForm(
            name = profile.name,
            dateOfBirth = profile.dateOfBirth,
            gender = profile.gender,
            heightCm = profile.heightCm.toInt().toString(),
            currentWeight = "%.1f".format(profile.currentWeightKg),
            targetWeight = "%.1f".format(profile.targetWeightKg),
            dailyBudget = profile.dailyBudget.toInt().toString(),
            activityLevel = profile.activityLevel,
            dietPreference = profile.dietPreference,
            wakeMinutes = profile.wakeMinutes,
            sleepMinutes = profile.sleepMinutes,
            calorieOverride = profile.calorieTargetOverride?.toInt()?.toString().orEmpty()
        )
    }

    fun update(transform: (ProfileEditForm) -> ProfileEditForm) {
        _form.update { transform(it).copy(dirty = true, errors = emptyMap()) }
    }

    fun revert() {
        viewModelScope.launch { profileRepository.get()?.let(::fillForm) }
    }

    fun save() {
        val form = _form.value
        val height = Validators.parseDecimal(form.heightCm)
        val weight = Validators.parseDecimal(form.currentWeight)
        val target = Validators.parseDecimal(form.targetWeight)
        val budget = Validators.parseDecimal(form.dailyBudget)
        val override = form.calorieOverride.takeIf { it.isNotBlank() }
            ?.let { Validators.parseDecimal(it) }

        val errors = buildMap {
            Validators.name(form.name).message?.let { put("name", it) }
            Validators.dateOfBirth(form.dateOfBirth).message?.let { put("dob", it) }
            Validators.height(height).message?.let { put("height", it) }
            Validators.weight(weight, "Current weight").message?.let { put("weight", it) }
            Validators.targetWeight(target, weight).message?.let { put("target", it) }
            Validators.budget(budget).message?.let { put("budget", it) }
            if (form.calorieOverride.isNotBlank()) {
                Validators.calories(override).message?.let { put("calories", it) }
                if (override != null && override < 1000) {
                    put("calories", "A manual target below 1,000 kcal is not safe. Leave it blank to use the estimate.")
                }
            }
        }

        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }

        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val existing = profileRepository.ensureExists()
                val budgetChanged = existing.dailyBudget != budget
                val preferenceChanged = existing.dietPreference != form.dietPreference
                val timesChanged = existing.wakeMinutes != form.wakeMinutes ||
                    existing.sleepMinutes != form.sleepMinutes

                profileRepository.save(
                    existing.copy(
                        name = form.name.trim(),
                        dobEpochDay = form.dateOfBirth.toEpochDay(),
                        gender = form.gender,
                        heightCm = height!!,
                        currentWeightKg = weight!!,
                        targetWeightKg = target!!,
                        dailyBudget = budget!!,
                        activityLevel = form.activityLevel,
                        dietPreference = form.dietPreference,
                        wakeMinutes = form.wakeMinutes,
                        sleepMinutes = form.sleepMinutes,
                        calorieTargetOverride = override
                    )
                )

                if (budgetChanged) {
                    // Today follows the new budget; past days keep their history.
                    profileRepository.get()?.let { updated ->
                        dietRepository.ensurePlanForDay(DateTimeUtils.todayEpochDay())
                        settingsRepository.setLastRollOverDay(DateTimeUtils.todayEpochDay())
                        updated
                    }
                }
                if (preferenceChanged) {
                    dietRepository.regeneratePlan(DateTimeUtils.todayEpochDay())
                }
                if (timesChanged) {
                    reminderRepository.rescheduleAll()
                }

                _form.update { it.copy(saving = false, dirty = false) }
                notifyUser("Profile updated.")
            } catch (error: Throwable) {
                _form.update { it.copy(saving = false) }
                notifyError(error, "Could not save your profile.")
            }
        }
    }
}
