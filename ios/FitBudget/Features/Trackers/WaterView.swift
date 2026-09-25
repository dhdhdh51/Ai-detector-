import SwiftUI
import FitBudgetCore

struct WaterView: View {

    @Environment(AppStore.self) private var store

    @State private var showTargetEditor = false
    @State private var showClearConfirm = false
    @State private var targetText = ""

    private var day: DayKey { store.todayKey }
    private var logs: [WaterLogRecord] { store.waterLogs(for: day) }
    private var snapshot: WaterSnapshot {
        WaterCalculator.snapshot(
            consumedMl: store.todaySummary.waterMl,
            targetMl: store.todaySummary.waterTargetMl
        )
    }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                ringCard
                quickButtons

                HStack(spacing: 8) {
                    Button {
                        store.undoLastWater()
                    } label: {
                        Label("Undo last", systemImage: "arrow.uturn.backward")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button {
                        showClearConfirm = true
                    } label: {
                        Text("Clear today").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                weekCard
                entriesCard
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Water")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Target") {
                    targetText = "\(store.settings.waterTargetMl)"
                    showTargetEditor = true
                }
            }
        }
        .alert("Daily water target", isPresented: $showTargetEditor) {
            TextField("Target in ml", text: $targetText)
                .keyboardType(.numberPad)
            Button("Save") {
                let parsed = Validators.parseInt(targetText)
                let result = Validators.waterTarget(parsed)
                if result.isValid, let parsed {
                    store.setWaterTarget(parsed)
                    store.show("Water target set to \(parsed) ml.")
                } else {
                    store.show(result.message ?? "Invalid target.")
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Between 500 ml and 10,000 ml. 2,500–3,000 ml suits most people.")
        }
        .confirmationDialog(
            "Clear today's water log?",
            isPresented: $showClearConfirm,
            titleVisibility: .visible
        ) {
            Button("Clear", role: .destructive) { store.clearWater(on: day) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("All of today's entries will be removed. History for other days is kept.")
        }
    }

    private var ringCard: some View {
        VStack(spacing: 14) {
            ProgressRing(fraction: snapshot.fraction, size: 180, lineWidth: 16, color: FitTheme.water) {
                VStack(spacing: 2) {
                    Text(Formatters.litres(snapshot.consumedMl)).font(.title2.bold())
                    Text("of \(Formatters.litres(snapshot.targetMl))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(snapshot.percent)%")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(FitTheme.water)
                }
            }
            Text(
                snapshot.isGoalMet
                    ? "Target reached — nicely done."
                    : "\(Formatters.ml(snapshot.remainingMl)) to go · about \(snapshot.glassesRemaining) glasses"
            )
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .fitCard(padding: 20)
    }

    private var quickButtons: some View {
        HStack(spacing: 8) {
            ForEach(WaterCalculator.quickAmountsMl, id: \.self) { amount in
                Button {
                    store.addWater(amount)
                } label: {
                    VStack(spacing: 2) {
                        Image(systemName: "drop.fill").font(.caption)
                        Text("+\(amount)").font(.caption2.weight(.semibold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .buttonStyle(.borderedProminent)
            }
        }
    }

    private var weekCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Last 7 days",
                subtitle: "Average \(Formatters.litres(averageWeek)) a day"
            )
            MiniBarStrip(
                values: weekTotals,
                goal: store.todaySummary.waterTargetMl,
                color: FitTheme.water
            )
            StreakChip(label: "Water streak", days: store.streaks.water)
        }
        .fitCard()
    }

    private var weekTotals: [Int] {
        store.summaries(from: day - 6, to: day).map(\.waterMl)
    }

    private var averageWeek: Int {
        WaterCalculator.averageMl(weekTotals)
    }

    private var entriesCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "Today's entries", subtitle: Formatters.fullDate(day))
            if logs.isEmpty {
                Text("Nothing logged yet today. Use the buttons above.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(logs) { log in
                    HStack {
                        Text(Formatters.ml(log.amountMl)).font(.subheadline)
                        Spacer()
                        Text(timeLabel(log.loggedAt))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Button(role: .destructive) {
                            store.delete(log)
                        } label: {
                            Image(systemName: "trash")
                        }
                        .buttonStyle(.borderless)
                    }
                    .padding(.vertical, 3)
                }
            }
            MetricRow(label: "Daily target", value: Formatters.litres(store.settings.waterTargetMl))
        }
        .fitCard()
    }

    private func timeLabel(_ date: Date) -> String {
        let components = DayCalendar.calendar().dateComponents([.hour, .minute], from: date)
        return Formatters.time(hour: components.hour ?? 0, minute: components.minute ?? 0)
    }
}
