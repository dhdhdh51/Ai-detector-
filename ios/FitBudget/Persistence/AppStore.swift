import Foundation
import Observation
import SwiftData
import FitBudgetCore
import OSLog

/// One meal section of the day (the four standard meals plus any custom meals).
struct MealGroup: Identifiable {
    let id: String
    let label: String
    let mealType: MealType?
    let minutesOfDay: Int
    let items: [MealEntryRecord]

    var calories: Double { items.reduce(0) { $0 + $1.totalCalories } }
    var protein: Double { items.reduce(0) { $0 + $1.totalProtein } }
    var cost: Double { items.reduce(0) { $0 + $1.totalCost } }
    var allCompleted: Bool { !items.isEmpty && items.allSatisfy(\.completed) }
    var completedCount: Int { items.filter(\.completed).count }
}

enum StoreError: LocalizedError {
    case duplicateFood(String)
    case missingProfile
    case invalidBackup

    var errorDescription: String? {
        switch self {
        case .duplicateFood(let name): return "\"\(name)\" is already in your food list."
        case .missingProfile: return "Your profile could not be loaded."
        case .invalidBackup: return "That file is not a FitBudget backup."
        }
    }
}

/// The app's single data service: it owns the SwiftData context, exposes observable state for the
/// views and performs every read and write. Mirrors the repository layer of the Android build.
@MainActor
@Observable
final class AppStore {

    static let logger = Logger(subsystem: "com.fitbudget.app", category: "AppStore")

    let container: ModelContainer
    private var context: ModelContext
    private let settingsStore: SettingsStore
    let notifications: NotificationService
    let pedometer: PedometerService

    // MARK: - Observable state

    var isReady = false
    var settings = AppSettings()
    var profile: ProfileRecord?
    var todayKey: DayKey = DayCalendar.today()

    var todaySummary = DaySummary(day: DayCalendar.today())
    var checklist: [ChecklistItem] = []
    var streaks = Streaks()
    var weekSteps: [Int] = []

    var foods: [FoodRecord] = []
    var reminders: [ReminderRecord] = []
    var weightLogs: [WeightLogRecord] = []
    var workoutHistory: [WorkoutSessionRecord] = []

    /// Day currently shown on the Diet tab (lets the user browse previous days).
    var dietDay: DayKey = DayCalendar.today()
    var dietGroups: [MealGroup] = []

    /// Transient banner message for the UI.
    var message: String?

    /// Collaborators are built inside the initialiser rather than as default arguments: default
    /// argument expressions are evaluated in a nonisolated context, which cannot call the
    /// `@MainActor` initialisers of these services.
    init(
        container: ModelContainer,
        settingsStore: SettingsStore? = nil,
        notifications: NotificationService? = nil,
        pedometer: PedometerService? = nil
    ) {
        let resolvedSettingsStore = settingsStore ?? SettingsStore()
        self.container = container
        self.context = ModelContext(container)
        self.settingsStore = resolvedSettingsStore
        self.notifications = notifications ?? NotificationService()
        self.pedometer = pedometer ?? PedometerService()
        self.settings = resolvedSettingsStore.load()
    }

    // MARK: - Bootstrap

    /// One-time startup work: seed the food database, install default reminders, make sure a
    /// profile row exists and today's plan is ready. Safe to call repeatedly.
    func bootstrap() async {
        todayKey = DayCalendar.today()
        dietDay = todayKey

        ensureProfile()
        seedFoodsIfEmpty()
        ensureDefaultReminders()

        if profile?.onboardingComplete == true {
            ensureDay(todayKey)
            ensurePlan(for: todayKey)
            await syncStepsFromSensor()
            await rescheduleReminders()
        }

        settings.lastRollOverDay = todayKey.value
        persistSettings()
        refresh()
        isReady = true
    }

    /// Called when the app returns to the foreground: picks up a date change and re-reads steps.
    func onForeground() async {
        let now = DayCalendar.today()
        let dayChanged = now != todayKey
        todayKey = now
        if dayChanged {
            dietDay = now
            ensureDay(now)
            ensurePlan(for: now)
            settings.lastRollOverDay = now.value
            persistSettings()
            await rescheduleReminders()
        }
        await syncStepsFromSensor()
        refresh()
    }

    /// Recomputes every piece of observable state from the database.
    func refresh() {
        save()
        profile = fetchProfile()
        foods = fetchAll(FoodRecord.self, sortBy: [SortDescriptor(\.name)])
        reminders = fetchAll(ReminderRecord.self).sorted { $0.kind.sortIndex < $1.kind.sortIndex }
        weightLogs = fetchAll(WeightLogRecord.self, sortBy: [SortDescriptor(\.day, order: .reverse)])
        workoutHistory = fetchAll(
            WorkoutSessionRecord.self,
            sortBy: [SortDescriptor(\.finishedAt, order: .reverse)]
        )
        todaySummary = summary(for: todayKey)
        checklist = DailyChecklist.build(todaySummary)
        streaks = computeStreaks(lookBackDays: 180)
        weekSteps = summaries(from: todayKey - 6, to: todayKey).map(\.steps)
        dietGroups = groups(for: dietDay)
    }

    func show(_ text: String) {
        message = text
    }

    // MARK: - Low-level helpers

    private func save() {
        guard context.hasChanges else { return }
        do {
            try context.save()
        } catch {
            Self.logger.error("Save failed: \(error.localizedDescription, privacy: .public)")
        }
    }

    private func fetchAll<T: PersistentModel>(
        _ type: T.Type,
        sortBy: [SortDescriptor<T>] = []
    ) -> [T] {
        let descriptor = FetchDescriptor<T>(sortBy: sortBy)
        return (try? context.fetch(descriptor)) ?? []
    }

    private func fetchProfile() -> ProfileRecord? {
        fetchAll(ProfileRecord.self).first
    }

    // MARK: - Profile

    @discardableResult
    func ensureProfile() -> ProfileRecord {
        if let existing = fetchProfile() {
            profile = existing
            return existing
        }
        let fresh = ProfileRecord()
        context.insert(fresh)
        save()
        profile = fresh
        return fresh
    }

    /// Persists the onboarding answers. The start weight is fixed here so progress is always
    /// measured from a real starting point.
    func completeOnboarding(
        name: String,
        dateOfBirth: Date,
        gender: Gender,
        heightCm: Double,
        weightKg: Double,
        targetKg: Double,
        dailyBudget: Double,
        activityLevel: ActivityLevel,
        dietPreference: DietPreference,
        wakeMinutes: Int,
        sleepMinutes: Int
    ) async {
        let record = ensureProfile()
        record.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        record.dobDay = DayCalendar.dayKey(for: dateOfBirth).value
        record.gender = gender
        record.heightCm = heightCm
        record.startWeightKg = weightKg
        record.currentWeightKg = weightKg
        record.targetWeightKg = targetKg
        record.dailyBudget = dailyBudget
        record.activityLevel = activityLevel
        record.dietPreference = dietPreference
        record.wakeMinutes = wakeMinutes
        record.sleepMinutes = sleepMinutes
        record.onboardingComplete = true
        record.updatedAt = Date()
        save()

        // The first weighing is real data the user just entered, so it is logged as day one.
        logWeight(weightKg, on: todayKey, note: "Starting weight")

        settings.waterTargetMl = HealthCalculator.suggestedWaterMl(weightKg: weightKg)
        persistSettings()

        ensureDay(todayKey)
        ensurePlan(for: todayKey)
        await rescheduleReminders()
        refresh()
    }

    func updateProfile(_ mutate: (ProfileRecord) -> Void) {
        let record = ensureProfile()
        mutate(record)
        record.updatedAt = Date()
        save()
        refresh()
    }

    func setDailyBudget(_ budget: Double) {
        updateProfile { $0.dailyBudget = budget }
        applyBudgetToToday(budget)
        refresh()
    }

    func setTargetWeight(_ targetKg: Double) {
        updateProfile { $0.targetWeightKg = targetKg }
    }

    func setCalorieOverride(_ calories: Double?) {
        updateProfile { $0.calorieTargetOverride = calories }
    }

    func setDietPreference(_ preference: DietPreference) {
        updateProfile { $0.dietPreference = preference }
        regeneratePlan(for: todayKey)
        refresh()
    }

    // MARK: - Settings

    func persistSettings() {
        settingsStore.save(settings)
    }

    func setWaterTarget(_ ml: Int) {
        settings.waterTargetMl = ml
        persistSettings()
        if let meta = fetchDayMeta(todayKey) { meta.waterTargetMl = ml }
        refresh()
    }

    func setStepGoal(_ goal: Int) {
        settings.stepGoal = goal
        persistSettings()
        if let meta = fetchDayMeta(todayKey) { meta.stepGoal = goal }
        refresh()
    }

    func setThemeMode(_ mode: ThemeMode) {
        settings.themeMode = mode
        persistSettings()
    }

    func setUnitSystem(_ units: UnitSystem) {
        settings.unitSystem = units
        persistSettings()
    }

    func toggleExcludedFood(_ nameKey: String) {
        let key = nameKey.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !key.isEmpty else { return }
        if settings.excludedFoodKeys.contains(key) {
            settings.excludedFoodKeys.remove(key)
        } else {
            settings.excludedFoodKeys.insert(key)
        }
        persistSettings()
        refresh()
    }

    // MARK: - Day metadata

    private func fetchDayMeta(_ day: DayKey) -> DayMetaRecord? {
        let value = day.value
        let descriptor = FetchDescriptor<DayMetaRecord>(predicate: #Predicate { $0.day == value })
        return (try? context.fetch(descriptor))?.first
    }

    @discardableResult
    func ensureDay(_ day: DayKey) -> DayMetaRecord {
        if let existing = fetchDayMeta(day) { return existing }
        let currentProfile = ensureProfile()
        let meta = DayMetaRecord(
            day: day.value,
            budget: currentProfile.dailyBudget,
            waterTargetMl: settings.waterTargetMl,
            stepGoal: settings.stepGoal,
            calorieTarget: currentProfile.estimatedCalorieTarget
        )
        context.insert(meta)
        save()
        return meta
    }

    private func applyBudgetToToday(_ budget: Double) {
        let meta = ensureDay(todayKey)
        meta.budget = budget
        save()
    }

    private func dayMetas(from: DayKey, to: DayKey) -> [Int: DayMetaRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<DayMetaRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper }
        )
        let rows = (try? context.fetch(descriptor)) ?? []
        return Dictionary(rows.map { ($0.day, $0) }, uniquingKeysWith: { first, _ in first })
    }

    // MARK: - Diet

    func meals(for day: DayKey) -> [MealEntryRecord] {
        let value = day.value
        let descriptor = FetchDescriptor<MealEntryRecord>(
            predicate: #Predicate { $0.day == value },
            sortBy: [SortDescriptor(\.minutesOfDay), SortDescriptor(\.sortOrder)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func groups(for day: DayKey) -> [MealGroup] {
        let entries = meals(for: day)
        guard !entries.isEmpty else { return [] }

        var result: [MealGroup] = []
        for type in MealType.allCases {
            let items = entries.filter { $0.mealType == type }
            guard !items.isEmpty else { continue }
            result.append(
                MealGroup(
                    id: type.rawValue,
                    label: type.label,
                    mealType: type,
                    minutesOfDay: items.map(\.minutesOfDay).min() ?? type.defaultMinutesOfDay,
                    items: items.sorted { $0.sortOrder < $1.sortOrder }
                )
            )
        }

        let customGrouped = Dictionary(grouping: entries.filter { $0.mealType == nil }) { $0.mealLabel }
        for (label, items) in customGrouped {
            result.append(
                MealGroup(
                    id: "CUSTOM:\(label)",
                    label: label,
                    mealType: nil,
                    minutesOfDay: items.map(\.minutesOfDay).min() ?? 0,
                    items: items.sorted { $0.sortOrder < $1.sortOrder }
                )
            )
        }
        return result.sorted { $0.minutesOfDay < $1.minutesOfDay }
    }

    func selectDietDay(_ day: DayKey) {
        dietDay = day
        ensurePlan(for: day)
        refresh()
    }

    /// Generates the day's plan the first time a day is opened. Does nothing if the user already
    /// has entries, or if they deliberately cleared the generated plan.
    func ensurePlan(for day: DayKey) {
        let meta = ensureDay(day)
        if meta.planGenerated { return }
        if !meals(for: day).isEmpty {
            meta.planGenerated = true
            save()
            return
        }
        generatePlan(for: day, meta: meta)
    }

    /// Explicit user action: throw away the standard meals and build a fresh plan.
    func regeneratePlan(for day: DayKey) {
        for entry in meals(for: day) where entry.mealType != nil {
            context.delete(entry)
        }
        save()
        let meta = ensureDay(day)
        generatePlan(for: day, meta: meta)
        refresh()
    }

    private func generatePlan(for day: DayKey, meta: DayMetaRecord) {
        let currentProfile = ensureProfile()
        let items = fetchAll(FoodRecord.self).map(\.item)
        let request = MealPlanGenerator.Request(
            day: day,
            calorieTarget: meta.calorieTarget > 0 ? meta.calorieTarget : currentProfile.estimatedCalorieTarget,
            dailyBudget: meta.budget > 0 ? meta.budget : currentProfile.dailyBudget,
            preference: currentProfile.dietPreference,
            excludedKeys: settings.excludedFoodKeys
        )
        for planned in MealPlanGenerator.generate(request: request, foods: items) {
            context.insert(MealEntryRecord(planned: planned))
        }
        meta.planGenerated = true
        save()
    }

    func setItemCompleted(_ item: MealEntryRecord, completed: Bool) {
        item.completed = completed
        item.completedAt = completed ? Date() : nil
        save()
        refresh()
    }

    func setMealCompleted(_ group: MealGroup, completed: Bool) {
        for item in group.items {
            item.completed = completed
            item.completedAt = completed ? Date() : nil
        }
        save()
        refresh()
    }

    /// Used by the notification "Mark eaten" action.
    func setMealCompleted(day: DayKey, mealType: MealType, completed: Bool) {
        ensurePlan(for: day)
        for item in meals(for: day) where item.mealType == mealType {
            item.completed = completed
            item.completedAt = completed ? Date() : nil
        }
        save()
        refresh()
    }

    func updateQuantity(_ item: MealEntryRecord, quantity: Double) {
        let result = Validators.quantity(quantity)
        guard result.isValid else {
            show(result.message ?? "Invalid quantity.")
            return
        }
        item.quantity = quantity
        save()
        refresh()
    }

    func delete(_ item: MealEntryRecord) {
        let name = item.foodName
        context.delete(item)
        save()
        refresh()
        show("\(name) removed.")
    }

    func clearDay(_ day: DayKey) {
        for entry in meals(for: day) { context.delete(entry) }
        save()
        refresh()
        show("Plan cleared.")
    }

    /// Swaps the food on an existing line, keeping the meal, time and position.
    func replaceFood(_ item: MealEntryRecord, with food: FoodRecord) {
        item.foodNameKey = food.nameKey
        item.foodName = food.name
        item.servingLabel = food.servingLabel
        item.quantity = 1
        item.caloriesPerServing = food.calories
        item.proteinPerServing = food.proteinG
        item.costPerServing = food.costRupees
        item.completed = false
        item.completedAt = nil
        save()
        refresh()
        show("Swapped for \(food.name).")
    }

    func addFood(_ food: FoodRecord, to group: MealGroup, quantity: Double = 1) {
        addFood(
            food,
            day: dietDay,
            mealType: group.mealType,
            mealLabel: group.label,
            minutesOfDay: group.minutesOfDay,
            quantity: quantity
        )
        show("\(food.name) added to \(group.label).")
    }

    /// A custom meal is just a group of entries with no `MealType` and a user-chosen label.
    func addCustomMeal(
        label: String,
        hour: Int,
        minute: Int,
        food: FoodRecord,
        quantity: Double = 1
    ) {
        let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
        let name = trimmed.isEmpty ? "Custom meal" : trimmed
        addFood(
            food,
            day: dietDay,
            mealType: nil,
            mealLabel: name,
            minutesOfDay: hour * 60 + minute,
            quantity: quantity
        )
        show("\(name) added.")
    }

    private func addFood(
        _ food: FoodRecord,
        day: DayKey,
        mealType: MealType?,
        mealLabel: String,
        minutesOfDay: Int,
        quantity: Double
    ) {
        ensureDay(day)
        let nextOrder = (meals(for: day).map(\.sortOrder).max() ?? 0) + 1
        context.insert(
            MealEntryRecord(
                day: day.value,
                mealType: mealType,
                mealLabel: mealLabel,
                minutesOfDay: minutesOfDay,
                foodNameKey: food.nameKey,
                foodName: food.name,
                servingLabel: food.servingLabel,
                quantity: quantity,
                caloriesPerServing: food.calories,
                proteinPerServing: food.proteinG,
                costPerServing: food.costRupees,
                sortOrder: nextOrder
            )
        )
        save()
        refresh()
    }

    func updateMealTime(_ group: MealGroup, hour: Int, minute: Int) {
        let minutes = hour * 60 + minute
        for item in group.items { item.minutesOfDay = minutes }
        save()
        refresh()
    }

    // MARK: - Water

    func waterLogs(for day: DayKey) -> [WaterLogRecord] {
        let value = day.value
        let descriptor = FetchDescriptor<WaterLogRecord>(
            predicate: #Predicate { $0.day == value },
            sortBy: [SortDescriptor(\.loggedAt, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func waterTotal(for day: DayKey) -> Int {
        waterLogs(for: day).reduce(0) { $0 + $1.amountMl }
    }

    func addWater(_ amountMl: Int, on day: DayKey? = nil) {
        let result = Validators.waterEntry(amountMl)
        guard result.isValid else {
            show(result.message ?? "Invalid amount.")
            return
        }
        let target = day ?? todayKey
        ensureDay(target)
        context.insert(WaterLogRecord(day: target.value, amountMl: amountMl))
        save()
        refresh()
    }

    /// Removes the most recent entry of the day - the "undo" for a mis-tap.
    func undoLastWater(on day: DayKey? = nil) {
        let target = day ?? todayKey
        guard let last = waterLogs(for: target).first else {
            show("Nothing logged yet today.")
            return
        }
        context.delete(last)
        save()
        refresh()
        show("Last entry removed.")
    }

    func delete(_ log: WaterLogRecord) {
        context.delete(log)
        save()
        refresh()
    }

    func clearWater(on day: DayKey) {
        for log in waterLogs(for: day) { context.delete(log) }
        save()
        refresh()
        show("Today's water log cleared.")
    }

    // MARK: - Weight

    func weightLog(for day: DayKey) -> WeightLogRecord? {
        let value = day.value
        let descriptor = FetchDescriptor<WeightLogRecord>(predicate: #Predicate { $0.day == value })
        return (try? context.fetch(descriptor))?.first
    }

    /// Logs a weighing. The profile's current weight stays in sync with the most recent entry so
    /// BMI and progress always reflect real data.
    func logWeight(_ weightKg: Double, on day: DayKey, note: String? = nil) {
        let cleanedNote = note?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let existing = weightLog(for: day) {
            existing.weightKg = weightKg
            existing.note = (cleanedNote?.isEmpty ?? true) ? nil : cleanedNote
        } else {
            context.insert(
                WeightLogRecord(
                    day: day.value,
                    weightKg: weightKg,
                    note: (cleanedNote?.isEmpty ?? true) ? nil : cleanedNote
                )
            )
        }
        save()
        syncProfileWithLatestWeight()
        refresh()
    }

    func delete(_ log: WeightLogRecord) {
        context.delete(log)
        save()
        syncProfileWithLatestWeight()
        refresh()
    }

    private func syncProfileWithLatestWeight() {
        let logs = fetchAll(WeightLogRecord.self, sortBy: [SortDescriptor(\.day, order: .reverse)])
        guard let latest = logs.first else { return }
        let record = ensureProfile()
        record.currentWeightKg = latest.weightKg
        record.updatedAt = Date()
        save()
    }

    // MARK: - Steps

    func stepLog(for day: DayKey) -> StepLogRecord? {
        let value = day.value
        let descriptor = FetchDescriptor<StepLogRecord>(predicate: #Predicate { $0.day == value })
        return (try? context.fetch(descriptor))?.first
    }

    @discardableResult
    private func ensureStepLog(for day: DayKey) -> StepLogRecord {
        if let existing = stepLog(for: day) { return existing }
        ensureDay(day)
        let record = StepLogRecord(day: day.value)
        context.insert(record)
        save()
        return record
    }

    /// Reads today's step count from CoreMotion. `CMPedometer` keeps the history itself, so the
    /// app can simply ask for "today" instead of running a background service.
    func syncStepsFromSensor() async {
        guard pedometer.isAvailable, pedometer.isAuthorized else { return }
        let start = DayCalendar.date(from: todayKey)
        guard let steps = await pedometer.stepCount(from: start, to: Date()) else { return }
        let record = ensureStepLog(for: todayKey)
        // The sensor is the source of truth for its own number; manual steps stay separate.
        record.sensorSteps = max(steps, 0)
        record.updatedAt = Date()
        save()
    }

    /// Manual entry used when the device has no motion sensor, or the user walked without it.
    func setManualSteps(_ steps: Int, on day: DayKey? = nil) {
        let result = Validators.steps(steps)
        guard result.isValid else {
            show(result.message ?? "Invalid step count.")
            return
        }
        let target = day ?? todayKey
        let record = ensureStepLog(for: target)
        record.manualSteps = min(max(steps, 0), 100_000)
        record.updatedAt = Date()
        save()
        refresh()
        show("Manual steps set to \(steps).")
    }

    func addManualSteps(_ amount: Int, on day: DayKey? = nil) {
        let target = day ?? todayKey
        let existing = stepLog(for: target)?.manualSteps ?? 0
        setManualSteps(existing + amount, on: target)
    }

    func requestMotionAccess() async {
        await pedometer.requestAuthorization()
        await syncStepsFromSensor()
        refresh()
    }

    // MARK: - Workouts

    func workoutCount(for day: DayKey) -> Int {
        let value = day.value
        let descriptor = FetchDescriptor<WorkoutSessionRecord>(predicate: #Predicate { $0.day == value })
        return ((try? context.fetch(descriptor)) ?? []).count
    }

    /// Saves a finished session. Calories are a rough estimate scaled by how much was completed.
    func saveWorkout(
        template: WorkoutTemplate,
        startedAt: Date,
        finishedAt: Date,
        durationSeconds: Int,
        completed: Int,
        skipped: Int,
        note: String? = nil
    ) {
        ensureDay(todayKey)
        let total = max(template.exercises.count, 1)
        let fraction = min(max(Double(completed) / Double(total), 0), 1)
        context.insert(
            WorkoutSessionRecord(
                day: todayKey.value,
                templateID: template.id,
                templateName: template.name,
                category: template.category,
                startedAt: startedAt,
                finishedAt: finishedAt,
                durationSeconds: durationSeconds,
                exercisesCompleted: completed,
                exercisesSkipped: skipped,
                exercisesTotal: template.exercises.count,
                estimatedCalories: Int(Double(template.estimatedCalories) * fraction),
                note: note?.trimmingCharacters(in: .whitespacesAndNewlines)
            )
        )
        save()
        refresh()
        show("Workout saved — \(completed) of \(template.exercises.count) exercises done.")
    }

    func delete(_ session: WorkoutSessionRecord) {
        context.delete(session)
        save()
        refresh()
        show("Session removed from history.")
    }

    // MARK: - Expenses

    func expenses(for day: DayKey) -> [ExpenseRecord] {
        let value = day.value
        let descriptor = FetchDescriptor<ExpenseRecord>(
            predicate: #Predicate { $0.day == value },
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func addExpense(label: String, amount: Double, on day: DayKey? = nil) {
        let result = Validators.cost(amount)
        guard result.isValid else {
            show(result.message ?? "Invalid amount.")
            return
        }
        let target = day ?? todayKey
        ensureDay(target)
        let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
        context.insert(
            ExpenseRecord(
                day: target.value,
                label: trimmed.isEmpty ? "Extra food spend" : trimmed,
                amount: max(amount, 0)
            )
        )
        save()
        refresh()
        show("Added \(Formatters.rupees(amount)) to today's spending.")
    }

    func delete(_ expense: ExpenseRecord) {
        context.delete(expense)
        save()
        refresh()
        show("Expense removed.")
    }

    // MARK: - Food database

    private func seedFoodsIfEmpty() {
        guard fetchAll(FoodRecord.self).isEmpty else { return }
        for item in FoodSeed.items {
            context.insert(FoodRecord(item: item))
        }
        save()
    }

    func food(withKey key: String) -> FoodRecord? {
        let descriptor = FetchDescriptor<FoodRecord>(predicate: #Predicate { $0.nameKey == key })
        return (try? context.fetch(descriptor))?.first
    }

    /// Adds a user food. Duplicate names are reported as a friendly failure.
    func addCustomFood(
        name: String,
        servingLabel: String,
        calories: Double,
        proteinG: Double,
        costRupees: Double,
        category: FoodCategory,
        role: FoodRole
    ) -> Result<Void, StoreError> {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let key = FoodItem.key(for: trimmed)
        if food(withKey: key) != nil {
            return .failure(.duplicateFood(trimmed))
        }
        let item = FoodItem(
            name: trimmed,
            servingLabel: servingLabel.trimmingCharacters(in: .whitespacesAndNewlines),
            calories: calories,
            proteinG: proteinG,
            costRupees: costRupees,
            category: category,
            role: role,
            isCustom: true
        )
        context.insert(FoodRecord(item: item))
        save()
        refresh()
        return .success(())
    }

    func updateFood(
        _ record: FoodRecord,
        name: String,
        servingLabel: String,
        calories: Double,
        proteinG: Double,
        costRupees: Double,
        category: FoodCategory,
        role: FoodRole
    ) -> Result<Void, StoreError> {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let key = FoodItem.key(for: trimmed)
        if key != record.nameKey, food(withKey: key) != nil {
            return .failure(.duplicateFood(trimmed))
        }
        record.name = trimmed
        record.nameKey = key
        record.servingLabel = servingLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        record.calories = calories
        record.proteinG = proteinG
        record.costRupees = costRupees
        record.category = category
        record.role = role
        save()
        refresh()
        return .success(())
    }

    func delete(_ food: FoodRecord) {
        let name = food.name
        context.delete(food)
        save()
        refresh()
        show("\(name) deleted.")
    }

    func restoreSeedFoods() {
        for record in fetchAll(FoodRecord.self) { context.delete(record) }
        save()
        seedFoodsIfEmpty()
        refresh()
        show("Built-in food list restored. Your custom foods were removed.")
    }

    // MARK: - Reminders

    private func ensureDefaultReminders() {
        let existing = Set(fetchAll(ReminderRecord.self).map(\.kindRaw))
        for kind in ReminderKind.allCases where !existing.contains(kind.rawValue) {
            context.insert(ReminderRecord(kind: kind))
        }
        save()
    }

    func reminder(_ kind: ReminderKind) -> ReminderRecord? {
        reminders.first { $0.kind == kind } ?? fetchAll(ReminderRecord.self).first { $0.kind == kind }
    }

    func updateReminder(_ kind: ReminderKind, _ mutate: (ReminderRecord) -> Void) async {
        ensureDefaultReminders()
        guard let record = reminder(kind) else { return }
        mutate(record)
        save()
        await rescheduleReminders()
        refresh()
    }

    func setRemindersEnabled(_ enabled: Bool) async {
        settings.remindersEnabled = enabled
        persistSettings()
        await rescheduleReminders()
        refresh()
        show(enabled ? "Reminders switched on." : "All reminders paused.")
    }

    func setNotificationSound(_ enabled: Bool) async {
        settings.notificationSoundEnabled = enabled
        persistSettings()
        for record in fetchAll(ReminderRecord.self) { record.soundEnabled = enabled }
        save()
        await rescheduleReminders()
        refresh()
    }

    /// Rebuilds every pending notification from the database.
    func rescheduleReminders() async {
        ensureDefaultReminders()
        let currentProfile = ensureProfile()
        await notifications.reschedule(
            reminders: fetchAll(ReminderRecord.self).map { record in
                ReminderPlan(
                    kind: record.kind,
                    enabled: record.enabled,
                    hour: record.hour,
                    minute: record.minute,
                    daysMask: record.daysMask,
                    intervalMinutes: record.intervalMinutes,
                    soundEnabled: record.soundEnabled
                )
            },
            wakeMinutes: currentProfile.wakeMinutes,
            sleepMinutes: currentProfile.sleepMinutes,
            masterEnabled: settings.remindersEnabled
        )
    }

    // MARK: - Reset

    func resetAllData() async {
        await notifications.cancelAll()
        for model in fetchAll(MealEntryRecord.self) { context.delete(model) }
        for model in fetchAll(WeightLogRecord.self) { context.delete(model) }
        for model in fetchAll(WaterLogRecord.self) { context.delete(model) }
        for model in fetchAll(StepLogRecord.self) { context.delete(model) }
        for model in fetchAll(WorkoutSessionRecord.self) { context.delete(model) }
        for model in fetchAll(ExpenseRecord.self) { context.delete(model) }
        for model in fetchAll(DayMetaRecord.self) { context.delete(model) }
        for model in fetchAll(ReminderRecord.self) { context.delete(model) }
        for model in fetchAll(FoodRecord.self) { context.delete(model) }
        for model in fetchAll(ProfileRecord.self) { context.delete(model) }
        save()

        settingsStore.clear()
        settings = settingsStore.load()
        profile = nil

        ensureProfile()
        seedFoodsIfEmpty()
        ensureDefaultReminders()
        clearExportDirectory()
        refresh()
        show("All local data has been erased.")
    }

    // MARK: - Stats plumbing used by the extension

    func fetchMeals(from: DayKey, to: DayKey) -> [MealEntryRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<MealEntryRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchWater(from: DayKey, to: DayKey) -> [WaterLogRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<WaterLogRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchSteps(from: DayKey, to: DayKey) -> [StepLogRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<StepLogRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchWorkouts(from: DayKey, to: DayKey) -> [WorkoutSessionRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<WorkoutSessionRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchWeights(from: DayKey, to: DayKey) -> [WeightLogRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<WeightLogRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper },
            sortBy: [SortDescriptor(\.day)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchExpenses(from: DayKey, to: DayKey) -> [ExpenseRecord] {
        let lower = from.value
        let upper = to.value
        let descriptor = FetchDescriptor<ExpenseRecord>(
            predicate: #Predicate { $0.day >= lower && $0.day <= upper }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchGoalSnapshots(from: DayKey, to: DayKey) -> [Int: DayMetaRecord] {
        dayMetas(from: from, to: to)
    }

    func allRecords<T: PersistentModel>(_ type: T.Type, sortBy: [SortDescriptor<T>] = []) -> [T] {
        fetchAll(type, sortBy: sortBy)
    }

    func insert<T: PersistentModel>(_ model: T) {
        context.insert(model)
    }

    func deleteAll<T: PersistentModel>(_ type: T.Type) {
        for model in fetchAll(type) { context.delete(model) }
    }

    func commit() {
        save()
    }
}
