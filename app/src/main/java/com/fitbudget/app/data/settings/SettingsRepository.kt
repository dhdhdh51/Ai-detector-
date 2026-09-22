package com.fitbudget.app.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.WaterCalculator
import com.fitbudget.app.domain.model.ThemeMode
import com.fitbudget.app.domain.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fitbudget_settings")

/**
 * DataStore-backed settings. Reads fall back to defaults when the file is missing or corrupt so
 * the app always starts, and writes are safe to call from any coroutine.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val UNITS = stringPreferencesKey("unit_system")
        val WATER_TARGET = intPreferencesKey("water_target_ml")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val SOUND = booleanPreferencesKey("notification_sound")
        val VIBRATION = booleanPreferencesKey("notification_vibration")
        val EXCLUDED_FOODS = stringSetPreferencesKey("excluded_foods")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LAST_ROLLOVER = longPreferencesKey("last_rollover_day")
        val STEP_COUNTER_VALUE = longPreferencesKey("step_counter_value")
        val STEP_COUNTER_DAY = longPreferencesKey("step_counter_day")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Settings unreadable, falling back to defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { prefs -> prefs.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings() = AppSettings(
        themeMode = ThemeMode.fromName(this[Keys.THEME]),
        unitSystem = UnitSystem.fromName(this[Keys.UNITS]),
        waterTargetMl = (this[Keys.WATER_TARGET] ?: WaterCalculator.DEFAULT_TARGET_ML)
            .coerceIn(Validators.WATER_TARGET_RANGE.first, Validators.WATER_TARGET_RANGE.last),
        stepGoal = (this[Keys.STEP_GOAL] ?: AppSettings.DEFAULT_STEP_GOAL)
            .coerceIn(Validators.STEP_RANGE.first, Validators.STEP_RANGE.last),
        remindersEnabled = this[Keys.REMINDERS_ENABLED] ?: true,
        notificationSoundEnabled = this[Keys.SOUND] ?: true,
        notificationVibrationEnabled = this[Keys.VIBRATION] ?: true,
        excludedFoodKeys = this[Keys.EXCLUDED_FOODS] ?: emptySet(),
        dynamicColorEnabled = this[Keys.DYNAMIC_COLOR] ?: false,
        lastRollOverEpochDay = this[Keys.LAST_ROLLOVER] ?: 0L,
        lastStepCounterValue = this[Keys.STEP_COUNTER_VALUE] ?: -1L,
        lastStepCounterEpochDay = this[Keys.STEP_COUNTER_DAY] ?: 0L
    )

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            context.settingsDataStore.edit(block)
        } catch (io: IOException) {
            Log.e(TAG, "Unable to persist settings", io)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    suspend fun setUnitSystem(units: UnitSystem) = edit { it[Keys.UNITS] = units.name }

    suspend fun setWaterTarget(ml: Int) = edit {
        it[Keys.WATER_TARGET] = ml.coerceIn(
            Validators.WATER_TARGET_RANGE.first,
            Validators.WATER_TARGET_RANGE.last
        )
    }

    suspend fun setStepGoal(goal: Int) = edit {
        it[Keys.STEP_GOAL] = goal.coerceIn(Validators.STEP_RANGE.first, Validators.STEP_RANGE.last)
    }

    suspend fun setRemindersEnabled(enabled: Boolean) = edit { it[Keys.REMINDERS_ENABLED] = enabled }

    suspend fun setNotificationSound(enabled: Boolean) = edit { it[Keys.SOUND] = enabled }

    suspend fun setNotificationVibration(enabled: Boolean) = edit { it[Keys.VIBRATION] = enabled }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setExcludedFoods(keys: Set<String>) = edit {
        it[Keys.EXCLUDED_FOODS] = keys.map { key -> key.trim().lowercase() }.filter { key -> key.isNotEmpty() }.toSet()
    }

    suspend fun toggleExcludedFood(nameKey: String) {
        val key = nameKey.trim().lowercase()
        if (key.isEmpty()) return
        val existing = current().excludedFoodKeys
        setExcludedFoods(if (key in existing) existing - key else existing + key)
    }

    suspend fun setLastRollOverDay(epochDay: Long) = edit { it[Keys.LAST_ROLLOVER] = epochDay }

    suspend fun setStepCounterState(counterValue: Long, epochDay: Long) = edit {
        it[Keys.STEP_COUNTER_VALUE] = counterValue
        it[Keys.STEP_COUNTER_DAY] = epochDay
    }

    /** Wipes preferences back to defaults (used by "Reset all data"). */
    suspend fun clear() = edit { it.clear() }

    private companion object {
        const val TAG = "SettingsRepository"
    }
}
