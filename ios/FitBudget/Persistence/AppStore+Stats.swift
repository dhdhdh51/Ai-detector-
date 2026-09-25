import Foundation
import FitBudgetCore

/// A full month of aggregated, logged data.
struct MonthlyReport {
    let monthDay: DayKey
    let daysWithData: Int
    let daysInMonth: Int
    let averageWeightKg: Double?
    let weightChangeKg: Double?
    let averageWeeklyChangeKg: Double?
    let startWeightKg: Double?
    let endWeightKg: Double?
    let targetWeightKg: Double?
    let averageCalories: Double
    let averageFoodCost: Double
    let budget: MonthlyBudgetSummary
    let workoutCount: Int
    let averageSteps: Int
    let averageWaterMl: Int
    let waterAdherenceFraction: Double
    let averageCompletionPercent: Int
    let streaks: Streaks
    let weightSeries: [(day: DayKey, value: Double)]
    let spendSeries: [(day: DayKey, value: Double)]
    let stepSeries: [(day: DayKey, value: Int)]
    let completionSeries: [(day: DayKey, value: Int)]
    let mealsCompleted: Int

    var hasData: Bool { daysWithData > 0 }
    var waterAdherencePercent: Int { Int((waterAdherenceFraction * 100).rounded()) }
    var title: String { Formatters.monthTitle(monthDay) }

    /// Plain-text report used by the share / export action.
    func shareText() -> String {
        var lines: [String] = []
        lines.append("FitBudget — \(title)")
        lines.append("₹100 a Day. Better Every Day.")
        lines.append("")
        lines.append("Days tracked: \(daysWithData) / \(daysInMonth)")
        if let averageWeightKg {
            lines.append(String(format: "Average weight: %.1f kg", averageWeightKg))
        }
        if let weightChangeKg {
            lines.append(String(format: "Weight change: %+.1f kg", weightChangeKg))
        }
        if let averageWeeklyChangeKg {
            lines.append(String(format: "Average weekly change: %+.2f kg", averageWeeklyChangeKg))
        }
        if let targetWeightKg {
            lines.append(String(format: "Target weight: %.1f kg", targetWeightKg))
        }
        lines.append("Average calories eaten: \(Int(averageCalories.rounded())) kcal")
        lines.append(String(format: "Average food spend: ₹%.0f", averageFoodCost))
        lines.append(
            "Budget adherence: \(budget.adherencePercent)% "
            + "(\(budget.daysUnderBudget) under, \(budget.daysOverBudget) over)"
        )
        lines.append(String(format: "Total spent: ₹%.0f of ₹%.0f", budget.totalSpent, budget.totalBudget))
        lines.append("Workouts finished: \(workoutCount)")
        lines.append("Average steps: \(averageSteps)")
        lines.append("Average water: \(averageWaterMl) ml (target met on \(waterAdherencePercent)% of tracked days)")
        lines.append("Average daily checklist: \(averageCompletionPercent)%")
        lines.append("")
        lines.append(
            "Current streaks — diet \(streaks.diet)d, workout \(streaks.workout)d, "
            + "water \(streaks.water)d, budget \(streaks.budget)d"
        )
        lines.append("")
        lines.append("All values are estimates from data logged on this device.")
        lines.append(
            "Nutrition estimates are approximate. For medical conditions or special dietary needs, "
            + "consult a qualified healthcare professional."
        )
        return lines.joined(separator: "\n")
    }
}

extension AppStore {

    /// Summary of one day, assembled from real logged data only.
    func summary(for day: DayKey) -> DaySummary {
        summaries(from: day, to: day).first ?? DaySummary(day: day)
    }

    /// Summaries for a closed range of days. Days with no data are still returned (as empty days),
    /// which is what lets the streak logic detect gaps.
    func summaries(from: DayKey, to: DayKey) -> [DaySummary] {
        guard to >= from else { return [] }

        let meals = fetchMeals(from: from, to: to)
        let water = fetchWater(from: from, to: to)
        let steps = fetchSteps(from: from, to: to)
        let workouts = fetchWorkouts(from: from, to: to)
        let weights = fetchWeights(from: from, to: to)
        let expenses = fetchExpenses(from: from, to: to)
        let metas = fetchGoalSnapshots(from: from, to: to)

        let mealsByDay = Dictionary(grouping: meals, by: \.day)
        let waterByDay = Dictionary(grouping: water, by: \.day)
        let stepsByDay = Dictionary(steps.map { ($0.day, $0) }, uniquingKeysWith: { first, _ in first })
        let workoutsByDay = Dictionary(grouping: workouts, by: \.day)
        let weightsByDay = Dictionary(weights.map { ($0.day, $0) }, uniquingKeysWith: { _, last in last })
        let expensesByDay = Dictionary(grouping: expenses, by: \.day)

        let fallbackBudget = profile?.dailyBudget ?? 0

        return (from.value...to.value).map { value in
            let dayMeals = mealsByDay[value] ?? []
            let completedMeals = dayMeals.filter(\.completed)
            let meta = metas[value]

            var plannedTypes: Set<MealType> = []
            var completedTypes: Set<MealType> = []
            for type in MealType.allCases {
                let items = dayMeals.filter { $0.mealType == type }
                guard !items.isEmpty else { continue }
                plannedTypes.insert(type)
                if items.allSatisfy(\.completed) { completedTypes.insert(type) }
            }

            let extraSpend = (expensesByDay[value] ?? []).reduce(0) { $0 + $1.amount }

            return DaySummary(
                day: DayKey(value),
                plannedMealTypes: plannedTypes,
                completedMealTypes: completedTypes,
                mealItemsPlanned: dayMeals.count,
                mealItemsCompleted: completedMeals.count,
                caloriesPlanned: dayMeals.reduce(0) { $0 + $1.totalCalories },
                caloriesConsumed: completedMeals.reduce(0) { $0 + $1.totalCalories },
                proteinConsumed: completedMeals.reduce(0) { $0 + $1.totalProtein },
                plannedCost: dayMeals.reduce(0) { $0 + $1.totalCost },
                spent: completedMeals.reduce(0) { $0 + $1.totalCost } + extraSpend,
                budget: meta?.budget ?? fallbackBudget,
                waterMl: (waterByDay[value] ?? []).reduce(0) { $0 + $1.amountMl },
                waterTargetMl: meta?.waterTargetMl ?? settings.waterTargetMl,
                steps: stepsByDay[value]?.totalSteps ?? 0,
                stepGoal: meta?.stepGoal ?? settings.stepGoal,
                workoutsCompleted: (workoutsByDay[value] ?? []).count,
                weightKg: weightsByDay[value]?.weightKg
            )
        }
    }

    func computeStreaks(lookBackDays: Int = 180) -> Streaks {
        let today = DayCalendar.today()
        return StreakCalculator.compute(
            summaries(from: today - lookBackDays, to: today),
            today: today
        )
    }

    func monthlyReport(for monthDay: DayKey) -> MonthlyReport {
        let days = DayCalendar.monthRange(containing: monthDay)
        guard let first = days.first, let last = days.last else {
            return MonthlyReport(
                monthDay: monthDay, daysWithData: 0, daysInMonth: 0,
                averageWeightKg: nil, weightChangeKg: nil, averageWeeklyChangeKg: nil,
                startWeightKg: nil, endWeightKg: nil, targetWeightKg: profile?.targetWeightKg,
                averageCalories: 0, averageFoodCost: 0, budget: .empty, workoutCount: 0,
                averageSteps: 0, averageWaterMl: 0, waterAdherenceFraction: 0,
                averageCompletionPercent: 0, streaks: Streaks(), weightSeries: [],
                spendSeries: [], stepSeries: [], completionSeries: [], mealsCompleted: 0
            )
        }

        let all = summaries(from: first, to: last)
        let active = all.filter(\.hasAnyActivity)

        let weightPoints: [(day: DayKey, value: Double)] = all.compactMap { summary in
            guard let weight = summary.weightKg else { return nil }
            return (day: summary.day, value: weight)
        }
        let calorieDays = all.filter { $0.caloriesConsumed > 0 }
        let costDays = all.filter { $0.spent > 0 }
        let stepDays = all.filter { $0.steps > 0 }
        let waterDays = all.filter { $0.waterMl > 0 }

        let averageWeight: Double? = weightPoints.isEmpty
            ? nil
            : weightPoints.reduce(0) { $0 + $1.value } / Double(weightPoints.count)

        var weightChange: Double?
        var weeklyChange: Double?
        if let firstPoint = weightPoints.first, let lastPoint = weightPoints.last, weightPoints.count >= 2 {
            weightChange = lastPoint.value - firstPoint.value
            weeklyChange = HealthCalculator.averageWeeklyChangeKg(
                firstWeightKg: firstPoint.value,
                lastWeightKg: lastPoint.value,
                daysBetween: lastPoint.day.value - firstPoint.day.value
            )
        }

        return MonthlyReport(
            monthDay: monthDay,
            daysWithData: active.count,
            daysInMonth: all.count,
            averageWeightKg: averageWeight,
            weightChangeKg: weightChange,
            averageWeeklyChangeKg: weeklyChange,
            startWeightKg: weightPoints.first?.value,
            endWeightKg: weightPoints.last?.value,
            targetWeightKg: profile?.targetWeightKg,
            averageCalories: calorieDays.isEmpty
                ? 0
                : calorieDays.reduce(0) { $0 + $1.caloriesConsumed } / Double(calorieDays.count),
            averageFoodCost: costDays.isEmpty
                ? 0
                : costDays.reduce(0) { $0 + $1.spent } / Double(costDays.count),
            budget: BudgetCalculator.monthly(all),
            workoutCount: all.reduce(0) { $0 + $1.workoutsCompleted },
            averageSteps: stepDays.isEmpty ? 0 : stepDays.reduce(0) { $0 + $1.steps } / stepDays.count,
            averageWaterMl: waterDays.isEmpty
                ? 0
                : waterDays.reduce(0) { $0 + $1.waterMl } / waterDays.count,
            waterAdherenceFraction: WaterCalculator.adherenceFraction(
                dailyTotals: active.map(\.waterMl),
                targetMl: active.first?.waterTargetMl ?? 0
            ),
            averageCompletionPercent: active.isEmpty
                ? 0
                : active.reduce(0) { $0 + DailyChecklist.completionPercent($1) } / active.count,
            streaks: StreakCalculator.compute(all, today: DayCalendar.today()),
            weightSeries: weightPoints,
            spendSeries: all.map { (day: $0.day, value: $0.spent) },
            stepSeries: all.map { (day: $0.day, value: $0.steps) },
            completionSeries: all.map { (day: $0.day, value: DailyChecklist.completionPercent($0)) },
            mealsCompleted: all.reduce(0) { $0 + $1.mealItemsCompleted }
        )
    }
}
