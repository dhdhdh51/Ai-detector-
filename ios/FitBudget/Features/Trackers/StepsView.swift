import SwiftUI
import FitBudgetCore

struct StepsView: View {

    @Environment(AppStore.self) private var store

    @State private var manualText = ""
    @State private var goalText = ""
    @State private var showGoalEditor = false

    private var log: StepLogRecord? { store.stepLog(for: store.todayKey) }
    private var sensorSteps: Int { log?.sensorSteps ?? 0 }
    private var manualSteps: Int { log?.manualSteps ?? 0 }
    private var total: Int { sensorSteps + manualSteps }
    private var goal: Int { store.todaySummary.stepGoal }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                ringCard
                sensorBanner
                manualCard
                weekCard
                detailCard
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Steps")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Goal") {
                    goalText = "\(store.settings.stepGoal)"
                    showGoalEditor = true
                }
            }
        }
        .task {
            manualText = "\(manualSteps)"
            await store.syncStepsFromSensor()
        }
        .alert("Daily step goal", isPresented: $showGoalEditor) {
            TextField("Step goal", text: $goalText)
                .keyboardType(.numberPad)
            Button("Save") {
                let parsed = Validators.parseInt(goalText)
                let result = Validators.steps(parsed)
                if result.isValid, let parsed {
                    store.setStepGoal(parsed)
                    store.show("Step goal set to \(parsed).")
                } else {
                    store.show(result.message ?? "Invalid goal.")
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("8,000 is a solid target for fat loss.")
        }
    }

    private var ringCard: some View {
        VStack(spacing: 14) {
            ProgressRing(
                fraction: goal > 0 ? Double(total) / Double(goal) : 0,
                size: 180,
                lineWidth: 16,
                color: FitTheme.steps
            ) {
                VStack(spacing: 2) {
                    Text(Formatters.steps(total)).font(.title2.bold())
                    Text("of \(Formatters.steps(goal))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Text(
                total >= goal && goal > 0
                    ? "Goal reached today."
                    : "\(Formatters.steps(max(goal - total, 0))) steps to go"
            )
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .fitCard(padding: 20)
    }

    @ViewBuilder
    private var sensorBanner: some View {
        if !store.pedometer.isAvailable {
            VStack(alignment: .leading, spacing: 4) {
                Text("No motion sensor on this device").font(.subheadline.weight(.semibold))
                Text("Enter your steps manually below — everything else keeps working exactly the same.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .fitCard(background: FitTheme.budget.opacity(0.12), padding: 14)
        } else if !store.pedometer.isAuthorized {
            VStack(alignment: .leading, spacing: 6) {
                Text("Automatic counting is off").font(.subheadline.weight(.semibold))
                Text("Allow motion access and FitBudget will read the step count your iPhone already keeps.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button("Allow motion access") {
                    Task { await store.requestMotionAccess() }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
            }
            .fitCard(background: FitTheme.budget.opacity(0.12), padding: 14)
        }
    }

    private var manualCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Manual entry",
                subtitle: "Adds to whatever the sensor already counted."
            )
            NumberField(
                title: "Manual steps for today",
                text: $manualText,
                supportingText: "0 – 100,000",
                allowsDecimal: false
            )
            HStack(spacing: 8) {
                Button {
                    if let parsed = Validators.parseInt(manualText) {
                        store.setManualSteps(parsed)
                    } else {
                        store.show("Enter a step count.")
                    }
                } label: {
                    Text("Save").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Button {
                    store.addManualSteps(1_000)
                    manualText = "\(manualSteps + 1_000)"
                } label: {
                    Text("+1,000").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
        .fitCard()
    }

    private var weekCard: some View {
        let values = store.summaries(from: store.todayKey - 6, to: store.todayKey).map(\.steps)
        let average = values.isEmpty ? 0 : values.reduce(0, +) / values.count
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: "Last 7 days",
                subtitle: "Average \(Formatters.steps(average)) steps"
            )
            MiniBarStrip(values: values, goal: goal, color: FitTheme.steps)
        }
        .fitCard()
    }

    private var detailCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            MetricRow(label: "Counted by sensor", value: Formatters.steps(sensorSteps))
            MetricRow(label: "Entered manually", value: Formatters.steps(manualSteps))
            MetricRow(label: "Daily goal", value: Formatters.steps(goal))
            MetricRow(label: "Source", value: (log?.source ?? .sensor).label)
            Text("iOS counts steps in the background; FitBudget reads today's total each time you open it.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .fitCard()
    }
}
