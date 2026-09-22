package com.fitbudget.app.domain

import java.time.LocalDate

/** Result of validating one input field. */
data class FieldResult(val isValid: Boolean, val message: String? = null) {
    companion object {
        val Valid = FieldResult(true)
        fun invalid(message: String) = FieldResult(false, message)
    }
}

/**
 * Central input validation. Every screen that accepts a number funnels through here so the
 * app can never persist an impossible value (and never crashes on odd input).
 */
object Validators {

    val HEIGHT_RANGE = 100.0..250.0
    val WEIGHT_RANGE = 30.0..300.0
    val BUDGET_RANGE = 1.0..10_000.0
    val WATER_TARGET_RANGE = 500..10_000
    val STEP_RANGE = 0..100_000
    val AGE_RANGE = 10..100
    val FOOD_COST_RANGE = 0.0..10_000.0
    val FOOD_CALORIE_RANGE = 0.0..5_000.0
    val FOOD_PROTEIN_RANGE = 0.0..300.0
    val QUANTITY_RANGE = 0.25..20.0
    val WATER_SINGLE_ENTRY_RANGE = 10..5_000

    fun name(value: String): FieldResult = when {
        value.isBlank() -> FieldResult.invalid("Enter your name.")
        value.trim().length > 40 -> FieldResult.invalid("Keep the name under 40 characters.")
        else -> FieldResult.Valid
    }

    fun height(cm: Double?): FieldResult = when {
        cm == null -> FieldResult.invalid("Enter your height in cm.")
        cm !in HEIGHT_RANGE -> FieldResult.invalid("Height must be between 100 and 250 cm.")
        else -> FieldResult.Valid
    }

    fun weight(kg: Double?, label: String = "Weight"): FieldResult = when {
        kg == null -> FieldResult.invalid("Enter a $label in kg.")
        kg !in WEIGHT_RANGE -> FieldResult.invalid("$label must be between 30 and 300 kg.")
        else -> FieldResult.Valid
    }

    /**
     * A weight-loss goal requires target < current. Equal values are allowed (maintenance),
     * a heavier target is rejected for this app's fat-loss framing.
     */
    fun targetWeight(targetKg: Double?, currentKg: Double?): FieldResult {
        val base = weight(targetKg, "Target weight")
        if (!base.isValid) return base
        if (currentKg == null) return FieldResult.Valid
        return when {
            targetKg!! > currentKg ->
                FieldResult.invalid("Target must be lower than or equal to your current weight.")
            currentKg - targetKg > 100 ->
                FieldResult.invalid("That gap is too large to plan safely. Pick a closer target.")
            else -> FieldResult.Valid
        }
    }

    fun budget(rupees: Double?): FieldResult = when {
        rupees == null -> FieldResult.invalid("Enter a daily budget.")
        rupees !in BUDGET_RANGE -> FieldResult.invalid("Budget must be between ₹1 and ₹10,000 a day.")
        else -> FieldResult.Valid
    }

    fun waterTarget(ml: Int?): FieldResult = when {
        ml == null -> FieldResult.invalid("Enter a water target in ml.")
        ml !in WATER_TARGET_RANGE -> FieldResult.invalid("Water target must be between 500 ml and 10 L.")
        else -> FieldResult.Valid
    }

    fun waterEntry(ml: Int?): FieldResult = when {
        ml == null -> FieldResult.invalid("Enter an amount in ml.")
        ml !in WATER_SINGLE_ENTRY_RANGE -> FieldResult.invalid("Log between 10 ml and 5,000 ml at a time.")
        else -> FieldResult.Valid
    }

    fun steps(value: Int?): FieldResult = when {
        value == null -> FieldResult.invalid("Enter a step count.")
        value !in STEP_RANGE -> FieldResult.invalid("Steps must be between 0 and 100,000.")
        else -> FieldResult.Valid
    }

    fun dateOfBirth(date: LocalDate?, today: LocalDate = LocalDate.now()): FieldResult {
        if (date == null) return FieldResult.invalid("Pick your date of birth.")
        if (date.isAfter(today)) return FieldResult.invalid("Date of birth cannot be in the future.")
        val age = HealthCalculator.age(date, today)
        return if (age in AGE_RANGE) FieldResult.Valid
        else FieldResult.invalid("Age must be between 10 and 100 years.")
    }

    fun foodName(value: String): FieldResult = when {
        value.isBlank() -> FieldResult.invalid("Enter a food name.")
        value.trim().length > 50 -> FieldResult.invalid("Keep the food name under 50 characters.")
        else -> FieldResult.Valid
    }

    fun servingLabel(value: String): FieldResult = when {
        value.isBlank() -> FieldResult.invalid("Describe the serving, e.g. \"1 bowl (150 g)\".")
        value.trim().length > 40 -> FieldResult.invalid("Keep the serving under 40 characters.")
        else -> FieldResult.Valid
    }

    fun calories(value: Double?): FieldResult = when {
        value == null -> FieldResult.invalid("Enter approximate calories.")
        value !in FOOD_CALORIE_RANGE -> FieldResult.invalid("Calories must be between 0 and 5,000.")
        else -> FieldResult.Valid
    }

    fun protein(value: Double?): FieldResult = when {
        value == null -> FieldResult.invalid("Enter approximate protein in grams.")
        value !in FOOD_PROTEIN_RANGE -> FieldResult.invalid("Protein must be between 0 and 300 g.")
        else -> FieldResult.Valid
    }

    fun cost(value: Double?): FieldResult = when {
        value == null -> FieldResult.invalid("Enter an approximate cost.")
        value !in FOOD_COST_RANGE -> FieldResult.invalid("Cost must be between ₹0 and ₹10,000.")
        else -> FieldResult.Valid
    }

    fun quantity(value: Double?): FieldResult = when {
        value == null -> FieldResult.invalid("Enter a quantity.")
        value !in QUANTITY_RANGE -> FieldResult.invalid("Quantity must be between 0.25 and 20 servings.")
        else -> FieldResult.Valid
    }

    fun reminderTime(hour: Int?, minute: Int?): FieldResult = when {
        hour == null || minute == null -> FieldResult.invalid("Pick a time.")
        hour !in 0..23 || minute !in 0..59 -> FieldResult.invalid("Pick a valid time of day.")
        else -> FieldResult.Valid
    }

    fun reminderInterval(minutes: Int?): FieldResult = when {
        minutes == null -> FieldResult.invalid("Pick a repeat interval.")
        minutes !in 15..720 -> FieldResult.invalid("Interval must be between 15 minutes and 12 hours.")
        else -> FieldResult.Valid
    }

    /** Parses user text tolerantly: strips ₹, spaces and commas before reading a number. */
    fun parseDecimal(raw: String): Double? =
        raw.trim().removePrefix("₹").replace(",", "").replace(" ", "").toDoubleOrNull()

    fun parseInt(raw: String): Int? =
        raw.trim().removePrefix("₹").replace(",", "").replace(" ", "").toIntOrNull()
            ?: parseDecimal(raw)?.let { if (it.isFinite()) it.toInt() else null }
}
