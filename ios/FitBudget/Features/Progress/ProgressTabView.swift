import SwiftUI
import FitBudgetCore

enum ProgressRange: String, CaseIterable, Identifiable {
    case week = "7 days"
    case month = "30 days"
    case quarter = "90 days"
    case all = "All time"

    var id: String { rawValue }

    var days: Int? {
        switch self {
        case .week: return 7
        case .month: return 30
        case .quarter: return 90
        case .all: return nil
        }
    }
}

struct ProgressTabView: View {

    @Environment(AppStore.self) private var store
    @Environment(Router.self) private var router

    @State private var range: ProgressRange = .month

    private var fromDay: DayKey {
        if let days = range.days { return store.todayKey - (days - 1) }
        let earliest = store.weightLogs.map(\.day).min()
        return DayKey(earliest ?? (store.todayKey.value - 29))
    }

    private var summaries: [DaySummary] {
        store.summaries(from: fromDay, to: store.todayKey)
    }

    private var weightPoints: [ChartPoint] {
        summaries.compactMap { summary in
            summary.weightKg.map { ChartPoint(day: summary.day, value: $0) }
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: FitTheme.sectionSpacing) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Progress").font(.title2.bold())
                    Text("Everything here comes from what you have logged.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)

                Picker("Range", selection: $range) {
                    ForEach(ProgressRange.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)

                weightCard

                HStack(spacing: 10) {
                    Button {
                        router.push(.weight, on: .progress)
                    } label: {
                        Text("Log weight").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)

                    Button {
                        router.push(.monthlyReport, on: .progress)
                    } label: {
                        Text("Monthly report").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                spendingCard
                activityCard
                checklistCard
                streaksCard

                Text("All calculations shown are estimates, not medical advice.")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Progress")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Cards

    private var weightCard: some View {
        let profile = store.profile
        let logCount = store.weightLogs.count
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Weight trend",
                subtitle: "\(logCount) entr\(logCount == 1 ? "y" : "ies") logged"
            )
            if weightPoints.isEmpty {
                EmptyStateView(
                    systemImage: "scalemass",
                    title: "No weight entries yet",
                    message: "Log today's weight to start your trend chart.",
                    actionTitle: "Log weight"
                ) {
                    router.push(.weight, on: .progress)
                }
            } else {
                TrendLineChart(points: weightPoints, targetValue: profile?.targetWeightKg)
                MetricRow(label: "Starting weight", value: Formatters.kg(profile?.startWeightKg ?? 0))
                MetricRow(label: "Current weight", value: Formatters.kg(profile?.currentWeightKg ?? 0))
                MetricRow(label: "Target weight", value: Formatters.kg(profile?.targetWeightKg ?? 0))
                MetricRow(
                    label: "Total change",
                    value: Formatters.signedKg(
                        (profile?.currentWeightKg ?? 0) - (profile?.startWeightKg ?? 0)
                    )
                )
                MetricRow(
                    label: "Average weekly change",
                    value: weeklyChangeText
                )
                Text("The dashed line is your target weight.")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .fitCard()
    }

    private var weeklyChangeText: String {
        guard weightPoints.count > 1,
              let first = weightPoints.first,
              let last = weightPoints.last else { return "Needs 2+ entries" }
        let days = Int(last.date.timeIntervalSince(first.date) / 86_400)
        let change = HealthCalculator.averageWeeklyChangeKg(
            firstWeightKg: first.value,
            lastWeightKg: last.value,
            daysBetween: days
        )
        return String(format: "%+.2f kg", change)
    }

    private var spendingCard: some View {
        let points = summaries.map { ChartPoint(day: $0.day, value: $0.spent) }
        let tracked = summaries.filter { $0.spent > 0 }
        let average = tracked.isEmpty ? 0 : tracked.reduce(0) { $0 + $1.spent } / Double(tracked.count)
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Food spending",
                subtitle: "Average \(Formatters.rupees(average)) a day"
            )
            if tracked.isEmpty {
                Text("Tick meals off in the Diet tab and your spending will show up here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                DailyBarChart(
                    points: points,
                    color: FitTheme.budget,
                    limitValue: store.profile?.dailyBudget,
                    valueLabel: "₹"
                )
                Text("The dashed line is your daily budget. Red bars went over.")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .fitCard()
    }

    private var activityCard: some View {
        let points = summaries.map { ChartPoint(day: $0.day, value: Double($0.steps)) }
        let stepDays = summaries.filter { $0.steps > 0 }
        let average = stepDays.isEmpty ? 0 : stepDays.reduce(0) { $0 + $1.steps } / stepDays.count
        let workouts = summaries.reduce(0) { $0 + $1.workoutsCompleted }
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Activity",
                subtitle: "Average \(Formatters.steps(average)) steps · \(workouts) workouts"
            )
            if stepDays.isEmpty {
                Text("No steps recorded for this period yet.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                DailyBarChart(
                    points: points,
                    color: FitTheme.steps,
                    limitValue: Double(summaries.last?.stepGoal ?? 0),
                    valueLabel: "Steps"
                )
            }
        }
        .fitCard()
    }

    private var checklistCard: some View {
        let points = summaries.map {
            ChartPoint(day: $0.day, value: Double(DailyChecklist.completionPercent($0)))
        }
        let active = summaries.filter(\.hasAnyActivity)
        let average = active.isEmpty
            ? 0
            : active.reduce(0) { $0 + DailyChecklist.completionPercent($1) } / active.count
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Daily checklist", subtitle: "Average \(average)% completed")
            if active.isEmpty {
                Text("Start ticking items off and this chart fills in.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                DailyBarChart(
                    points: points,
                    color: FitTheme.diet,
                    limitValue: 100,
                    valueLabel: "%"
                )
            }
        }
        .fitCard()
    }

    private var streaksCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Current streaks")
            HStack(spacing: 8) {
                StreakChip(label: "Diet", days: store.streaks.diet)
                StreakChip(label: "Workout", days: store.streaks.workout)
            }
            HStack(spacing: 8) {
                StreakChip(label: "Water", days: store.streaks.water)
                StreakChip(label: "Budget", days: store.streaks.budget)
            }
            Text("Today counts towards a streak only once the goal is actually met.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .fitCard()
    }
}
