import SwiftUI
import FitBudgetCore

struct ProfileView: View {

    @Environment(AppStore.self) private var store
    @Environment(Router.self) private var router

    @State private var name = ""
    @State private var dateOfBirth = Date()
    @State private var gender: Gender = .male
    @State private var heightText = ""
    @State private var weightText = ""
    @State private var targetText = ""
    @State private var budgetText = ""
    @State private var calorieOverrideText = ""
    @State private var activityLevel: ActivityLevel = .light
    @State private var dietPreference: DietPreference = .vegetarian
    @State private var wakeDate = Date()
    @State private var sleepDate = Date()
    @State private var errors: [String: String] = [:]
    @State private var loaded = false

    private var profile: ProfileRecord? { store.profile }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                headerCard
                estimatesCard
                streaksCard
                editCard
                linksCard
                DisclaimerCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            guard !loaded else { return }
            loadForm()
            loaded = true
        }
    }

    // MARK: - Cards

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(profile?.name.isEmpty == false ? profile!.name : "Your profile")
                .font(.title2.bold())
            Text(
                "\(profile?.age ?? 0) years · \(profile?.gender.label ?? "—") · "
                + Formatters.height(profile?.heightCm ?? 0, units: store.settings.unitSystem)
            )
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
    }

    private var estimatesCard: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Estimates from your profile").font(.subheadline.weight(.semibold))
                .padding(.bottom, 6)
            MetricRow(
                label: "BMI",
                value: String(format: "%.1f · %@", profile?.bmi ?? 0, profile?.bmiCategory ?? "—")
            )
            if let range = profile?.healthyWeightRange {
                MetricRow(
                    label: "Healthy weight range",
                    value: String(format: "%.0f – %.0f kg", range.lowerBound, range.upperBound)
                )
            }
            MetricRow(
                label: "Maintenance calories",
                value: "\(Int((profile?.tdee ?? 0).rounded())) kcal"
            )
            MetricRow(
                label: "Daily target",
                value: "\(Int((profile?.estimatedCalorieTarget ?? 0).rounded())) kcal"
            )
            MetricRow(label: "Protein target", value: Formatters.grams(profile?.proteinTargetGrams ?? 0))
            MetricRow(
                label: "Progress to goal",
                value: "\(Int((profile?.progressPercent ?? 0).rounded()))%"
            )
        }
        .fitCard(background: FitTheme.brand.opacity(0.10))
    }

    private var streaksCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Streaks")
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

    private var editCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Edit profile", subtitle: "Changes apply from today onwards.")

            PlainField(title: "Name", text: $name, errorMessage: errors["name"])

            DatePicker("Date of birth", selection: $dateOfBirth, in: ...Date(), displayedComponents: .date)
            if let message = errors["dob"] {
                Text(message).font(.caption2).foregroundStyle(.red)
            }

            Text("Gender").font(.subheadline.weight(.semibold))
            Picker("Gender", selection: $gender) {
                ForEach(Gender.allCases, id: \.self) { Text($0.label).tag($0) }
            }
            .pickerStyle(.segmented)

            NumberField(title: "Height", text: $heightText, suffix: "cm", errorMessage: errors["height"])
            NumberField(title: "Current weight", text: $weightText, suffix: "kg", errorMessage: errors["weight"])
            NumberField(title: "Target weight", text: $targetText, suffix: "kg", errorMessage: errors["target"])
            NumberField(
                title: "Daily food budget",
                text: $budgetText,
                suffix: "₹",
                errorMessage: errors["budget"],
                allowsDecimal: false
            )
            NumberField(
                title: "Calorie target override",
                text: $calorieOverrideText,
                suffix: "kcal",
                supportingText: "Leave blank to use the estimate from your profile.",
                errorMessage: errors["calories"],
                allowsDecimal: false
            )

            Text("Food preference").font(.subheadline.weight(.semibold))
            Picker("Food preference", selection: $dietPreference) {
                ForEach(DietPreference.allCases, id: \.self) { Text($0.label).tag($0) }
            }
            .pickerStyle(.segmented)

            Text("Activity level").font(.subheadline.weight(.semibold))
            Picker("Activity level", selection: $activityLevel) {
                ForEach(ActivityLevel.allCases, id: \.self) { Text($0.label).tag($0) }
            }
            .pickerStyle(.menu)
            Text(activityLevel.detail).font(.caption2).foregroundStyle(.secondary)

            DatePicker("Wake-up time", selection: $wakeDate, displayedComponents: .hourAndMinute)
            DatePicker("Sleep time", selection: $sleepDate, displayedComponents: .hourAndMinute)

            HStack(spacing: 10) {
                Button {
                    save()
                } label: {
                    Text("Save changes").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Button {
                    loadForm()
                    errors = [:]
                } label: {
                    Text("Undo").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
        .fitCard()
    }

    private var linksCard: some View {
        VStack(spacing: 0) {
            link("Settings", systemImage: "gearshape", route: .settings)
            Divider()
            link("Reminders", systemImage: "bell", route: .reminders)
            Divider()
            link("Food database", systemImage: "carrot", route: .foods)
            Divider()
            link("Privacy", systemImage: "lock", route: .privacy)
            Divider()
            link("About FitBudget", systemImage: "info.circle", route: .about)
        }
        .fitCard(padding: 0)
    }

    private func link(_ title: String, systemImage: String, route: Route) -> some View {
        Button {
            router.push(route, on: .profile)
        } label: {
            HStack {
                Label(title, systemImage: systemImage)
                    .font(.subheadline)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Logic

    private func loadForm() {
        guard let profile else { return }
        name = profile.name
        dateOfBirth = profile.dateOfBirth
        gender = profile.gender
        heightText = "\(Int(profile.heightCm))"
        weightText = String(format: "%.1f", profile.currentWeightKg)
        targetText = String(format: "%.1f", profile.targetWeightKg)
        budgetText = "\(Int(profile.dailyBudget))"
        calorieOverrideText = profile.calorieTargetOverride.map { "\(Int($0))" } ?? ""
        activityLevel = profile.activityLevel
        dietPreference = profile.dietPreference
        wakeDate = timeDate(minutes: profile.wakeMinutes)
        sleepDate = timeDate(minutes: profile.sleepMinutes)
    }

    private func save() {
        let height = Validators.parseDecimal(heightText)
        let weight = Validators.parseDecimal(weightText)
        let target = Validators.parseDecimal(targetText)
        let budget = Validators.parseDecimal(budgetText)
        let override = calorieOverrideText.isEmpty ? nil : Validators.parseDecimal(calorieOverrideText)

        var found: [String: String] = [:]
        if let message = Validators.name(name).message { found["name"] = message }
        if let message = Validators.dateOfBirth(dateOfBirth).message { found["dob"] = message }
        if let message = Validators.height(height).message { found["height"] = message }
        if let message = Validators.weight(weight, label: "Current weight").message {
            found["weight"] = message
        }
        if let message = Validators.targetWeight(target, currentKg: weight).message {
            found["target"] = message
        }
        if let message = Validators.budget(budget).message { found["budget"] = message }
        if !calorieOverrideText.isEmpty {
            if let message = Validators.calories(override).message {
                found["calories"] = message
            } else if let override, override < 1_000 {
                found["calories"] = "A manual target below 1,000 kcal is not safe. Leave it blank to use the estimate."
            }
        }

        guard found.isEmpty,
              let height, let weight, let target, let budget else {
            errors = found
            return
        }
        errors = [:]

        let previousPreference = profile?.dietPreference
        let previousWake = profile?.wakeMinutes
        let previousSleep = profile?.sleepMinutes
        let newWake = minutes(from: wakeDate)
        let newSleep = minutes(from: sleepDate)

        store.updateProfile { record in
            record.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
            record.dobDay = DayCalendar.dayKey(for: dateOfBirth).value
            record.gender = gender
            record.heightCm = height
            record.currentWeightKg = weight
            record.targetWeightKg = target
            record.dailyBudget = budget
            record.calorieTargetOverride = override
            record.activityLevel = activityLevel
            record.dietPreference = dietPreference
            record.wakeMinutes = newWake
            record.sleepMinutes = newSleep
        }

        Task {
            if previousPreference != dietPreference {
                store.regeneratePlan(for: store.todayKey)
            }
            if previousWake != newWake || previousSleep != newSleep {
                await store.rescheduleReminders()
            }
            store.show("Profile updated.")
        }
    }

    private func timeDate(minutes: Int) -> Date {
        var components = DateComponents()
        components.hour = minutes / 60
        components.minute = minutes % 60
        return DayCalendar.calendar().date(from: components) ?? Date()
    }

    private func minutes(from date: Date) -> Int {
        let components = DayCalendar.calendar().dateComponents([.hour, .minute], from: date)
        return (components.hour ?? 0) * 60 + (components.minute ?? 0)
    }
}
