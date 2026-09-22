package com.fitbudget.app.util

import com.fitbudget.app.domain.model.UnitSystem
import kotlin.math.abs
import kotlin.math.roundToInt

object Formatters {

    fun rupees(amount: Double): String =
        if (abs(amount - amount.roundToInt()) < 0.005) "₹${amount.roundToInt()}"
        else "₹%.2f".format(amount)

    fun rupees(amount: Int): String = "₹$amount"

    fun kg(value: Double): String = "%.1f kg".format(value)

    fun weight(valueKg: Double, units: UnitSystem): String = when (units) {
        UnitSystem.METRIC -> "%.1f kg".format(valueKg)
        UnitSystem.IMPERIAL -> "%.1f lb".format(valueKg * 2.2046226)
    }

    fun height(valueCm: Double, units: UnitSystem): String = when (units) {
        UnitSystem.METRIC -> "${valueCm.roundToInt()} cm"
        UnitSystem.IMPERIAL -> {
            val totalInches = valueCm / 2.54
            val feet = (totalInches / 12).toInt()
            val inches = (totalInches - feet * 12).roundToInt()
            if (inches == 12) "${feet + 1}' 0\"" else "$feet' $inches\""
        }
    }

    fun signedKg(value: Double): String = when {
        value > 0.05 -> "+%.1f kg".format(value)
        value < -0.05 -> "%.1f kg".format(value)
        else -> "0.0 kg"
    }

    fun litres(ml: Int): String =
        if (ml >= 1000) "%.2f L".format(ml / 1000.0) else "$ml ml"

    fun ml(ml: Int): String = "$ml ml"

    fun calories(value: Double): String = "${value.roundToInt()} kcal"

    fun grams(value: Double): String = "%.0f g".format(value)

    fun percent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

    fun steps(value: Int): String = "%,d".format(value)

    fun quantity(value: Double): String =
        if (abs(value - value.roundToInt()) < 0.01) value.roundToInt().toString() else "%.2f".format(value)

    fun duration(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val minutes = safe / 60
        val remaining = safe % 60
        return if (minutes >= 60) {
            "%dh %02dm".format(minutes / 60, minutes % 60)
        } else {
            "%d:%02d".format(minutes, remaining)
        }
    }
}
