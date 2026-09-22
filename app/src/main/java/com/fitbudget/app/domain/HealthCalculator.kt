package com.fitbudget.app.domain

import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.Gender
import java.time.LocalDate
import java.time.Period
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Every value produced here is an **estimate** and is labelled as such in the UI.
 * The app never presents a diagnosis or a guaranteed outcome.
 */
object HealthCalculator {

    /** Calories in roughly 1 kg of body fat, the standard planning figure. */
    const val KCAL_PER_KG = 7700.0

    const val MIN_SAFE_CALORIES_MALE = 1500.0
    const val MIN_SAFE_CALORIES_FEMALE = 1200.0

    fun age(dateOfBirth: LocalDate, on: LocalDate = LocalDate.now()): Int {
        if (dateOfBirth.isAfter(on)) return 0
        return Period.between(dateOfBirth, on).years
    }

    /** BMI = weight(kg) / height(m)². Returns 0 for impossible input instead of crashing. */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0.0 || weightKg <= 0.0) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi <= 0.0 -> "—"
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Healthy range"
        bmi < 30.0 -> "Overweight"
        else -> "Obese range"
    }

    /** Healthy-BMI weight span for a height, used to sanity-check a target weight. */
    fun healthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double> {
        if (heightCm <= 0.0) return 0.0..0.0
        val heightM = heightCm / 100.0
        return (18.5 * heightM * heightM)..(24.9 * heightM * heightM)
    }

    /** Remaining weight to the target. Negative means the target is already passed. */
    fun weightDifference(currentKg: Double, targetKg: Double): Double = currentKg - targetKg

    /**
     * Progress = (start - current) / (start - target) × 100, clamped to 0..100.
     * Returns 100 when the start weight already equals the target (nothing left to do).
     */
    fun progressPercent(startKg: Double, currentKg: Double, targetKg: Double): Double {
        val span = startKg - targetKg
        if (abs(span) < 0.001) return if (currentKg <= targetKg) 100.0 else 0.0
        val done = (startKg - currentKg) / span * 100.0
        return done.coerceIn(0.0, 100.0)
    }

    /** Mifflin-St Jeor basal metabolic rate estimate. */
    fun bmr(weightKg: Double, heightCm: Double, age: Int, gender: Gender): Double {
        if (weightKg <= 0.0 || heightCm <= 0.0) return 0.0
        val safeAge = age.coerceIn(10, 100)
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * safeAge
        return when (gender) {
            Gender.MALE -> base + 5.0
            Gender.FEMALE -> base - 161.0
            // Neutral midpoint so the estimate is never wildly off for either direction.
            Gender.OTHER -> base - 78.0
        }.coerceAtLeast(0.0)
    }

    /** Total daily energy expenditure estimate = BMR × activity factor. */
    fun tdee(bmr: Double, activityLevel: ActivityLevel): Double = bmr * activityLevel.factor

    /**
     * Suggested intake for a fat-loss goal: a moderate deficit off maintenance, never below
     * a conservative floor. When the user is at or below target this returns maintenance.
     */
    fun calorieTarget(
        tdee: Double,
        gender: Gender,
        losingWeight: Boolean
    ): Double {
        if (tdee <= 0.0) return 0.0
        if (!losingWeight) return tdee.roundToNearest(10)
        val floor = when (gender) {
            Gender.FEMALE -> MIN_SAFE_CALORIES_FEMALE
            Gender.MALE -> MIN_SAFE_CALORIES_MALE
            Gender.OTHER -> (MIN_SAFE_CALORIES_MALE + MIN_SAFE_CALORIES_FEMALE) / 2
        }
        val deficit = (tdee * 0.20).coerceIn(300.0, 700.0)
        return (tdee - deficit).coerceAtLeast(floor).roundToNearest(10)
    }

    /** The deficit band the UI shows as a range, e.g. 300–550 kcal below maintenance. */
    fun deficitRange(tdee: Double): IntRange {
        if (tdee <= 0.0) return 0..0
        val low = (tdee * 0.10).coerceIn(200.0, 500.0).roundToInt()
        val high = (tdee * 0.22).coerceIn(300.0, 750.0).roundToInt()
        return low..maxOf(high, low)
    }

    /** kg/week implied by a calorie deficit. */
    fun weeklyWeightChangeKg(tdee: Double, intakeCalories: Double): Double {
        if (tdee <= 0.0) return 0.0
        return (tdee - intakeCalories) * 7.0 / KCAL_PER_KG
    }

    /** Weeks to reach the target at the current estimated pace. Null when not applicable. */
    fun weeksToTarget(currentKg: Double, targetKg: Double, weeklyLossKg: Double): Int? {
        val remaining = currentKg - targetKg
        if (remaining <= 0.0 || weeklyLossKg <= 0.01) return null
        return kotlin.math.ceil(remaining / weeklyLossKg).toInt()
    }

    /** Protein target for a fat-loss phase, expressed in grams. */
    fun proteinTargetGrams(weightKg: Double): Double =
        if (weightKg <= 0.0) 0.0 else (weightKg * 1.4).roundToNearest(5)

    /**
     * Suggested daily water in ml (≈35 ml per kg), rounded to the nearest 100 ml and clamped to
     * the range the app accepts.
     */
    fun suggestedWaterMl(weightKg: Double): Int {
        if (weightKg <= 0.0) return 2500
        val raw = weightKg * 35.0
        return ((raw / 100.0).roundToInt() * 100).coerceIn(2000, 4000)
    }

    /** Average weekly change from the first and last log of a period. */
    fun averageWeeklyChangeKg(
        firstWeightKg: Double,
        lastWeightKg: Double,
        daysBetween: Long
    ): Double {
        if (daysBetween <= 0L) return 0.0
        return (lastWeightKg - firstWeightKg) / daysBetween * 7.0
    }

    private fun Double.roundToNearest(step: Int): Double {
        if (step <= 0) return this
        return (this / step).roundToInt().toDouble() * step
    }
}
