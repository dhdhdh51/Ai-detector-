import SwiftUI
import FitBudgetCore

/// First-launch flow. The example profile from the brief is pre-filled, every field is editable and
/// nothing is stored until the answers validate.
struct OnboardingView: View {

    @Environment(AppStore.self) private var store

    @State private var step = 0
    @State private var name = ""
    @State private var dateOfBirth = DayCalendar.date(from: DayKey(ProfileRecord.defaultDobDay))
    @State private var gender: Gender = .male
    @State private var heightText = "172"
    @State private var weightText = "85"
    @State private var targetText = "75"
    @State private var budgetText = "100"
    @State private var activityLevel: ActivityLevel = .light
    @State private var dietPreference: DietPreference = .eggetarian
    @State private var wakeDate = OnboardingView.time(hour: 6, minute: 30)
    @State private var sleepDate = OnboardingView.time(hour: 22, minute: 30)

    @State private var errors: [String: String] = [:]
    @State private var isSaving = false
    @State private var planSummary: PlanSummary?

    private let totalSteps = 4

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                header

                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        switch step {
                        case 0: aboutYouStep
                        case 1: bodyStep
                        case 2: budgetStep
                        default: routineStep
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
                }

                footer
            }
            .background(FitTheme.background)
            .navigationDestination(item: $planSummary) { summary in
                PlanReadyView(summary: summary)
            }
        }
    }

    // MARK: - Sections

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("FitBudget")
                .font(.largeTitle.bold())
                .foregroundStyle(FitTheme.brand)
            Text("₹100 a Day. Better Every Day.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            HStack(spacing: 6) {
                ForEach(0..<totalSteps, id: \.self) { index in
                    Capsule()
                        .fill(index <= step ? FitTheme.brand : FitTheme.subtleCard)
                        .frame(height: 6)
                }
            }
            .padding(.top, 6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .padding(.bottom, 16)
    }

    private var aboutYouStep: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(
                title: "About you",
                subtitle: "Used only on this device to estimate your plan."
            )
            PlainField(title: "Your name", text: $name, errorMessage: errors["name"])

            VStack(alignment: .leading, spacing: 4) {
                DatePicker(
                    "Date of birth",
                    selection: $dateOfBirth,
                    in: ...Date(),
                    displayedComponents: .date
                )
                .fitCard(background: FitTheme.subtleCard, padding: 14)
                if let message = errors["dob"] {
                    Text(message).font(.caption2).foregroundStyle(.red)
                } else {
                    Text("Age: \(HealthCalculator.age(dateOfBirth: dateOfBirth)) years")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            Text("Gender").font(.subheadline.weight(.semibold))
            Picker("Gender", selection: $gender) {
                ForEach(Gender.allCases, id: \.self) { Text($0.label).tag($0) }
            }
            .pickerStyle(.segmented)
            Text("Gender is only used to pick the right formula for the calorie estimate.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private var bodyStep: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: "Your numbers", subtitle: "You can change all of these later.")
            NumberField(
                title: "Height",
                text: $heightText,
                suffix: "cm",
                supportingText: "Between 100 and 250 cm",
                errorMessage: errors["height"]
            )
            NumberField(
                title: "Current weight",
                text: $weightText,
                suffix: "kg",
                supportingText: "Between 30 and 300 kg",
                errorMessage: errors["weight"]
            )
            NumberField(
                title: "Target weight",
                text: $targetText,
                suffix: "kg",
                supportingText: "Must be lower than or equal to your current weight",
                errorMessage: errors["target"]
            )
        }
    }

    private var budgetStep: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(
                title: "Food budget & activity",
                subtitle: "This is what keeps the plan affordable."
            )
            NumberField(
                title: "Daily food budget",
                text: $budgetText,
                suffix: "₹",
                supportingText: "Between ₹1 and ₹10,000 a day",
                errorMessage: errors["budget"],
                allowsDecimal: false
            )

            Text("Food preference").font(.subheadline.weight(.semibold))
            Picker("Food preference", selection: $dietPreference) {
                ForEach(DietPreference.allCases, id: \.self) { Text($0.label).tag($0) }
            }
            .pickerStyle(.segmented)

            Text("Activity level").font(.subheadline.weight(.semibold))
            VStack(spacing: 8) {
                ForEach(ActivityLevel.allCases, id: \.self) { level in
                    Button {
                        activityLevel = level
                    } label: {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(level.label).font(.subheadline.weight(.semibold))
                                Text(level.detail).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer(minLength: 8)
                            Image(systemName: activityLevel == level ? "largecircle.fill.circle" : "circle")
                                .foregroundStyle(activityLevel == level ? FitTheme.brand : Color.secondary)
                        }
                        .fitCard(
                            background: activityLevel == level
                                ? FitTheme.brand.opacity(0.12)
                                : FitTheme.subtleCard,
                            padding: 14
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var routineStep: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(
                title: "Your daily rhythm",
                subtitle: "Water reminders are spread between these two times."
            )
            DatePicker("Wake-up time", selection: $wakeDate, displayedComponents: .hourAndMinute)
                .fitCard(background: FitTheme.subtleCard, padding: 14)
            DatePicker("Sleep time", selection: $sleepDate, displayedComponents: .hourAndMinute)
                .fitCard(background: FitTheme.subtleCard, padding: 14)
            Text("All calculations in FitBudget are estimates, not medical advice.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var footer: some View {
        HStack {
            if step > 0 {
                Button("Back") {
                    withAnimation { step -= 1 }
                    errors = [:]
                }
                .buttonStyle(.bordered)
            }
            Spacer()
            Button(step == totalSteps - 1 ? "Create my plan" : "Continue") {
                advance()
            }
            .buttonStyle(.borderedProminent)
            .disabled(isSaving)
        }
        .padding(20)
        .background(.bar)
    }

    // MARK: - Logic

    private func advance() {
        let found = validate(step: step)
        guard found.isEmpty else {
            errors = found
            return
        }
        errors = [:]

        if step < totalSteps - 1 {
            withAnimation { step += 1 }
            return
        }
        save()
    }

    private func validate(step: Int) -> [String: String] {
        var found: [String: String] = [:]
        switch step {
        case 0:
            if let message = Validators.name(name).message { found["name"] = message }
            if let message = Validators.dateOfBirth(dateOfBirth).message { found["dob"] = message }
        case 1:
            let height = Validators.parseDecimal(heightText)
            let weight = Validators.parseDecimal(weightText)
            let target = Validators.parseDecimal(targetText)
            if let message = Validators.height(height).message { found["height"] = message }
            if let message = Validators.weight(weight, label: "Current weight").message {
                found["weight"] = message
            }
            if let message = Validators.targetWeight(target, currentKg: weight).message {
                found["target"] = message
            }
        case 2:
            if let message = Validators.budget(Validators.parseDecimal(budgetText)).message {
                found["budget"] = message
            }
        default:
            break
        }
        return found
    }

    private func save() {
        guard
            let height = Validators.parseDecimal(heightText),
            let weight = Validators.parseDecimal(weightText),
            let target = Validators.parseDecimal(targetText),
            let budget = Validators.parseDecimal(budgetText)
        else { return }

        isSaving = true
        Task {
            await store.completeOnboarding(
                name: name,
                dateOfBirth: dateOfBirth,
                gender: gender,
                heightCm: height,
                weightKg: weight,
                targetKg: target,
                dailyBudget: budget,
                activityLevel: activityLevel,
                dietPreference: dietPreference,
                wakeMinutes: Self.minutes(from: wakeDate),
                sleepMinutes: Self.minutes(from: sleepDate)
            )
            // Ask for notification permission once there is actually a plan to be reminded about.
            _ = await store.notifications.requestAuthorization()
            await store.rescheduleReminders()

            if let profile = store.profile {
                planSummary = PlanSummary(
                    name: profile.name,
                    currentWeightKg: profile.currentWeightKg,
                    targetWeightKg: profile.targetWeightKg,
                    dailyBudget: profile.dailyBudget,
                    waterTargetMl: store.settings.waterTargetMl,
                    stepGoal: store.settings.stepGoal,
                    calorieTarget: profile.estimatedCalorieTarget,
                    proteinTargetG: profile.proteinTargetGrams,
                    bmi: profile.bmi,
                    bmiCategory: profile.bmiCategory,
                    weeklyTrendKg: HealthCalculator.weeklyWeightChangeKg(
                        tdee: profile.tdee,
                        intakeCalories: profile.estimatedCalorieTarget
                    )
                )
            }
            isSaving = false
        }
    }

    private static func time(hour: Int, minute: Int) -> Date {
        var components = DateComponents()
        components.hour = hour
        components.minute = minute
        return DayCalendar.calendar().date(from: components) ?? Date()
    }

    private static func minutes(from date: Date) -> Int {
        let components = DayCalendar.calendar().dateComponents([.hour, .minute], from: date)
        return (components.hour ?? 0) * 60 + (components.minute ?? 0)
    }
}

/// Values shown on the "Your plan is ready" screen.
struct PlanSummary: Identifiable, Hashable {
    var id: String { "\(name)-\(currentWeightKg)-\(targetWeightKg)" }

    let name: String
    let currentWeightKg: Double
    let targetWeightKg: Double
    let dailyBudget: Double
    let waterTargetMl: Int
    let stepGoal: Int
    let calorieTarget: Double
    let proteinTargetG: Double
    let bmi: Double
    let bmiCategory: String
    let weeklyTrendKg: Double
}

/// Shown once, right after onboarding.
struct PlanReadyView: View {

    let summary: PlanSummary

    @Environment(AppStore.self) private var store

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(FitTheme.brand)
                    .padding(.top, 24)

                Text("Your plan is ready.").font(.title2.bold())
                Text(
                    summary.name.isEmpty
                        ? "Let's get started."
                        : "Let's get started, \(summary.name)."
                )
                .font(.subheadline)
                .foregroundStyle(.secondary)

                VStack(spacing: 0) {
                    MetricRow(label: "Current weight", value: Formatters.kg(summary.currentWeightKg))
                    MetricRow(label: "Target weight", value: Formatters.kg(summary.targetWeightKg))
                    MetricRow(label: "Daily food budget", value: Formatters.rupees(summary.dailyBudget))
                    MetricRow(label: "Water goal", value: Formatters.litres(summary.waterTargetMl))
                    MetricRow(label: "Step goal", value: Formatters.steps(summary.stepGoal))
                }
                .fitCard()

                VStack(alignment: .leading, spacing: 0) {
                    Text("Estimates").font(.subheadline.weight(.semibold))
                    MetricRow(label: "Daily calories", value: Formatters.calories(summary.calorieTarget))
                    MetricRow(label: "Protein target", value: Formatters.grams(summary.proteinTargetG))
                    MetricRow(
                        label: "BMI",
                        value: String(format: "%.1f · %@", summary.bmi, summary.bmiCategory)
                    )
                    MetricRow(
                        label: "Estimated trend",
                        value: String(format: "%.2f kg / week", summary.weeklyTrendKg)
                    )
                }
                .fitCard(background: FitTheme.brand.opacity(0.10))

                DisclaimerCard()

                Button("Open my dashboard") {
                    // Leaving onboarding is driven by the stored profile flag.
                    store.refresh()
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding(.top, 8)
            }
            .padding(20)
        }
        .background(FitTheme.background)
        .navigationBarBackButtonHidden(true)
    }
}
