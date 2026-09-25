import SwiftUI
import FitBudgetCore

struct BudgetView: View {

    @Environment(AppStore.self) private var store

    @State private var monthAnchor = DayCalendar.today()
    @State private var showBudgetEditor = false
    @State private var showExpenseEditor = false
    @State private var budgetText = ""
    @State private var expenseLabel = ""
    @State private var expenseAmount = ""

    private var day: DayKey { store.todayKey }
    private var summary: DaySummary { store.todaySummary }
    private var snapshot: BudgetSnapshot {
        BudgetCalculator.snapshot(
            budget: summary.budget,
            spent: summary.spent,
            pendingPlannedCost: max(summary.plannedCost - summary.spent, 0)
        )
    }
    private var monthSummaries: [DaySummary] {
        let days = DayCalendar.monthRange(containing: monthAnchor)
        guard let first = days.first, let last = days.last else { return [] }
        return store.summaries(from: first, to: last)
    }
    private var monthly: MonthlyBudgetSummary { BudgetCalculator.monthly(monthSummaries) }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                todayCard

                Button {
                    expenseLabel = ""
                    expenseAmount = ""
                    showExpenseEditor = true
                } label: {
                    Text("Add an extra food expense").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                let expenses = store.expenses(for: day)
                if !expenses.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        SectionHeader(title: "Extra expenses today")
                        ForEach(expenses) { expense in
                            HStack {
                                Text(expense.label).font(.subheadline)
                                Spacer()
                                Text(Formatters.rupees(expense.amount))
                                    .font(.subheadline.weight(.semibold))
                                Button(role: .destructive) {
                                    store.delete(expense)
                                } label: {
                                    Image(systemName: "trash")
                                }
                                .buttonStyle(.borderless)
                            }
                            .padding(.vertical, 2)
                        }
                    }
                    .fitCard()
                }

                monthCard
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Food budget")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Edit") {
                    budgetText = "\(Int(summary.budget))"
                    showBudgetEditor = true
                }
            }
        }
        .alert("Daily food budget", isPresented: $showBudgetEditor) {
            TextField("Budget in ₹", text: $budgetText)
                .keyboardType(.numberPad)
            Button("Save") {
                let parsed = Validators.parseDecimal(budgetText)
                let result = Validators.budget(parsed)
                if result.isValid, let parsed {
                    store.setDailyBudget(parsed)
                    store.show("Daily budget set to \(Formatters.rupees(parsed)).")
                } else {
                    store.show(result.message ?? "Invalid budget.")
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Between ₹1 and ₹10,000. Past days keep their old budget.")
        }
        .alert("Extra food expense", isPresented: $showExpenseEditor) {
            TextField("What was it?", text: $expenseLabel)
            TextField("Amount in ₹", text: $expenseAmount)
                .keyboardType(.decimalPad)
            Button("Add") {
                if let amount = Validators.parseDecimal(expenseAmount) {
                    store.addExpense(label: expenseLabel, amount: amount)
                } else {
                    store.show("Enter an amount.")
                }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    private var todayCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("\(Formatters.relativeDay(day, today: store.todayKey))'s budget").font(.headline)
            Text(Formatters.rupees(summary.budget))
                .font(.system(size: 34, weight: .bold, design: .rounded))
            LabeledProgressBar(
                label: "Spent",
                valueText: Formatters.rupees(snapshot.spent),
                fraction: snapshot.usedFraction,
                color: snapshot.isOverBudget ? .red : FitTheme.budget
            )
            MetricRow(
                label: snapshot.isOverBudget ? "Over budget by" : "Remaining",
                value: snapshot.isOverBudget
                    ? Formatters.rupees(snapshot.overBy)
                    : Formatters.rupees(snapshot.remaining),
                valueColor: snapshot.isOverBudget ? .red : .primary
            )
            MetricRow(label: "Planned meal cost today", value: Formatters.rupees(summary.plannedCost))
            if snapshot.pendingPlannedCost > 0 {
                MetricRow(
                    label: "Still to eat (planned)",
                    value: Formatters.rupees(snapshot.pendingPlannedCost)
                )
                if snapshot.projectedOverBudget {
                    Text("Finishing the whole plan would put you over budget.")
                        .font(.caption2)
                        .foregroundStyle(.red)
                }
            }
            Text("Spending counts a meal once you tick it off in the Diet tab, plus any extras you add here.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .fitCard(background: snapshot.isOverBudget ? Color.red.opacity(0.10) : FitTheme.card)
    }

    private var monthCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Button {
                    monthAnchor = shiftMonth(by: -1)
                } label: {
                    Image(systemName: "chevron.left")
                }
                .buttonStyle(.bordered)
                Text(Formatters.monthTitle(monthAnchor))
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

            if monthly.daysTracked > 0 {
                DailyBarChart(
                    points: monthSummaries.map { ChartPoint(day: $0.day, value: $0.spent) },
                    color: FitTheme.budget,
                    limitValue: summary.budget,
                    valueLabel: "₹"
                )
                MetricRow(label: "Days tracked", value: "\(monthly.daysTracked)")
                MetricRow(label: "Total budget", value: Formatters.rupees(monthly.totalBudget))
                MetricRow(label: "Total spent", value: Formatters.rupees(monthly.totalSpent))
                MetricRow(label: "Average a day", value: Formatters.rupees(monthly.averageDailySpend))
                MetricRow(label: "Days under budget", value: "\(monthly.daysUnderBudget)")
                MetricRow(label: "Days over budget", value: "\(monthly.daysOverBudget)")
                MetricRow(label: "Budget adherence", value: "\(monthly.adherencePercent)%")
            } else {
                Text("No spending tracked in this month yet.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            StreakChip(label: "Budget streak", days: store.streaks.budget)
        }
        .fitCard()
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
