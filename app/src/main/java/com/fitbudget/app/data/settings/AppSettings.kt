package com.fitbudget.app.data.settings

import com.fitbudget.app.domain.WaterCalculator
import com.fitbudget.app.domain.model.ThemeMode
import com.fitbudget.app.domain.model.UnitSystem

/** User preferences that are not part of the health profile. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val waterTargetMl: Int = WaterCalculator.DEFAULT_TARGET_ML,
    val stepGoal: Int = DEFAULT_STEP_GOAL,
    /** Master switch; individual reminders have their own toggle too. */
    val remindersEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    /** Lower-cased food names the user never wants suggested, e.g. "boiled egg". */
    val excludedFoodKeys: Set<String> = emptySet(),
    val dynamicColorEnabled: Boolean = false,
    /** Last day a plan/rollover pass ran, used by the daily maintenance worker. */
    val lastRollOverEpochDay: Long = 0L,
    /** Persisted hardware step-counter bookkeeping. */
    val lastStepCounterValue: Long = -1L,
    val lastStepCounterEpochDay: Long = 0L
) {
    companion object {
        const val DEFAULT_STEP_GOAL = 8_000
    }
}
