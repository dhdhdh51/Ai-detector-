import SwiftUI
import FitBudgetCore

struct WeightView: View {

    @Environment(AppStore.self) private var store

    @State private var weightText = ""
    @State private var note = ""
    @State private var selectedDay = DayCalendar.today()
    @State private var error: String?
    @State private var pendingDelete: WeightLogRecord?

    private var chartPoints: [ChartPoint] {
        store.weightLogs
            .sorted { $0.day < $1.day }
            .map { ChartPoint(day: DayKey($0.day), value: $0.weightKg) }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                entryCard
                if !chartPoints.isEmpty { trendCard }
                historySection
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Weight")
        .navigationBarTitleDisplayMode(.inline)
        .task { prefill(for: selectedDay) }
        .confirmationDialog(
            "Delete this entry?",
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let pendingDelete { store.delete(pendingDelete) }
                pendingDelete = nil
            }
            Button("Cancel", role: .cancel) { pendingDelete = nil }
        }
    }

    private var entryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Log a weighing",
                subtitle: "\(Formatters.relativeDay(selectedDay, today: store.todayKey)) · \(Formatters.fullDate(selectedDay))"
            )
            NumberField(
                title: "Weight",
                text: $weightText,
                suffix: "kg",
                supportingText: "Weigh yourself at the same time each day for a clean trend.",
                errorMessage: error
            )
            PlainField(title: "Note (optional)", text: $note)

            HStack(spacing: 8) {
                Button("Previous day") { move(by: -1) }
                    .font(.caption)
                if selectedDay < store.todayKey {
                    Button("Next day") { move(by: 1) }
                        .font(.caption)
                }
            }

            Button {
                save()
            } label: {
                Text("Save weight").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
        .fitCard()
    }

    private var trendCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "All time trend")
            TrendLineChart(points: chartPoints, targetValue: store.profile?.targetWeightKg)
            MetricRow(label: "Starting weight", value: Formatters.kg(store.profile?.startWeightKg ?? 0))
            MetricRow(
                label: "Current weight",
                value: Formatters.kg(store.weightLogs.first?.weightKg ?? store.profile?.currentWeightKg ?? 0)
            )
            MetricRow(label: "Target weight", value: Formatters.kg(store.profile?.targetWeightKg ?? 0))
            if let first = chartPoints.first, let last = chartPoints.last, chartPoints.count > 1 {
                MetricRow(
                    label: "Change since first entry",
                    value: Formatters.signedKg(last.value - first.value)
                )
                let days = Int(last.date.timeIntervalSince(first.date) / 86_400)
                MetricRow(
                    label: "Average weekly change",
                    value: String(
                        format: "%+.2f kg",
                        HealthCalculator.averageWeeklyChangeKg(
                            firstWeightKg: first.value,
                            lastWeightKg: last.value,
                            daysBetween: days
                        )
                    )
                )
            }
        }
        .fitCard()
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "History", subtitle: "Newest first")
            if store.weightLogs.isEmpty {
                EmptyStateView(
                    systemImage: "scalemass",
                    title: "No weight entries yet",
                    message: "Save your first weighing above and the chart will appear."
                )
            } else {
                ForEach(store.weightLogs) { log in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(Formatters.kg(log.weightKg)).font(.subheadline.weight(.semibold))
                            Text(Formatters.fullDate(DayKey(log.day)))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            if let note = log.note {
                                Text(note).font(.caption)
                            }
                        }
                        Spacer(minLength: 8)
                        Button {
                            selectedDay = DayKey(log.day)
                            prefill(for: selectedDay)
                        } label: {
                            Image(systemName: "pencil")
                        }
                        .buttonStyle(.borderless)
                        Button(role: .destructive) {
                            pendingDelete = log
                        } label: {
                            Image(systemName: "trash")
                        }
                        .buttonStyle(.borderless)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .fitCard()
    }

    // MARK: - Logic

    private func move(by offset: Int) {
        let candidate = selectedDay + offset
        guard candidate <= store.todayKey else {
            store.show("You cannot log a weight for a future date.")
            return
        }
        selectedDay = candidate
        prefill(for: candidate)
    }

    private func prefill(for day: DayKey) {
        if let existing = store.weightLog(for: day) {
            weightText = String(format: "%.1f", existing.weightKg)
            note = existing.note ?? ""
        } else if let current = store.profile?.currentWeightKg {
            weightText = String(format: "%.1f", current)
            note = ""
        }
        error = nil
    }

    private func save() {
        let parsed = Validators.parseDecimal(weightText)
        let result = Validators.weight(parsed)
        guard result.isValid, let parsed else {
            error = result.message
            return
        }
        error = nil
        store.logWeight(parsed, on: selectedDay, note: note)
        store.show("Weight logged for \(Formatters.relativeDay(selectedDay, today: store.todayKey).lowercased()).")
    }
}
