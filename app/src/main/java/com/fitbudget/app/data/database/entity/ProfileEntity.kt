package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.util.DateTimeUtils
import java.time.LocalDate

/**
 * Single-row table: the local user profile. There is no account and nothing leaves the device.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String = "",
    val dobEpochDay: Long = DEFAULT_DOB_EPOCH_DAY,
    val gender: Gender = Gender.MALE,
    val heightCm: Double = 172.0,
    val startWeightKg: Double = 85.0,
    val currentWeightKg: Double = 85.0,
    val targetWeightKg: Double = 75.0,
    val dailyBudget: Double = 100.0,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val wakeMinutes: Int = 6 * 60 + 30,
    val sleepMinutes: Int = 22 * 60 + 30,
    val dietPreference: DietPreference = DietPreference.EGGETARIAN,
    /** Manual calorie override; null means "use the estimate". */
    val calorieTargetOverride: Double? = null,
    val onboardingComplete: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val dateOfBirth: LocalDate get() = DateTimeUtils.date(dobEpochDay)

    val age: Int get() = HealthCalculator.age(dateOfBirth)

    val bmi: Double get() = HealthCalculator.bmi(currentWeightKg, heightCm)

    val remainingWeightKg: Double
        get() = HealthCalculator.weightDifference(currentWeightKg, targetWeightKg)

    val progressPercent: Double
        get() = HealthCalculator.progressPercent(startWeightKg, currentWeightKg, targetWeightKg)

    val isLosingWeight: Boolean get() = currentWeightKg > targetWeightKg

    val bmr: Double get() = HealthCalculator.bmr(currentWeightKg, heightCm, age, gender)

    val tdee: Double get() = HealthCalculator.tdee(bmr, activityLevel)

    val estimatedCalorieTarget: Double
        get() = calorieTargetOverride
            ?: HealthCalculator.calorieTarget(tdee, gender, isLosingWeight)

    val proteinTargetGrams: Double get() = HealthCalculator.proteinTargetGrams(currentWeightKg)

    companion object {
        const val SINGLETON_ID = 1

        /** Example value pre-filled on the onboarding form; the user can change everything. */
        val DEFAULT_DOB_EPOCH_DAY: Long = LocalDate.of(2006, 11, 22).toEpochDay()

        fun default(): ProfileEntity {
            val now = DateTimeUtils.nowMillis()
            return ProfileEntity(createdAtMillis = now, updatedAtMillis = now)
        }
    }
}
