import SwiftUI
import FitBudgetCore

struct MonthlyReportView: View {

    @Environment(AppStore.self) private var store

    @State private var monthAnchor = DayCalendar.today()
    @State private var exportedFile: URL?

    private var report: MonthlyReport { store.monthlyReport(for: monthAnchor) }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                monthSwitcher

                if report.hasData {
                    summaryCard
                    if !report.weightSeries.isEmpty { weightCard }
                    spendingCard
                    activityCard
                    streaksCard
                    shareRow
                } else {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Nothing tracked this month").font(.headline)
                        Text(
                            "Log meals, water, steps, workouts or a weighing and this report fills in "
                            + "automatically. FitBudget never invents data."
                        )
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    }
                    .fitCard()
                }

                DisclaimerCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Monthly report")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var monthSwitcher: some View {
        HStack {
            Button {
                monthAnchor = shiftMonth(by: -1)
            } label: {
                Image(systemName: "chevron.left")
            }
            .buttonStyle(.bordered)

            Text(report.title)
                .font(.headline)
                .frame(maxWidth: .infinity)

            Button {
                let next = shiftMonth(by: 1)
                if next <= store.todayKey { monthAnchor = next }
            } label: {
                Image(systemName: "chevron.right")
            }
            .buttonStyle(.bordered)
            .disabled(shiftMonth(by: 1) > store.todayKey)
        }
        .padding(.top, 4)
    }

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Summary").font(.headline).padding(.bottom, 6)
            MetricRow(label: "Days tracked", value: "\(report.daysWithData) of \(report.daysInMonth)")
            if let average = report.averageWeightKg {
                MetricRow(label: "Average weight", value: Formatters.kg(average))
            }
            if let change = report.weightChangeKg {
                MetricRow(label: "Weight change", value: Formatters.signedKg(change))
            }
            if let weekly = report.averageWeeklyChangeKg {
                MetricRow(label: "Average weekly change", value: String(format: "%+.2f kg", weekly))
            }
            MetricRow(
                label: "Average calories eaten",
                value: "\(Int(report.averageCalories.rounded())) kcal"
            )
            MetricRow(label: "Average food spend", value: Formatters.rupees(report.averageFoodCost))
            MetricRow(label: "Budget adherence", value: "\(report.budget.adherencePercent)%")
            MetricRow(label: "Workouts finished", value: "\(report.workoutCount)")
            MetricRow(label: "Average steps", value: Formatters.steps(report.averageSteps))
            MetricRow(label: "Average water", value: Formatters.litres(report.averageWaterMl))
            MetricRow(label: "Water target met", value: "\(report.waterAdherencePercent)% of days")
            MetricRow(label: "Average checklist", value: "\(report.averageCompletionPercent)%")
            MetricRow(label: "Meals ticked off", value: "\(report.mealsCompleted)")
        }
        .fitCard(background: FitTheme.brand.opacity(0.10))
    }

    private var weightCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Weight trend")
            TrendLineChart(
                points: report.weightSeries.map { ChartPoint(day: $0.day, value: $0.value) },
                targetValue: report.targetWeightKg
            )
        }
        .fitCard()
    }

    private var spendingCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Spending trend",
                subtitle: "\(Formatters.rupees(report.budget.totalSpent)) spent of \(Formatters.rupees(report.budget.totalBudget))"
            )
            DailyBarChart(
                points: report.spendSeries.map { ChartPoint(day: $0.day, value: $0.value) },
                color: FitTheme.budget,
                limitValue: report.budget.daysTracked > 0
                    ? report.budget.totalBudget / Double(report.budget.daysTracked)
                    : nil,
                valueLabel: "₹"
            )
            MetricRow(label: "Days under budget", value: "\(report.budget.daysUnderBudget)")
            MetricRow(label: "Days over budget", value: "\(report.budget.daysOverBudget)")
        }
        .fitCard()
    }

    private var activityCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Activity trend")
            DailyBarChart(
                points: report.stepSeries.map { ChartPoint(day: $0.day, value: Double($0.value)) },
                color: FitTheme.steps,
                valueLabel: "Steps"
            )
        }
        .fitCard()
    }

    private var streaksCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Streaks at the end of this period")
            HStack(spacing: 8) {
                StreakChip(label: "Diet", days: report.streaks.diet)
                StreakChip(label: "Workout", days: report.streaks.workout)
            }
            HStack(spacing: 8) {
                StreakChip(label: "Water", days: report.streaks.water)
                StreakChip(label: "Budget", days: report.streaks.budget)
            }
        }
        .fitCard()
    }

    private var shareRow: some View {
        HStack(spacing: 10) {
            if let exportedFile {
                ShareLink(item: exportedFile) {
                    Text("Share file").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            } else {
                Button {
                    exportedFile = try? store.writeMonthlyReport(for: monthAnchor)
                    if exportedFile == nil { store.show("Could not build the report file.") }
                } label: {
                    Text("Prepare file").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }

            ShareLink(item: report.shareText()) {
                Text("Share as text").frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
    }

    private func shiftMonth(by months: Int) -> DayKey {
        let calendar = DayCalendar.calendar()
        let date = DayCalendar.date(from: monthAnchor, calendar: calendar)
        guard let shifted = calendar.date(byAdding: .month, value: months, to: date) else {
            return monthAnchor
        }
        return DayCalendar.dayKey(for: shifted, calendar: calendar)
    }
}
