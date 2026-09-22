package com.fitbudget.app.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.fitbudget.app.data.database.FitBudgetDatabase
import com.fitbudget.app.data.database.entity.DayMetaEntity
import com.fitbudget.app.data.database.entity.ExpenseEntity
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.data.database.entity.StepLogEntity
import com.fitbudget.app.data.database.entity.WaterLogEntity
import com.fitbudget.app.data.database.entity.WeightLogEntity
import com.fitbudget.app.data.database.entity.WorkoutSessionEntity
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.FoodRole
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.domain.model.MealType
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.domain.model.ThemeMode
import com.fitbudget.app.domain.model.UnitSystem
import com.fitbudget.app.domain.model.WorkoutCategory
import com.fitbudget.app.util.DateTimeUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.YearMonth

data class RestoreSummary(
    val weightLogs: Int,
    val mealEntries: Int,
    val waterLogs: Int,
    val workouts: Int,
    val stepDays: Int,
    val expenses: Int,
    val customFoods: Int
)

/**
 * Local export / backup / restore / reset.
 *
 * Nothing here talks to a network. Files are written into the app cache and handed to the system
 * share sheet through a FileProvider, so data only leaves the device if the user explicitly
 * shares it.
 */
class BackupRepository(
    private val context: Context,
    private val database: FitBudgetDatabase,
    private val settingsRepository: SettingsRepository,
    private val statsRepository: StatsRepository,
    private val foodRepository: FoodRepository,
    private val reminderRepository: ReminderRepository
) {

    private val exportDir: File
        get() = File(context.cacheDir, "exports").apply { mkdirs() }

    // ------------------------------------------------------------------ reports

    suspend fun writeMonthlyReport(month: YearMonth): File {
        val report = statsRepository.monthlyReport(month)
        val file = File(exportDir, "FitBudget-report-$month.txt")
        file.writeText(report.toShareText())
        return file
    }

    /** Human readable CSV with one section per tracked data type. */
    suspend fun writeCsvExport(): File {
        val builder = StringBuilder()
        builder.appendLine("FitBudget data export")
        builder.appendLine("Generated,${DateTimeUtils.formatFullDate(DateTimeUtils.today())}")
        builder.appendLine()

        database.profileDao().get()?.let { profile ->
            builder.appendLine("[Profile]")
            builder.appendLine("Name,DateOfBirth,Gender,HeightCm,StartWeightKg,CurrentWeightKg,TargetWeightKg,DailyBudget,ActivityLevel,DietPreference")
            builder.appendLine(
                listOf(
                    profile.name.csv(),
                    profile.dateOfBirth.toString(),
                    profile.gender.name,
                    profile.heightCm,
                    profile.startWeightKg,
                    profile.currentWeightKg,
                    profile.targetWeightKg,
                    profile.dailyBudget,
                    profile.activityLevel.name,
                    profile.dietPreference.name
                ).joinToString(",")
            )
            builder.appendLine()
        }

        builder.appendLine("[Weight log]")
        builder.appendLine("Date,WeightKg,Note")
        database.weightDao().getAll().forEach {
            builder.appendLine("${DateTimeUtils.date(it.epochDay)},${it.weightKg},${it.note.orEmpty().csv()}")
        }
        builder.appendLine()

        builder.appendLine("[Meals]")
        builder.appendLine("Date,Meal,Food,Servings,Calories,ProteinG,CostRupees,Completed")
        database.mealDao().getRange(0, Long.MAX_VALUE / 2).forEach {
            builder.appendLine(
                listOf(
                    DateTimeUtils.date(it.epochDay),
                    it.mealLabel.csv(),
                    it.foodName.csv(),
                    it.quantity,
                    "%.0f".format(it.totalCalories),
                    "%.1f".format(it.totalProtein),
                    "%.2f".format(it.totalCost),
                    it.completed
                ).joinToString(",")
            )
        }
        builder.appendLine()

        builder.appendLine("[Water]")
        builder.appendLine("Date,AmountMl")
        database.waterDao().getAll().forEach {
            builder.appendLine("${DateTimeUtils.date(it.epochDay)},${it.amountMl}")
        }
        builder.appendLine()

        builder.appendLine("[Steps]")
        builder.appendLine("Date,SensorSteps,ManualSteps,Total")
        database.stepDao().getAll().forEach {
            builder.appendLine("${DateTimeUtils.date(it.epochDay)},${it.sensorSteps},${it.manualSteps},${it.totalSteps}")
        }
        builder.appendLine()

        builder.appendLine("[Workouts]")
        builder.appendLine("Date,Workout,Category,DurationSeconds,Completed,Skipped,Total,EstimatedCalories")
        database.workoutDao().getAll().forEach {
            builder.appendLine(
                listOf(
                    DateTimeUtils.date(it.epochDay),
                    it.templateName.csv(),
                    it.category.name,
                    it.durationSeconds,
                    it.exercisesCompleted,
                    it.exercisesSkipped,
                    it.exercisesTotal,
                    it.estimatedCalories
                ).joinToString(",")
            )
        }
        builder.appendLine()

        builder.appendLine("[Extra expenses]")
        builder.appendLine("Date,Label,Amount")
        database.expenseDao().getAll().forEach {
            builder.appendLine("${DateTimeUtils.date(it.epochDay)},${it.label.csv()},${it.amount}")
        }
        builder.appendLine()
        builder.appendLine("All nutrition and calorie values are estimates.")

        val file = File(exportDir, "FitBudget-export-${DateTimeUtils.todayEpochDay()}.csv")
        file.writeText(builder.toString())
        return file
    }

    private fun String.csv(): String =
        if (contains(',') || contains('"')) "\"${replace("\"", "\"\"")}\"" else this

    // ------------------------------------------------------------------ backup

    suspend fun writeBackup(): File {
        val json = buildBackupJson()
        val file = File(exportDir, "FitBudget-backup-${DateTimeUtils.todayEpochDay()}.json")
        file.writeText(json.toString(2))
        return file
    }

    private suspend fun buildBackupJson(): JSONObject {
        val root = JSONObject()
        root.put("format", BACKUP_FORMAT)
        root.put("appVersion", 1)
        root.put("exportedAtMillis", DateTimeUtils.nowMillis())

        database.profileDao().get()?.let { profile ->
            root.put(
                "profile",
                JSONObject().apply {
                    put("name", profile.name)
                    put("dobEpochDay", profile.dobEpochDay)
                    put("gender", profile.gender.name)
                    put("heightCm", profile.heightCm)
                    put("startWeightKg", profile.startWeightKg)
                    put("currentWeightKg", profile.currentWeightKg)
                    put("targetWeightKg", profile.targetWeightKg)
                    put("dailyBudget", profile.dailyBudget)
                    put("activityLevel", profile.activityLevel.name)
                    put("wakeMinutes", profile.wakeMinutes)
                    put("sleepMinutes", profile.sleepMinutes)
                    put("dietPreference", profile.dietPreference.name)
                    put("calorieTargetOverride", profile.calorieTargetOverride ?: JSONObject.NULL)
                    put("onboardingComplete", profile.onboardingComplete)
                }
            )
        }

        root.put("customFoods", database.foodDao().getAll().filter { it.isCustom }.toJsonArray { food ->
            JSONObject().apply {
                put("name", food.name)
                put("servingLabel", food.servingLabel)
                put("calories", food.calories)
                put("proteinG", food.proteinG)
                put("carbsG", food.carbsG)
                put("fatG", food.fatG)
                put("costRupees", food.costRupees)
                put("category", food.category.name)
                put("role", food.role.name)
            }
        })

        root.put("weightLogs", database.weightDao().getAll().toJsonArray { log ->
            JSONObject().apply {
                put("epochDay", log.epochDay)
                put("weightKg", log.weightKg)
                put("note", log.note ?: JSONObject.NULL)
            }
        })

        root.put("mealEntries", database.mealDao().getRange(0, MAX_DAY).toJsonArray { entry ->
            JSONObject().apply {
                put("epochDay", entry.epochDay)
                put("mealType", entry.mealType?.name ?: JSONObject.NULL)
                put("mealLabel", entry.mealLabel)
                put("timeMinutes", entry.timeMinutes)
                put("foodName", entry.foodName)
                put("servingLabel", entry.servingLabel)
                put("quantity", entry.quantity)
                put("caloriesPerServing", entry.caloriesPerServing)
                put("proteinPerServing", entry.proteinPerServing)
                put("costPerServing", entry.costPerServing)
                put("completed", entry.completed)
                put("sortOrder", entry.sortOrder)
            }
        })

        root.put("waterLogs", database.waterDao().getAll().toJsonArray { log ->
            JSONObject().apply {
                put("epochDay", log.epochDay)
                put("amountMl", log.amountMl)
                put("loggedAtMillis", log.loggedAtMillis)
            }
        })

        root.put("stepLogs", database.stepDao().getAll().toJsonArray { log ->
            JSONObject().apply {
                put("epochDay", log.epochDay)
                put("sensorSteps", log.sensorSteps)
                put("manualSteps", log.manualSteps)
            }
        })

        root.put("workouts", database.workoutDao().getAll().toJsonArray { session ->
            JSONObject().apply {
                put("epochDay", session.epochDay)
                put("templateId", session.templateId)
                put("templateName", session.templateName)
                put("category", session.category.name)
                put("startedAtMillis", session.startedAtMillis)
                put("finishedAtMillis", session.finishedAtMillis)
                put("durationSeconds", session.durationSeconds)
                put("exercisesCompleted", session.exercisesCompleted)
                put("exercisesSkipped", session.exercisesSkipped)
                put("exercisesTotal", session.exercisesTotal)
                put("estimatedCalories", session.estimatedCalories)
                put("note", session.note ?: JSONObject.NULL)
            }
        })

        root.put("expenses", database.expenseDao().getAll().toJsonArray { expense ->
            JSONObject().apply {
                put("epochDay", expense.epochDay)
                put("label", expense.label)
                put("amount", expense.amount)
            }
        })

        root.put("dayMeta", database.dayMetaDao().getAll().toJsonArray { meta ->
            JSONObject().apply {
                put("epochDay", meta.epochDay)
                put("budget", meta.budget)
                put("waterTargetMl", meta.waterTargetMl)
                put("stepGoal", meta.stepGoal)
                put("calorieTarget", meta.calorieTarget)
                put("planGenerated", meta.planGenerated)
            }
        })

        root.put("reminders", database.reminderDao().getAll().toJsonArray { reminder ->
            JSONObject().apply {
                put("type", reminder.type.name)
                put("enabled", reminder.enabled)
                put("hour", reminder.hour)
                put("minute", reminder.minute)
                put("daysMask", reminder.daysMask)
                put("intervalMinutes", reminder.intervalMinutes)
                put("soundEnabled", reminder.soundEnabled)
                put("vibrationEnabled", reminder.vibrationEnabled)
            }
        })

        val settings = settingsRepository.current()
        root.put(
            "settings",
            JSONObject().apply {
                put("themeMode", settings.themeMode.name)
                put("unitSystem", settings.unitSystem.name)
                put("waterTargetMl", settings.waterTargetMl)
                put("stepGoal", settings.stepGoal)
                put("remindersEnabled", settings.remindersEnabled)
                put("notificationSoundEnabled", settings.notificationSoundEnabled)
                put("notificationVibrationEnabled", settings.notificationVibrationEnabled)
                put("dynamicColorEnabled", settings.dynamicColorEnabled)
                put("excludedFoodKeys", JSONArray(settings.excludedFoodKeys.toList()))
            }
        )
        return root
    }

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        forEach { array.put(mapper(it)) }
        return array
    }

    /**
     * Replaces the local database with the contents of a backup file. Validated before anything is
     * deleted, so a malformed file leaves existing data untouched.
     */
    suspend fun restoreBackup(content: String): Result<RestoreSummary> = try {
        val root = JSONObject(content)
        require(root.optString("format") == BACKUP_FORMAT) {
            "This file is not a FitBudget backup."
        }

        val now = DateTimeUtils.nowMillis()

        val profile = root.optJSONObject("profile")?.let { json ->
            ProfileEntity(
                name = json.optString("name"),
                dobEpochDay = json.optLong("dobEpochDay", ProfileEntity.DEFAULT_DOB_EPOCH_DAY),
                gender = Gender.fromName(json.optString("gender")),
                heightCm = json.optDouble("heightCm", 172.0),
                startWeightKg = json.optDouble("startWeightKg", 85.0),
                currentWeightKg = json.optDouble("currentWeightKg", 85.0),
                targetWeightKg = json.optDouble("targetWeightKg", 75.0),
                dailyBudget = json.optDouble("dailyBudget", 100.0),
                activityLevel = ActivityLevel.fromName(json.optString("activityLevel")),
                wakeMinutes = json.optInt("wakeMinutes", 6 * 60 + 30),
                sleepMinutes = json.optInt("sleepMinutes", 22 * 60 + 30),
                dietPreference = DietPreference.fromName(json.optString("dietPreference")),
                calorieTargetOverride = json.optDouble("calorieTargetOverride")
                    .takeIf { !it.isNaN() && it > 0 },
                onboardingComplete = json.optBoolean("onboardingComplete", true),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        }

        val customFoods = root.optJSONArray("customFoods").mapObjects { json ->
            val name = json.optString("name")
            FoodEntity(
                name = name,
                nameKey = name.trim().lowercase(),
                servingLabel = json.optString("servingLabel", "1 serving"),
                calories = json.optDouble("calories", 0.0),
                proteinG = json.optDouble("proteinG", 0.0),
                carbsG = json.optDouble("carbsG", 0.0),
                fatG = json.optDouble("fatG", 0.0),
                costRupees = json.optDouble("costRupees", 0.0),
                category = FoodCategory.fromName(json.optString("category")),
                role = FoodRole.fromName(json.optString("role")),
                isCustom = true,
                createdAtMillis = now
            )
        }.filter { it.name.isNotBlank() }

        val weightLogs = root.optJSONArray("weightLogs").mapObjects { json ->
            WeightLogEntity(
                epochDay = json.optLong("epochDay"),
                weightKg = json.optDouble("weightKg", 0.0),
                note = json.optStringOrNull("note"),
                createdAtMillis = now
            )
        }.filter { it.weightKg > 0 }

        val mealEntries = root.optJSONArray("mealEntries").mapObjects { json ->
            MealEntryEntity(
                epochDay = json.optLong("epochDay"),
                mealType = json.optStringOrNull("mealType")
                    ?.let { name -> MealType.entries.firstOrNull { it.name == name } },
                mealLabel = json.optString("mealLabel", "Meal"),
                timeMinutes = json.optInt("timeMinutes", 8 * 60),
                foodName = json.optString("foodName", "Food"),
                servingLabel = json.optString("servingLabel", "1 serving"),
                quantity = json.optDouble("quantity", 1.0),
                caloriesPerServing = json.optDouble("caloriesPerServing", 0.0),
                proteinPerServing = json.optDouble("proteinPerServing", 0.0),
                costPerServing = json.optDouble("costPerServing", 0.0),
                completed = json.optBoolean("completed", false),
                sortOrder = json.optInt("sortOrder", 0)
            )
        }

        val waterLogs = root.optJSONArray("waterLogs").mapObjects { json ->
            WaterLogEntity(
                epochDay = json.optLong("epochDay"),
                amountMl = json.optInt("amountMl"),
                loggedAtMillis = json.optLong("loggedAtMillis", now)
            )
        }.filter { it.amountMl > 0 }

        val stepLogs = root.optJSONArray("stepLogs").mapObjects { json ->
            StepLogEntity(
                epochDay = json.optLong("epochDay"),
                sensorSteps = json.optInt("sensorSteps"),
                manualSteps = json.optInt("manualSteps"),
                updatedAtMillis = now
            )
        }

        val workouts = root.optJSONArray("workouts").mapObjects { json ->
            WorkoutSessionEntity(
                epochDay = json.optLong("epochDay"),
                templateId = json.optString("templateId"),
                templateName = json.optString("templateName", "Workout"),
                category = WorkoutCategory.fromName(json.optString("category")),
                startedAtMillis = json.optLong("startedAtMillis", now),
                finishedAtMillis = json.optLong("finishedAtMillis", now),
                durationSeconds = json.optInt("durationSeconds"),
                exercisesCompleted = json.optInt("exercisesCompleted"),
                exercisesSkipped = json.optInt("exercisesSkipped"),
                exercisesTotal = json.optInt("exercisesTotal"),
                estimatedCalories = json.optInt("estimatedCalories"),
                note = json.optStringOrNull("note")
            )
        }

        val expenses = root.optJSONArray("expenses").mapObjects { json ->
            ExpenseEntity(
                epochDay = json.optLong("epochDay"),
                label = json.optString("label", "Extra"),
                amount = json.optDouble("amount", 0.0),
                createdAtMillis = now
            )
        }

        val dayMeta = root.optJSONArray("dayMeta").mapObjects { json ->
            DayMetaEntity(
                epochDay = json.optLong("epochDay"),
                budget = json.optDouble("budget", 100.0),
                waterTargetMl = json.optInt("waterTargetMl", 2500),
                stepGoal = json.optInt("stepGoal", 8000),
                calorieTarget = json.optDouble("calorieTarget", 0.0),
                planGenerated = json.optBoolean("planGenerated", true),
                createdAtMillis = now
            )
        }

        val reminders = root.optJSONArray("reminders").mapObjects { json ->
            val type = ReminderType.fromName(json.optString("type"))
            ReminderEntity(
                type = type,
                enabled = json.optBoolean("enabled", true),
                hour = json.optInt("hour", type.defaultHour),
                minute = json.optInt("minute", type.defaultMinute),
                daysMask = json.optInt("daysMask", DateTimeUtils.ALL_DAYS_MASK),
                intervalMinutes = json.optInt("intervalMinutes", type.defaultIntervalMinutes),
                soundEnabled = json.optBoolean("soundEnabled", true),
                vibrationEnabled = json.optBoolean("vibrationEnabled", true)
            )
        }

        // ---- everything parsed successfully, now swap the data in ----
        database.withTransactionCompat {
            database.weightDao().clear()
            database.mealDao().clear()
            database.waterDao().clear()
            database.stepDao().clear()
            database.workoutDao().clear()
            database.expenseDao().clear()
            database.dayMetaDao().clear()

            profile?.let { database.profileDao().upsert(it) }
            database.foodDao().insertAllIgnoringDuplicates(customFoods)
            database.weightDao().upsertAll(weightLogs)
            database.mealDao().insertAll(mealEntries)
            database.waterDao().insertAll(waterLogs)
            database.stepDao().insertAll(stepLogs)
            database.workoutDao().insertAll(workouts)
            database.expenseDao().insertAll(expenses)
            database.dayMetaDao().insertAll(dayMeta)
            if (reminders.isNotEmpty()) database.reminderDao().upsertAll(reminders)
        }

        root.optJSONObject("settings")?.let { json ->
            settingsRepository.setThemeMode(ThemeMode.fromName(json.optString("themeMode")))
            settingsRepository.setUnitSystem(UnitSystem.fromName(json.optString("unitSystem")))
            settingsRepository.setWaterTarget(json.optInt("waterTargetMl", 2500))
            settingsRepository.setStepGoal(json.optInt("stepGoal", 8000))
            settingsRepository.setRemindersEnabled(json.optBoolean("remindersEnabled", true))
            settingsRepository.setNotificationSound(json.optBoolean("notificationSoundEnabled", true))
            settingsRepository.setNotificationVibration(json.optBoolean("notificationVibrationEnabled", true))
            settingsRepository.setDynamicColor(json.optBoolean("dynamicColorEnabled", false))
            val excluded = json.optJSONArray("excludedFoodKeys")
            val keys = buildSet {
                for (index in 0 until (excluded?.length() ?: 0)) {
                    excluded?.optString(index)?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
            settingsRepository.setExcludedFoods(keys)
        }

        reminderRepository.rescheduleAll()

        Result.success(
            RestoreSummary(
                weightLogs = weightLogs.size,
                mealEntries = mealEntries.size,
                waterLogs = waterLogs.size,
                workouts = workouts.size,
                stepDays = stepLogs.size,
                expenses = expenses.size,
                customFoods = customFoods.size
            )
        )
    } catch (error: Exception) {
        Log.e(TAG, "Restore failed", error)
        Result.failure(error)
    }

    // ------------------------------------------------------------------ reset

    /** Destructive: wipes every local table and preference, then re-seeds defaults. */
    suspend fun resetAllData() {
        reminderRepository.cancelAll()
        database.withTransactionCompat {
            database.weightDao().clear()
            database.mealDao().clear()
            database.waterDao().clear()
            database.stepDao().clear()
            database.workoutDao().clear()
            database.expenseDao().clear()
            database.dayMetaDao().clear()
            database.reminderDao().clear()
            database.foodDao().clear()
            database.profileDao().clear()
        }
        settingsRepository.clear()
        foodRepository.seedIfEmpty()
        reminderRepository.ensureDefaults()
        exportDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    private companion object {
        const val TAG = "BackupRepository"
        const val BACKUP_FORMAT = "fitbudget-backup-v1"
        const val MAX_DAY = 4_000_000L
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun <T> JSONArray?.mapObjects(mapper: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val result = mutableListOf<T>()
    for (index in 0 until length()) {
        optJSONObject(index)?.let { result += mapper(it) }
    }
    return result
}

/** Small wrapper so the restore/reset paths are always atomic. */
private suspend fun FitBudgetDatabase.withTransactionCompat(block: suspend () -> Unit) {
    withTransaction { block() }
}
