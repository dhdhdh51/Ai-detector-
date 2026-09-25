import Foundation
import FitBudgetCore

struct RestoreSummary {
    let weightLogs: Int
    let mealEntries: Int
    let waterLogs: Int
    let workouts: Int
    let stepDays: Int
    let expenses: Int
    let customFoods: Int
}

/// Codable mirror of the whole local database. The format is deliberately identical in meaning to
/// the Android build's backup so a user can move between the two apps.
private struct BackupFile: Codable {
    struct Profile: Codable {
        var name: String
        var dobDay: Int
        var gender: String
        var heightCm: Double
        var startWeightKg: Double
        var currentWeightKg: Double
        var targetWeightKg: Double
        var dailyBudget: Double
        var activityLevel: String
        var wakeMinutes: Int
        var sleepMinutes: Int
        var dietPreference: String
        var calorieTargetOverride: Double?
        var onboardingComplete: Bool
    }

    struct Food: Codable {
        var name: String
        var servingLabel: String
        var calories: Double
        var proteinG: Double
        var carbsG: Double
        var fatG: Double
        var costRupees: Double
        var category: String
        var role: String
    }

    struct Meal: Codable {
        var epochDay: Int
        var mealType: String?
        var mealLabel: String
        var timeMinutes: Int
        var foodName: String
        var servingLabel: String
        var quantity: Double
        var caloriesPerServing: Double
        var proteinPerServing: Double
        var costPerServing: Double
        var completed: Bool
        var sortOrder: Int
    }

    struct Weight: Codable {
        var epochDay: Int
        var weightKg: Double
        var note: String?
    }

    struct Water: Codable {
        var epochDay: Int
        var amountMl: Int
    }

    struct Step: Codable {
        var epochDay: Int
        var sensorSteps: Int
        var manualSteps: Int
    }

    struct Workout: Codable {
        var epochDay: Int
        var templateId: String
        var templateName: String
        var category: String
        var durationSeconds: Int
        var exercisesCompleted: Int
        var exercisesSkipped: Int
        var exercisesTotal: Int
        var estimatedCalories: Int
        var note: String?
    }

    struct Expense: Codable {
        var epochDay: Int
        var label: String
        var amount: Double
    }

    struct DayMeta: Codable {
        var epochDay: Int
        var budget: Double
        var waterTargetMl: Int
        var stepGoal: Int
        var calorieTarget: Double
        var planGenerated: Bool
    }

    struct Reminder: Codable {
        var type: String
        var enabled: Bool
        var hour: Int
        var minute: Int
        var daysMask: Int
        var intervalMinutes: Int
        var soundEnabled: Bool
        var vibrationEnabled: Bool
    }

    struct Settings: Codable {
        var themeMode: String
        var unitSystem: String
        var waterTargetMl: Int
        var stepGoal: Int
        var remindersEnabled: Bool
        var notificationSoundEnabled: Bool
        var excludedFoodKeys: [String]
    }

    var format: String
    var appVersion: Int
    var exportedAtMillis: Double
    var platform: String?
    var profile: Profile?
    var customFoods: [Food]
    var mealEntries: [Meal]
    var weightLogs: [Weight]
    var waterLogs: [Water]
    var stepLogs: [Step]
    var workouts: [Workout]
    var expenses: [Expense]
    var dayMeta: [DayMeta]
    var reminders: [Reminder]
    var settings: Settings?
}

extension AppStore {

    static let backupFormat = "fitbudget-backup-v1"

    var exportDirectory: URL {
        let base = FileManager.default.temporaryDirectory.appendingPathComponent("exports", isDirectory: true)
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        return base
    }

    func clearExportDirectory() {
        let contents = try? FileManager.default.contentsOfDirectory(
            at: exportDirectory,
            includingPropertiesForKeys: nil
        )
        for url in contents ?? [] {
            try? FileManager.default.removeItem(at: url)
        }
    }

    // MARK: - Reports

    func writeMonthlyReport(for monthDay: DayKey) throws -> URL {
        let report = monthlyReport(for: monthDay)
        let url = exportDirectory.appendingPathComponent("FitBudget-report-\(report.title).txt")
        try report.shareText().write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    /// Human readable CSV with one section per tracked data type.
    func writeCSVExport() throws -> URL {
        var text = "FitBudget data export\n"
        text += "Generated,\(Formatters.fullDate(DayCalendar.today()))\n\n"

        if let record = profile ?? allRecords(ProfileRecord.self).first {
            text += "[Profile]\n"
            text += "Name,DateOfBirth,Gender,HeightCm,StartWeightKg,CurrentWeightKg,"
            text += "TargetWeightKg,DailyBudget,ActivityLevel,DietPreference\n"
            let fields: [String] = [
                csv(record.name),
                Formatters.fullDate(DayKey(record.dobDay)),
                record.gender.rawValue,
                "\(record.heightCm)",
                "\(record.startWeightKg)",
                "\(record.currentWeightKg)",
                "\(record.targetWeightKg)",
                "\(record.dailyBudget)",
                record.activityLevel.rawValue,
                record.dietPreference.rawValue
            ]
            text += fields.joined(separator: ",") + "\n\n"
        }

        text += "[Weight log]\n"
        text += "Date,WeightKg,Note\n"
        for log in allRecords(WeightLogRecord.self, sortBy: [SortDescriptor(\.day)]) {
            text += "\(Formatters.fullDate(DayKey(log.day))),\(log.weightKg),\(csv(log.note ?? ""))\n"
        }
        text += "\n"

        text += "[Meals]\n"
        text += "Date,Meal,Food,Servings,Calories,ProteinG,CostRupees,Completed\n"
        for entry in allRecords(MealEntryRecord.self, sortBy: [SortDescriptor(\.day)]) {
            let fields: [String] = [
                Formatters.fullDate(DayKey(entry.day)),
                csv(entry.mealLabel),
                csv(entry.foodName),
                "\(entry.quantity)",
                String(format: "%.0f", entry.totalCalories),
                String(format: "%.1f", entry.totalProtein),
                String(format: "%.2f", entry.totalCost),
                "\(entry.completed)"
            ]
            text += fields.joined(separator: ",") + "\n"
        }
        text += "\n"

        text += "[Water]\n"
        text += "Date,AmountMl\n"
        for log in allRecords(WaterLogRecord.self, sortBy: [SortDescriptor(\.day)]) {
            text += "\(Formatters.fullDate(DayKey(log.day))),\(log.amountMl)\n"
        }
        text += "\n"

        text += "[Steps]\n"
        text += "Date,SensorSteps,ManualSteps,Total\n"
        for log in allRecords(StepLogRecord.self, sortBy: [SortDescriptor(\.day)]) {
            text += "\(Formatters.fullDate(DayKey(log.day))),\(log.sensorSteps),"
            text += "\(log.manualSteps),\(log.totalSteps)\n"
        }
        text += "\n"

        text += "[Workouts]\n"
        text += "Date,Workout,Category,DurationSeconds,Completed,Skipped,Total,EstimatedCalories\n"
        for session in allRecords(WorkoutSessionRecord.self, sortBy: [SortDescriptor(\.day)]) {
            let fields: [String] = [
                Formatters.fullDate(DayKey(session.day)),
                csv(session.templateName),
                session.category.rawValue,
                "\(session.durationSeconds)",
                "\(session.exercisesCompleted)",
                "\(session.exercisesSkipped)",
                "\(session.exercisesTotal)",
                "\(session.estimatedCalories)"
            ]
            text += fields.joined(separator: ",") + "\n"
        }
        text += "\n"

        text += "[Extra expenses]\n"
        text += "Date,Label,Amount\n"
        for expense in allRecords(ExpenseRecord.self, sortBy: [SortDescriptor(\.day)]) {
            text += "\(Formatters.fullDate(DayKey(expense.day))),\(csv(expense.label)),\(expense.amount)\n"
        }
        text += "\nAll nutrition and calorie values are estimates.\n"

        let url = exportDirectory.appendingPathComponent("FitBudget-export-\(DayCalendar.today().value).csv")
        try text.write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    private func csv(_ value: String) -> String {
        guard value.contains(",") || value.contains("\"") else { return value }
        return "\"" + value.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }

    // MARK: - Backup

    func writeBackup() throws -> URL {
        let file = BackupFile(
            format: Self.backupFormat,
            appVersion: 1,
            exportedAtMillis: Date().timeIntervalSince1970 * 1_000,
            platform: "ios",
            profile: (profile ?? allRecords(ProfileRecord.self).first).map { record in
                BackupFile.Profile(
                    name: record.name,
                    dobDay: record.dobDay,
                    gender: record.gender.rawValue,
                    heightCm: record.heightCm,
                    startWeightKg: record.startWeightKg,
                    currentWeightKg: record.currentWeightKg,
                    targetWeightKg: record.targetWeightKg,
                    dailyBudget: record.dailyBudget,
                    activityLevel: record.activityLevel.rawValue,
                    wakeMinutes: record.wakeMinutes,
                    sleepMinutes: record.sleepMinutes,
                    dietPreference: record.dietPreference.rawValue,
                    calorieTargetOverride: record.calorieTargetOverride,
                    onboardingComplete: record.onboardingComplete
                )
            },
            customFoods: allRecords(FoodRecord.self).filter(\.isCustom).map { record in
                BackupFile.Food(
                    name: record.name,
                    servingLabel: record.servingLabel,
                    calories: record.calories,
                    proteinG: record.proteinG,
                    carbsG: record.carbsG,
                    fatG: record.fatG,
                    costRupees: record.costRupees,
                    category: record.category.rawValue,
                    role: record.role.rawValue
                )
            },
            mealEntries: allRecords(MealEntryRecord.self).map { entry in
                BackupFile.Meal(
                    epochDay: entry.day,
                    mealType: entry.mealTypeRaw,
                    mealLabel: entry.mealLabel,
                    timeMinutes: entry.minutesOfDay,
                    foodName: entry.foodName,
                    servingLabel: entry.servingLabel,
                    quantity: entry.quantity,
                    caloriesPerServing: entry.caloriesPerServing,
                    proteinPerServing: entry.proteinPerServing,
                    costPerServing: entry.costPerServing,
                    completed: entry.completed,
                    sortOrder: entry.sortOrder
                )
            },
            weightLogs: allRecords(WeightLogRecord.self).map {
                BackupFile.Weight(epochDay: $0.day, weightKg: $0.weightKg, note: $0.note)
            },
            waterLogs: allRecords(WaterLogRecord.self).map {
                BackupFile.Water(epochDay: $0.day, amountMl: $0.amountMl)
            },
            stepLogs: allRecords(StepLogRecord.self).map {
                BackupFile.Step(epochDay: $0.day, sensorSteps: $0.sensorSteps, manualSteps: $0.manualSteps)
            },
            workouts: allRecords(WorkoutSessionRecord.self).map { session in
                BackupFile.Workout(
                    epochDay: session.day,
                    templateId: session.templateID,
                    templateName: session.templateName,
                    category: session.category.rawValue,
                    durationSeconds: session.durationSeconds,
                    exercisesCompleted: session.exercisesCompleted,
                    exercisesSkipped: session.exercisesSkipped,
                    exercisesTotal: session.exercisesTotal,
                    estimatedCalories: session.estimatedCalories,
                    note: session.note
                )
            },
            expenses: allRecords(ExpenseRecord.self).map {
                BackupFile.Expense(epochDay: $0.day, label: $0.label, amount: $0.amount)
            },
            dayMeta: allRecords(DayMetaRecord.self).map { meta in
                BackupFile.DayMeta(
                    epochDay: meta.day,
                    budget: meta.budget,
                    waterTargetMl: meta.waterTargetMl,
                    stepGoal: meta.stepGoal,
                    calorieTarget: meta.calorieTarget,
                    planGenerated: meta.planGenerated
                )
            },
            reminders: allRecords(ReminderRecord.self).map { record in
                BackupFile.Reminder(
                    type: record.kind.rawValue,
                    enabled: record.enabled,
                    hour: record.hour,
                    minute: record.minute,
                    daysMask: record.daysMask,
                    intervalMinutes: record.intervalMinutes,
                    soundEnabled: record.soundEnabled,
                    vibrationEnabled: record.vibrationEnabled
                )
            },
            settings: BackupFile.Settings(
                themeMode: settings.themeMode.rawValue,
                unitSystem: settings.unitSystem.rawValue,
                waterTargetMl: settings.waterTargetMl,
                stepGoal: settings.stepGoal,
                remindersEnabled: settings.remindersEnabled,
                notificationSoundEnabled: settings.notificationSoundEnabled,
                excludedFoodKeys: Array(settings.excludedFoodKeys).sorted()
            )
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(file)
        let url = exportDirectory.appendingPathComponent("FitBudget-backup-\(DayCalendar.today().value).json")
        try data.write(to: url, options: .atomic)
        return url
    }

    /// Replaces the local database with the contents of a backup file. The file is fully decoded and
    /// validated before anything is deleted, so a malformed file leaves existing data untouched.
    func restoreBackup(from data: Data) async -> Result<RestoreSummary, Error> {
        do {
            let file = try JSONDecoder().decode(BackupFile.self, from: data)
            guard file.format == Self.backupFormat else {
                return .failure(StoreError.invalidBackup)
            }

            // ---- everything parsed, now swap the data in ----
            deleteAll(MealEntryRecord.self)
            deleteAll(WeightLogRecord.self)
            deleteAll(WaterLogRecord.self)
            deleteAll(StepLogRecord.self)
            deleteAll(WorkoutSessionRecord.self)
            deleteAll(ExpenseRecord.self)
            deleteAll(DayMetaRecord.self)
            commit()

            if let backupProfile = file.profile {
                updateProfile { record in
                    record.name = backupProfile.name
                    record.dobDay = backupProfile.dobDay
                    record.gender = Gender.from(backupProfile.gender)
                    record.heightCm = backupProfile.heightCm
                    record.startWeightKg = backupProfile.startWeightKg
                    record.currentWeightKg = backupProfile.currentWeightKg
                    record.targetWeightKg = backupProfile.targetWeightKg
                    record.dailyBudget = backupProfile.dailyBudget
                    record.activityLevel = ActivityLevel.from(backupProfile.activityLevel)
                    record.wakeMinutes = backupProfile.wakeMinutes
                    record.sleepMinutes = backupProfile.sleepMinutes
                    record.dietPreference = DietPreference.from(backupProfile.dietPreference)
                    record.calorieTargetOverride = backupProfile.calorieTargetOverride
                    record.onboardingComplete = backupProfile.onboardingComplete
                }
            }

            var restoredFoods = 0
            for food in file.customFoods where !food.name.trimmingCharacters(in: .whitespaces).isEmpty {
                let key = FoodItem.key(for: food.name)
                guard self.food(withKey: key) == nil else { continue }
                insert(
                    FoodRecord(
                        item: FoodItem(
                            name: food.name,
                            servingLabel: food.servingLabel,
                            calories: food.calories,
                            proteinG: food.proteinG,
                            carbsG: food.carbsG,
                            fatG: food.fatG,
                            costRupees: food.costRupees,
                            category: FoodCategory.from(food.category),
                            role: FoodRole.from(food.role),
                            isCustom: true
                        )
                    )
                )
                restoredFoods += 1
            }

            for entry in file.mealEntries {
                insert(
                    MealEntryRecord(
                        day: entry.epochDay,
                        mealType: MealType.from(entry.mealType),
                        mealLabel: entry.mealLabel,
                        minutesOfDay: entry.timeMinutes,
                        foodNameKey: FoodItem.key(for: entry.foodName),
                        foodName: entry.foodName,
                        servingLabel: entry.servingLabel,
                        quantity: entry.quantity,
                        caloriesPerServing: entry.caloriesPerServing,
                        proteinPerServing: entry.proteinPerServing,
                        costPerServing: entry.costPerServing,
                        completed: entry.completed,
                        sortOrder: entry.sortOrder
                    )
                )
            }

            for log in file.weightLogs where log.weightKg > 0 {
                insert(WeightLogRecord(day: log.epochDay, weightKg: log.weightKg, note: log.note))
            }
            for log in file.waterLogs where log.amountMl > 0 {
                insert(WaterLogRecord(day: log.epochDay, amountMl: log.amountMl))
            }
            for log in file.stepLogs {
                insert(
                    StepLogRecord(
                        day: log.epochDay,
                        sensorSteps: log.sensorSteps,
                        manualSteps: log.manualSteps
                    )
                )
            }
            for session in file.workouts {
                insert(
                    WorkoutSessionRecord(
                        day: session.epochDay,
                        templateID: session.templateId,
                        templateName: session.templateName,
                        category: WorkoutCategory.from(session.category),
                        startedAt: Date(),
                        finishedAt: Date(),
                        durationSeconds: session.durationSeconds,
                        exercisesCompleted: session.exercisesCompleted,
                        exercisesSkipped: session.exercisesSkipped,
                        exercisesTotal: session.exercisesTotal,
                        estimatedCalories: session.estimatedCalories,
                        note: session.note
                    )
                )
            }
            for expense in file.expenses {
                insert(ExpenseRecord(day: expense.epochDay, label: expense.label, amount: expense.amount))
            }
            for meta in file.dayMeta {
                insert(
                    DayMetaRecord(
                        day: meta.epochDay,
                        budget: meta.budget,
                        waterTargetMl: meta.waterTargetMl,
                        stepGoal: meta.stepGoal,
                        calorieTarget: meta.calorieTarget,
                        planGenerated: meta.planGenerated
                    )
                )
            }
            commit()

            for backupReminder in file.reminders {
                let kind = ReminderKind.from(backupReminder.type)
                await updateReminder(kind) { record in
                    record.enabled = backupReminder.enabled
                    record.hour = backupReminder.hour
                    record.minute = backupReminder.minute
                    record.daysMask = backupReminder.daysMask
                    record.intervalMinutes = backupReminder.intervalMinutes
                    record.soundEnabled = backupReminder.soundEnabled
                    record.vibrationEnabled = backupReminder.vibrationEnabled
                }
            }

            if let backupSettings = file.settings {
                settings.themeMode = ThemeMode.from(backupSettings.themeMode)
                settings.unitSystem = UnitSystem.from(backupSettings.unitSystem)
                settings.waterTargetMl = min(
                    max(backupSettings.waterTargetMl, Validators.waterTargetRange.lowerBound),
                    Validators.waterTargetRange.upperBound
                )
                settings.stepGoal = min(
                    max(backupSettings.stepGoal, Validators.stepRange.lowerBound),
                    Validators.stepRange.upperBound
                )
                settings.remindersEnabled = backupSettings.remindersEnabled
                settings.notificationSoundEnabled = backupSettings.notificationSoundEnabled
                settings.excludedFoodKeys = Set(backupSettings.excludedFoodKeys)
                persistSettings()
            }

            await rescheduleReminders()
            refresh()

            return .success(
                RestoreSummary(
                    weightLogs: file.weightLogs.count,
                    mealEntries: file.mealEntries.count,
                    waterLogs: file.waterLogs.count,
                    workouts: file.workouts.count,
                    stepDays: file.stepLogs.count,
                    expenses: file.expenses.count,
                    customFoods: restoredFoods
                )
            )
        } catch {
            Self.logger.error("Restore failed: \(error.localizedDescription, privacy: .public)")
            return .failure(error)
        }
    }
}
