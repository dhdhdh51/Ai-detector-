import SwiftUI
import UIKit
import FitBudgetCore

struct HomeView: View {

    @Environment(AppStore.self) private var store
    @Environment(Router.self) private var router

    private var profile: ProfileRecord? { store.profile }
    private var summary: DaySummary { store.todaySummary }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: FitTheme.sectionSpacing) {
                greeting

                if store.notifications.authorizationStatus == .denied {
                    ActionBanner(
                        title: "Reminders are switched off",
                        message: "FitBudget needs notification permission to remind you about meals, water and workouts.",
                        actionTitle: "Open Settings"
                    ) {
                        openSystemSettings()
                    }
                } else if store.notifications.authorizationStatus == .notDetermined {
                    ActionBanner(
                        title: "Turn on reminders",
                        message: "Allow notifications so your meal, water and workout reminders arrive on time.",
                        actionTitle: "Allow"
                    ) {
                        Task {
                            _ = await store.notifications.requestAuthorization()
                            await store.rescheduleReminders()
                        }
                    }
                }

                heroCard

                HStack(spacing: 12) {
                    StatCard(
                        title: "Current weight",
                        value: Formatters.weight(profile?.currentWeightKg ?? 0, units: store.settings.unitSystem),
                        systemImage: "scalemass.fill",
                        accent: FitTheme.weight,
                        caption: "Tap to log today"
                    )
                    .onTapGesture { router.push(.weight, on: .home) }

                    StatCard(
                        title: "Target weight",
                        value: Formatters.weight(profile?.targetWeightKg ?? 0, units: store.settings.unitSystem),
                        systemImage: "target",
                        accent: FitTheme.diet,
                        caption: targetCaption
                    )
                }

                HStack(spacing: 12) {
                    StatCard(
                        title: "BMI (estimate)",
                        value: (profile?.bmi ?? 0) > 0 ? String(format: "%.1f", profile?.bmi ?? 0) : "—",
                        systemImage: "heart.text.square.fill",
                        accent: FitTheme.workout,
                        caption: profile?.bmiCategory ?? "—"
                    )
                    StatCard(
                        title: "Today's calories",
                        value: "\(Int(summary.caloriesConsumed.rounded()))",
                        systemImage: "flame.fill",
                        accent: FitTheme.diet,
                        caption: "of \(Int((profile?.estimatedCalorieTarget ?? 0).rounded())) kcal target"
                    )
                    .onTapGesture { router.selectedTab = .diet }
                }

                budgetCard
                trackersCard
                checklistCard
                streaksCard

                if !store.weekSteps.isEmpty {
                    Button {
                        router.push(.steps, on: .home)
                    } label: {
                        VStack(alignment: .leading, spacing: 10) {
                            SectionHeader(title: "Last 7 days of steps")
                            MiniBarStrip(values: store.weekSteps, goal: summary.stepGoal)
                        }
                        .fitCard()
                    }
                    .buttonStyle(.plain)
                }

                estimatesCard
                DisclaimerCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Home")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    router.push(.settings, on: .home)
                } label: {
                    Image(systemName: "gearshape")
                }
                .accessibilityLabel("Settings")
            }
        }
    }

    // MARK: - Sections

    private var greeting: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(Formatters.greeting())
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(profile?.name.isEmpty == false ? profile!.name : "Welcome")
                .font(.title2.bold())
            Text(Formatters.fullDate(store.todayKey))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
    }

    private var targetCaption: String {
        guard let profile else { return "—" }
        return profile.remainingWeightKg > 0
            ? "\(Formatters.kg(profile.remainingWeightKg)) to go"
            : "Target reached"
    }

    private var heroCard: some View {
        Button {
            router.selectedTab = .progress
        } label: {
            HStack(spacing: 18) {
                ProgressRing(
                    fraction: (profile?.progressPercent ?? 0) / 100,
                    size: 104,
                    lineWidth: 11
                ) {
                    VStack(spacing: 0) {
                        Text("\(Int((profile?.progressPercent ?? 0).rounded()))%")
                            .font(.title3.bold())
                        Text("of goal").font(.caption2)
                    }
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("\(Formatters.kg(profile?.startWeightKg ?? 0)) → \(Formatters.kg(profile?.targetWeightKg ?? 0))")
                        .font(.headline)
                    Text(
                        (profile?.currentWeightKg ?? 0) > (profile?.targetWeightKg ?? 0)
                            ? "\(Formatters.kg((profile?.currentWeightKg ?? 0) - (profile?.targetWeightKg ?? 0))) remaining"
                            : "Target reached"
                    )
                    .font(.subheadline)
                    Text("Today's progress: \(completionPercent)%")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(FitTheme.brand)
                }
                Spacer(minLength: 0)
            }
            .fitCard(background: FitTheme.brand.opacity(0.12), padding: 18)
        }
        .buttonStyle(.plain)
    }

    private var completionPercent: Int {
        Int(DailyChecklist.completionFraction(store.checklist) * 100)
    }

    private var budgetCard: some View {
        let snapshot = BudgetCalculator.snapshot(budget: summary.budget, spent: summary.spent)
        return Button {
            router.push(.budget, on: .home)
        } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "indianrupeesign.circle.fill").foregroundStyle(FitTheme.budget)
                    Text("Today's budget").font(.headline)
                    Spacer()
                    Text(Formatters.rupees(summary.budget)).font(.headline)
                }
                LabeledProgressBar(
                    label: snapshot.isOverBudget ? "Over budget" : "Spent",
                    valueText: Formatters.rupees(snapshot.spent),
                    fraction: snapshot.usedFraction,
                    color: snapshot.isOverBudget ? .red : FitTheme.budget
                )
                Text(
                    snapshot.isOverBudget
                        ? "Over by \(Formatters.rupees(snapshot.overBy))"
                        : "\(Formatters.rupees(snapshot.remaining)) remaining"
                )
                .font(.subheadline)
                .foregroundStyle(snapshot.isOverBudget ? .red : .secondary)
            }
            .fitCard()
        }
        .buttonStyle(.plain)
    }

    private var trackersCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Today's trackers")

            LabeledProgressBar(
                label: "Water",
                valueText: "\(summary.waterMl) / \(summary.waterTargetMl) ml",
                fraction: WaterCalculator.snapshot(
                    consumedMl: summary.waterMl,
                    targetMl: summary.waterTargetMl
                ).fraction,
                color: FitTheme.water
            )
            LabeledProgressBar(
                label: "Steps",
                valueText: "\(Formatters.steps(summary.steps)) / \(Formatters.steps(summary.stepGoal))",
                fraction: summary.stepGoal > 0 ? Double(summary.steps) / Double(summary.stepGoal) : 0,
                color: FitTheme.steps
            )
            LabeledProgressBar(
                label: "Meals ticked off",
                valueText: "\(summary.mealItemsCompleted) / \(summary.mealItemsPlanned)",
                fraction: summary.mealItemsPlanned > 0
                    ? Double(summary.mealItemsCompleted) / Double(summary.mealItemsPlanned)
                    : 0,
                color: FitTheme.diet
            )
            LabeledProgressBar(
                label: "Workout",
                valueText: summary.workoutsCompleted > 0 ? "\(summary.workoutsCompleted) done" : "Not yet",
                fraction: summary.workoutsCompleted > 0 ? 1 : 0,
                color: FitTheme.workout
            )

            HStack(spacing: 8) {
                ForEach(WaterCalculator.quickAmountsMl.prefix(3), id: \.self) { amount in
                    Button {
                        store.addWater(amount)
                    } label: {
                        Label("\(amount)", systemImage: "drop.fill")
                            .font(.caption.weight(.semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(.top, 4)
        }
        .fitCard()
    }

    private var checklistCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(
                title: "Daily checklist",
                subtitle: "Today's progress: \(completionPercent)%"
            )
            ForEach(store.checklist) { item in
                ChecklistRow(item: item)
            }
        }
        .fitCard()
    }

    private var streaksCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Streaks", subtitle: "Only real completed days count.")
            HStack(spacing: 8) {
                StreakChip(label: "Diet", days: store.streaks.diet)
                StreakChip(label: "Workout", days: store.streaks.workout)
            }
            HStack(spacing: 8) {
                StreakChip(label: "Water", days: store.streaks.water)
                StreakChip(label: "Budget", days: store.streaks.budget)
            }
        }
        .fitCard()
    }

    private var estimatesCard: some View {
        let tdee = profile?.tdee ?? 0
        let target = profile?.estimatedCalorieTarget ?? 0
        let deficit = HealthCalculator.deficitRange(tdee: tdee)
        let weekly = HealthCalculator.weeklyWeightChangeKg(tdee: tdee, intakeCalories: target)
        let weeks = HealthCalculator.weeksToTarget(
            currentKg: profile?.currentWeightKg ?? 0,
            targetKg: profile?.targetWeightKg ?? 0,
            weeklyLossKg: weekly
        )
        return VStack(alignment: .leading, spacing: 6) {
            Text("Your estimates").font(.subheadline.weight(.semibold))
            Text("Maintenance ≈ \(Int(tdee.rounded())) kcal · suggested deficit \(deficit.lowerBound)–\(deficit.upperBound) kcal")
                .font(.caption)
            Text(
                String(format: "Estimated trend ≈ %.2f kg per week", weekly)
                    + (weeks.map { " · about \($0) weeks to target" } ?? "")
            )
            .font(.caption)
        }
        .fitCard(background: FitTheme.budget.opacity(0.12))
    }

    private func openSystemSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}
