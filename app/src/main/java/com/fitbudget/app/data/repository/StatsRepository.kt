package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.DayMetaDao
import com.fitbudget.app.data.database.dao.ExpenseDao
import com.fitbudget.app.data.database.dao.MealDao
import com.fitbudget.app.data.database.dao.StepDao
import com.fitbudget.app.data.database.dao.WaterDao
import com.fitbudget.app.data.database.dao.WeightDao
import com.fitbudget.app.data.database.dao.WorkoutDao
import com.fitbudget.app.data.database.entity.DayMetaEntity
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.BudgetCalculator
import com.fitbudget.app.domain.DailyChecklist
import com.fitbudget.app.domain.DaySummary
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.MonthlyBudgetSummary
import com.fitbudget.app.domain.StreakCalculator
import com.fitbudget.app.domain.Streaks
import com.fitbudget.app.domain.WaterCalculator
import com.fitbudget.app.domain.model.MealType
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import kotlin.math.roundToInt

/** Aggregated, read-only views over everything the user has logged. */
class StatsRepository(
    private val mealDao: MealDao,
    private val waterDao: WaterDao,
    private val stepDao: StepDao,
    private val workoutDao: WorkoutDao,
    private val weightDao: WeightDao,
    private val expenseDao: ExpenseDao,
    private val dayMetaDao: DayMetaDao,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) {

    /** Live summary of one day, recomputed whenever any underlying table changes. */
    fun observeSummary(epochDay: Long): Flow<DaySummary> {
        val logsFlow = combine(
            mealDao.observeForDay(epochDay),
            waterDao.observeTotalForDay(epochDay),
            stepDao.observeForDay(epochDay),
            workoutDao.observeCountForDay(epochDay),
            weightDao.observeForDay(epochDay)
        ) { meals, waterMl, stepLog, workouts, weightLog ->
            LogBundle(
                meals = meals,
                waterMl = waterMl,
                steps = stepLog?.totalSteps ?: 0,
                workouts = workouts,
                weightKg = weightLog?.weightKg
            )
        }

        return combine(
            logsFlow,
            expenseDao.observeTotalForDay(epochDay),
            dayMetaDao.observeForDay(epochDay),
            profileRepository.profile,
            settingsRepository.settings
        ) { logs, extraSpend, meta, profile, settings ->
            val budget = meta?.budget ?: profile?.dailyBudget ?: 0.0
            val waterTarget = meta?.waterTargetMl ?: settings.waterTargetMl
            val stepGoal = meta?.stepGoal ?: settings.stepGoal

            val byMeal = logs.meals.filter { it.mealType != null }.groupBy { it.mealType!! }
            val planned = byMeal.keys
            val completed = byMeal
                .filterValues { entries -> entries.isNotEmpty() && entries.all { it.completed } }
                .keys

            DaySummary(
                epochDay = epochDay,
                plannedMealTypes = planned,
                completedMealTypes = completed,
                mealItemsPlanned = logs.meals.size,
                mealItemsCompleted = logs.meals.count { it.completed },
                caloriesPlanned = logs.meals.sumOf { it.totalCalories },
                caloriesConsumed = logs.meals.filter { it.completed }.sumOf { it.totalCalories },
                proteinConsumed = logs.meals.filter { it.completed }.sumOf { it.totalProtein },
                plannedCost = logs.meals.sumOf { it.totalCost },
                spent = logs.meals.filter { it.completed }.sumOf { it.totalCost } + extraSpend,
                budget = budget,
                waterMl = logs.waterMl,
                waterTargetMl = waterTarget,
                steps = logs.steps,
                stepGoal = stepGoal,
                workoutsCompleted = logs.workouts,
                weightKg = logs.weightKg
            )
        }
    }

    private data class LogBundle(
        val meals: List<com.fitbudget.app.data.database.entity.MealEntryEntity>,
        val waterMl: Int,
        val steps: Int,
        val workouts: Int,
        val weightKg: Double?
    )

    /** One-shot summaries for a closed range of days. Days with no data are still returned. */
    suspend fun daySummaries(from: Long, to: Long): List<DaySummary> {
        if (to < from) return emptyList()

        val mealAgg = mealDao.aggregateRange(from, to).associateBy { it.epochDay }
        val mealTypeAgg = mealDao.aggregateMealTypes(from, to).groupBy { it.epochDay }
        val water = waterDao.totalsForRange(from, to).associate { it.epochDay to it.value }
        val steps = stepDao.totalsForRange(from, to).associate { it.epochDay to it.value }
        val workouts = workoutDao.countsForRange(from, to).associate { it.epochDay to it.value }
        val weights = weightDao.getRange(from, to).associate { it.epochDay to it.weightKg }
        val expenses = expenseDao.totalsForRange(from, to).associate { it.epochDay to it.value }
        val metas = dayMetaDao.getRange(from, to).associateBy { it.epochDay }

        val profile = profileRepository.get()
        val settings = settingsRepository.current()
        val fallbackBudget = profile?.dailyBudget ?: 0.0

        return (from..to).map { day ->
            val meta: DayMetaEntity? = metas[day]
            val meals = mealAgg[day]
            val types = mealTypeAgg[day].orEmpty()
            val completedCost = meals?.consumedCost ?: 0.0
            DaySummary(
                epochDay = day,
                plannedMealTypes = types.mapNotNull { it.mealType }.toSet(),
                completedMealTypes = types.filter { it.fullyCompleted }.mapNotNull { it.mealType }.toSet(),
                mealItemsPlanned = meals?.itemCount ?: 0,
                mealItemsCompleted = meals?.completedCount ?: 0,
                caloriesPlanned = meals?.plannedCalories ?: 0.0,
                caloriesConsumed = meals?.consumedCalories ?: 0.0,
                proteinConsumed = meals?.consumedProtein ?: 0.0,
                plannedCost = meals?.plannedCost ?: 0.0,
                spent = completedCost + (expenses[day] ?: 0.0),
                budget = meta?.budget ?: fallbackBudget,
                waterMl = water[day] ?: 0,
                waterTargetMl = meta?.waterTargetMl ?: settings.waterTargetMl,
                steps = steps[day] ?: 0,
                stepGoal = meta?.stepGoal ?: settings.stepGoal,
                workoutsCompleted = workouts[day] ?: 0,
                weightKg = weights[day]
            )
        }
    }

    suspend fun summaryForDay(epochDay: Long): DaySummary =
        daySummaries(epochDay, epochDay).firstOrNull() ?: DaySummary(epochDay)

    /** Streaks computed over the last [lookBackDays] days of real logs. */
    suspend fun streaks(lookBackDays: Int = 400): Streaks {
        val today = DateTimeUtils.todayEpochDay()
        val summaries = daySummaries(today - lookBackDays, today)
        return StreakCalculator.compute(summaries, today)
    }

    suspend fun monthlyReport(month: YearMonth): MonthlyReport {
        val range = DateTimeUtils.monthRange(month)
        val summaries = daySummaries(range.first, range.last)
        val active = summaries.filter { it.hasAnyActivity }

        val weightPoints = summaries.mapNotNull { summary ->
            summary.weightKg?.let { summary.epochDay to it }
        }
        val calorieDays = summaries.filter { it.caloriesConsumed > 0 }
        val costDays = summaries.filter { it.spent > 0 }
        val stepDays = summaries.filter { it.steps > 0 }
        val waterDays = summaries.filter { it.waterMl > 0 }
        val profile = profileRepository.get()

        val averageWeight = weightPoints.takeIf { it.isNotEmpty() }
            ?.map { it.second }?.average()
        val weightChange = if (weightPoints.size >= 2) {
            weightPoints.last().second - weightPoints.first().second
        } else {
            null
        }
        val weeklyChange = if (weightPoints.size >= 2) {
            HealthCalculator.averageWeeklyChangeKg(
                firstWeightKg = weightPoints.first().second,
                lastWeightKg = weightPoints.last().second,
                daysBetween = weightPoints.last().first - weightPoints.first().first
            )
        } else {
            null
        }

        return MonthlyReport(
            month = month,
            daysWithData = active.size,
            daysInMonth = summaries.size,
            averageWeightKg = averageWeight,
            weightChangeKg = weightChange,
            averageWeeklyChangeKg = weeklyChange,
            startWeightKg = weightPoints.firstOrNull()?.second,
            endWeightKg = weightPoints.lastOrNull()?.second,
            targetWeightKg = profile?.targetWeightKg,
            averageCalories = if (calorieDays.isEmpty()) 0.0 else calorieDays.sumOf { it.caloriesConsumed } / calorieDays.size,
            averageFoodCost = if (costDays.isEmpty()) 0.0 else costDays.sumOf { it.spent } / costDays.size,
            budget = BudgetCalculator.monthly(summaries),
            workoutCount = summaries.sumOf { it.workoutsCompleted },
            averageSteps = if (stepDays.isEmpty()) 0 else stepDays.sumOf { it.steps } / stepDays.size,
            averageWaterMl = if (waterDays.isEmpty()) 0 else waterDays.sumOf { it.waterMl } / waterDays.size,
            waterAdherenceFraction = WaterCalculator.adherenceFraction(
                dailyTotals = active.map { it.waterMl },
                targetMl = active.firstOrNull()?.waterTargetMl ?: 0
            ),
            averageCompletionPercent = if (active.isEmpty()) 0
            else active.sumOf { DailyChecklist.completionPercent(it) } / active.size,
            streaks = StreakCalculator.compute(summaries, DateTimeUtils.todayEpochDay()),
            weightSeries = weightPoints,
            spendSeries = summaries.map { it.epochDay to it.spent },
            stepSeries = summaries.map { it.epochDay to it.steps },
            completionSeries = summaries.map { it.epochDay to DailyChecklist.completionPercent(it) },
            mealsCompleted = summaries.sumOf { it.mealItemsCompleted },
            daysUnderBudget = summaries.count { it.budgetGoalMet }
        )
    }
}

data class MonthlyReport(
    val month: YearMonth,
    val daysWithData: Int,
    val daysInMonth: Int,
    val averageWeightKg: Double?,
    val weightChangeKg: Double?,
    val averageWeeklyChangeKg: Double?,
    val startWeightKg: Double?,
    val endWeightKg: Double?,
    val targetWeightKg: Double?,
    val averageCalories: Double,
    val averageFoodCost: Double,
    val budget: MonthlyBudgetSummary,
    val workoutCount: Int,
    val averageSteps: Int,
    val averageWaterMl: Int,
    val waterAdherenceFraction: Double,
    val averageCompletionPercent: Int,
    val streaks: Streaks,
    val weightSeries: List<Pair<Long, Double>>,
    val spendSeries: List<Pair<Long, Double>>,
    val stepSeries: List<Pair<Long, Int>>,
    val completionSeries: List<Pair<Long, Int>>,
    val mealsCompleted: Int,
    val daysUnderBudget: Int
) {
    val hasData: Boolean get() = daysWithData > 0
    val waterAdherencePercent: Int get() = (waterAdherenceFraction * 100).roundToInt()

    /** Plain-text report used by the share / export action. */
    fun toShareText(): String = buildString {
        appendLine("FitBudget — ${DateTimeUtils.formatMonth(month)}")
        appendLine("₹100 a Day. Better Every Day.")
        appendLine()
        appendLine("Days tracked: $daysWithData / $daysInMonth")
        averageWeightKg?.let { appendLine("Average weight: %.1f kg".format(it)) }
        weightChangeKg?.let { appendLine("Weight change: %+.1f kg".format(it)) }
        averageWeeklyChangeKg?.let { appendLine("Average weekly change: %+.2f kg".format(it)) }
        targetWeightKg?.let { appendLine("Target weight: %.1f kg".format(it)) }
        appendLine("Average calories eaten: ${averageCalories.roundToInt()} kcal")
        appendLine("Average food spend: ₹%.0f".format(averageFoodCost))
        appendLine("Budget adherence: ${budget.adherencePercent}% (${budget.daysUnderBudget} under, ${budget.daysOverBudget} over)")
        appendLine("Total spent: ₹%.0f of ₹%.0f".format(budget.totalSpent, budget.totalBudget))
        appendLine("Workouts finished: $workoutCount")
        appendLine("Average steps: $averageSteps")
        appendLine("Average water: $averageWaterMl ml (target met on $waterAdherencePercent% of tracked days)")
        appendLine("Average daily checklist: $averageCompletionPercent%")
        appendLine()
        appendLine("Current streaks — diet ${streaks.diet}d, workout ${streaks.workout}d, water ${streaks.water}d, budget ${streaks.budget}d")
        appendLine()
        appendLine("All values are estimates from data logged on this device.")
        appendLine("Nutrition estimates are approximate. For medical conditions or special dietary needs, consult a qualified healthcare professional.")
    }
}
