package com.fitbudget.app.ui.screens.onboarding

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.repository.DietRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.ReminderRepository
import com.fitbudget.app.data.repository.WeightRepository
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OnboardingForm(
    val name: String = "",
    val dateOfBirth: LocalDate = LocalDate.ofEpochDay(ProfileEntity.DEFAULT_DOB_EPOCH_DAY),
    val gender: Gender = Gender.MALE,
    val heightCm: String = "172",
    val currentWeight: String = "85",
    val targetWeight: String = "75",
    val dailyBudget: String = "100",
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val dietPreference: DietPreference = DietPreference.EGGETARIAN,
    val wakeMinutes: Int = 6 * 60 + 30,
    val sleepMinutes: Int = 22 * 60 + 30
)

data class PlanSummary(
    val name: String,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val dailyBudget: Double,
    val waterTargetMl: Int,
    val stepGoal: Int,
    val calorieTarget: Double,
    val proteinTargetG: Double,
    val bmi: Double,
    val bmiCategory: String,
    val weeklyTrendKg: Double
)

data class OnboardingUiState(
    val step: Int = 0,
    val form: OnboardingForm = OnboardingForm(),
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val summary: PlanSummary? = null
) {
    val totalSteps: Int get() = 4
    val isLastStep: Boolean get() = step == totalSteps - 1
}

/**
 * First-launch flow. The example profile from the brief is pre-filled, every field is editable and
 * nothing is stored until the answers validate.
 */
class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
    private val settingsRepository: SettingsRepository,
    private val dietRepository: DietRepository,
    private val reminderRepository: ReminderRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Keep any values the user already had (e.g. after re-opening onboarding).
            profileRepository.get()?.takeIf { it.name.isNotBlank() }?.let { existing ->
                _uiState.update { state ->
                    state.copy(
                        form = OnboardingForm(
                            name = existing.name,
                            dateOfBirth = existing.dateOfBirth,
                            gender = existing.gender,
                            heightCm = existing.heightCm.toInt().toString(),
                            currentWeight = "%.1f".format(existing.currentWeightKg),
                            targetWeight = "%.1f".format(existing.targetWeightKg),
                            dailyBudget = "%.0f".format(existing.dailyBudget),
                            activityLevel = existing.activityLevel,
                            dietPreference = existing.dietPreference,
                            wakeMinutes = existing.wakeMinutes,
                            sleepMinutes = existing.sleepMinutes
                        )
                    )
                }
            }
        }
    }

    fun update(transform: (OnboardingForm) -> OnboardingForm) {
        _uiState.update { it.copy(form = transform(it.form), errors = emptyMap()) }
    }

    fun back() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0), errors = emptyMap()) }
    }

    /** Validates the current step and either advances or exposes field errors. */
    fun next(onFinished: () -> Unit) {
        val state = _uiState.value
        val errors = validate(state.step, state.form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        if (state.isLastStep) {
            save(onFinished)
        } else {
            _uiState.update { it.copy(step = it.step + 1, errors = emptyMap()) }
        }
    }

    private fun validate(step: Int, form: OnboardingForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        when (step) {
            0 -> {
                Validators.name(form.name).message?.let { errors["name"] = it }
                Validators.dateOfBirth(form.dateOfBirth).message?.let { errors["dob"] = it }
            }

            1 -> {
                val height = Validators.parseDecimal(form.heightCm)
                val weight = Validators.parseDecimal(form.currentWeight)
                val target = Validators.parseDecimal(form.targetWeight)
                Validators.height(height).message?.let { errors["height"] = it }
                Validators.weight(weight, "Current weight").message?.let { errors["weight"] = it }
                Validators.targetWeight(target, weight).message?.let { errors["target"] = it }
            }

            2 -> {
                Validators.budget(Validators.parseDecimal(form.dailyBudget)).message
                    ?.let { errors["budget"] = it }
            }
        }
        return errors
    }

    private fun save(onFinished: () -> Unit) {
        val form = _uiState.value.form
        val height = Validators.parseDecimal(form.heightCm) ?: return
        val weight = Validators.parseDecimal(form.currentWeight) ?: return
        val target = Validators.parseDecimal(form.targetWeight) ?: return
        val budget = Validators.parseDecimal(form.dailyBudget) ?: return

        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            try {
                val existing = profileRepository.ensureExists()
                val profile = existing.copy(
                    name = form.name.trim(),
                    dobEpochDay = form.dateOfBirth.toEpochDay(),
                    gender = form.gender,
                    heightCm = height,
                    startWeightKg = weight,
                    currentWeightKg = weight,
                    targetWeightKg = target,
                    dailyBudget = budget,
                    activityLevel = form.activityLevel,
                    dietPreference = form.dietPreference,
                    wakeMinutes = form.wakeMinutes,
                    sleepMinutes = form.sleepMinutes
                )
                profileRepository.completeOnboarding(profile)

                // The first weighing is real data the user just entered, so it is logged as day one.
                weightRepository.log(weight, DateTimeUtils.todayEpochDay(), "Starting weight")

                val waterTarget = HealthCalculator.suggestedWaterMl(weight)
                settingsRepository.setWaterTarget(waterTarget)

                dietRepository.ensurePlanForDay(DateTimeUtils.todayEpochDay())
                reminderRepository.rescheduleAll()

                val settings = settingsRepository.current()
                val stored = profileRepository.get() ?: profile
                _uiState.update {
                    it.copy(
                        saving = false,
                        summary = PlanSummary(
                            name = stored.name,
                            currentWeightKg = stored.currentWeightKg,
                            targetWeightKg = stored.targetWeightKg,
                            dailyBudget = stored.dailyBudget,
                            waterTargetMl = settings.waterTargetMl,
                            stepGoal = settings.stepGoal,
                            calorieTarget = stored.estimatedCalorieTarget,
                            proteinTargetG = stored.proteinTargetGrams,
                            bmi = stored.bmi,
                            bmiCategory = HealthCalculator.bmiCategory(stored.bmi),
                            weeklyTrendKg = HealthCalculator.weeklyWeightChangeKg(
                                tdee = stored.tdee,
                                intakeCalories = stored.estimatedCalorieTarget
                            )
                        )
                    )
                }
                onFinished()
            } catch (error: Throwable) {
                _uiState.update { it.copy(saving = false) }
                notifyError(error, "Could not save your profile. Please try again.")
            }
        }
    }
}
