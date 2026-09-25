import SwiftUI
import UIKit
import UniformTypeIdentifiers
import FitBudgetCore

struct SettingsView: View {

    @Environment(AppStore.self) private var store
    @Environment(Router.self) private var router

    @State private var editing: EditTarget?
    @State private var editText = ""
    @State private var showResetConfirm = false
    @State private var showImporter = false
    @State private var exportURL: URL?
    @State private var busy = false

    enum EditTarget: String, Identifiable {
        case budget, targetWeight, water, steps, calories

        var id: String { rawValue }

        var title: String {
            switch self {
            case .budget: return "Daily food budget"
            case .targetWeight: return "Target weight"
            case .water: return "Water target"
            case .steps: return "Step goal"
            case .calories: return "Calorie target"
            }
        }

        var hint: String {
            switch self {
            case .budget: return "Between ₹1 and ₹10,000"
            case .targetWeight: return "Must be at or below your current weight"
            case .water: return "Between 500 ml and 10,000 ml"
            case .steps: return "Between 0 and 100,000"
            case .calories: return "Leave blank to use the estimate"
            }
        }
    }

    var body: some View {
        Form {
            if busy {
                Section { ProgressView() }
            }

            Section("Goals") {
                row("Daily food budget", value: Formatters.rupees(store.profile?.dailyBudget ?? 0)) {
                    startEditing(.budget, initial: "\(Int(store.profile?.dailyBudget ?? 100))")
                }
                row("Target weight", value: Formatters.kg(store.profile?.targetWeightKg ?? 0)) {
                    startEditing(
                        .targetWeight,
                        initial: String(format: "%.1f", store.profile?.targetWeightKg ?? 75)
                    )
                }
                row("Calorie target", value: calorieLabel) {
                    startEditing(
                        .calories,
                        initial: store.profile?.calorieTargetOverride.map { "\(Int($0))" } ?? ""
                    )
                }
                row("Water target", value: Formatters.litres(store.settings.waterTargetMl)) {
                    startEditing(.water, initial: "\(store.settings.waterTargetMl)")
                }
                row("Step goal", value: Formatters.steps(store.settings.stepGoal)) {
                    startEditing(.steps, initial: "\(store.settings.stepGoal)")
                }
            }

            Section("Food preferences") {
                Picker("Preference", selection: preferenceBinding) {
                    ForEach(DietPreference.allCases, id: \.self) { Text($0.label).tag($0) }
                }
                Button {
                    router.push(.foods, on: router.selectedTab)
                } label: {
                    HStack {
                        Text(
                            store.settings.excludedFoodKeys.isEmpty
                                ? "Food database & exclusions"
                                : "Food database · \(store.settings.excludedFoodKeys.count) excluded"
                        )
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)
            }

            Section("Reminders & notifications") {
                Toggle("All reminders", isOn: remindersBinding)
                Toggle("Notification sound", isOn: soundBinding)
                Button {
                    router.push(.reminders, on: router.selectedTab)
                } label: {
                    HStack {
                        Text("Reminder times")
                        Spacer()
                        Text("\(ReminderKind.allCases.count) reminders")
                            .foregroundStyle(.secondary)
                        Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)

                if store.notifications.authorizationStatus == .denied {
                    Button("Open iOS notification settings") { openSystemSettings() }
                        .font(.footnote)
                }
                Text("iOS controls vibration for notifications through Settings → Sounds & Haptics.")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Section("Appearance") {
                Picker("Theme", selection: themeBinding) {
                    ForEach(ThemeMode.allCases, id: \.self) { Text($0.label).tag($0) }
                }
                Picker("Units", selection: unitsBinding) {
                    ForEach(UnitSystem.allCases, id: \.self) { Text($0.label).tag($0) }
                }
            }

            Section {
                Button("Export data (CSV)") { export { try store.writeCSVExport() } }
                Button("Share this month's report") {
                    export { try store.writeMonthlyReport(for: store.todayKey) }
                }
                Button("Create backup file (JSON)") { export { try store.writeBackup() } }
                Button("Restore from backup") { showImporter = true }
            } header: {
                Text("Your data")
            } footer: {
                Text(
                    "Everything stays on this device. Restoring replaces the logs on this device with "
                    + "the contents of the backup file."
                )
            }

            Section {
                Button("Reset all data", role: .destructive) { showResetConfirm = true }
            }

            Section("About") {
                Button {
                    router.push(.privacy, on: router.selectedTab)
                } label: {
                    HStack {
                        Text("Privacy")
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)
                Button {
                    router.push(.about, on: router.selectedTab)
                } label: {
                    HStack {
                        Text("About FitBudget")
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .alert(editing?.title ?? "", isPresented: editingBinding) {
            TextField(editing?.title ?? "", text: $editText)
                .keyboardType(.decimalPad)
            Button("Save") { commitEdit() }
            Button("Cancel", role: .cancel) { editing = nil }
        } message: {
            Text(editing?.hint ?? "")
        }
        .confirmationDialog(
            "Erase all FitBudget data?",
            isPresented: $showResetConfirm,
            titleVisibility: .visible
        ) {
            Button("Erase everything", role: .destructive) {
                Task { await store.resetAllData() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "Your profile, weight log, meals, water, workouts, steps, expenses and reminder "
                + "settings will be permanently deleted from this device. Export a backup first if "
                + "you want to keep them."
            )
        }
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.json, .plainText, .item],
            allowsMultipleSelection: false
        ) { result in
            handleImport(result)
        }
        .sheet(item: Binding(
            get: { exportURL.map { ExportedFile(url: $0) } },
            set: { if $0 == nil { exportURL = nil } }
        )) { file in
            ShareSheet(url: file.url)
        }
    }

    // MARK: - Bindings

    private var editingBinding: Binding<Bool> {
        Binding(get: { editing != nil }, set: { if !$0 { editing = nil } })
    }

    private var preferenceBinding: Binding<DietPreference> {
        Binding(
            get: { store.profile?.dietPreference ?? .vegetarian },
            set: { store.setDietPreference($0) }
        )
    }

    private var remindersBinding: Binding<Bool> {
        Binding(
            get: { store.settings.remindersEnabled },
            set: { value in Task { await store.setRemindersEnabled(value) } }
        )
    }

    private var soundBinding: Binding<Bool> {
        Binding(
            get: { store.settings.notificationSoundEnabled },
            set: { value in Task { await store.setNotificationSound(value) } }
        )
    }

    private var themeBinding: Binding<ThemeMode> {
        Binding(get: { store.settings.themeMode }, set: { store.setThemeMode($0) })
    }

    private var unitsBinding: Binding<UnitSystem> {
        Binding(get: { store.settings.unitSystem }, set: { store.setUnitSystem($0) })
    }

    private var calorieLabel: String {
        if let override = store.profile?.calorieTargetOverride {
            return "\(Int(override)) kcal (manual)"
        }
        return "\(Int((store.profile?.estimatedCalorieTarget ?? 0).rounded())) kcal (estimated)"
    }

    // MARK: - Helpers

    private func row(_ title: String, value: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(title)
                Spacer()
                Text(value).foregroundStyle(.secondary)
            }
        }
        .foregroundStyle(.primary)
    }

    private func startEditing(_ target: EditTarget, initial: String) {
        editText = initial
        editing = target
    }

    private func commitEdit() {
        guard let editing else { return }
        switch editing {
        case .budget:
            let parsed = Validators.parseDecimal(editText)
            let result = Validators.budget(parsed)
            if result.isValid, let parsed {
                store.setDailyBudget(parsed)
                store.show("Daily budget updated.")
            } else {
                store.show(result.message ?? "Invalid budget.")
            }
        case .targetWeight:
            let parsed = Validators.parseDecimal(editText)
            let result = Validators.targetWeight(parsed, currentKg: store.profile?.currentWeightKg)
            if result.isValid, let parsed {
                store.setTargetWeight(parsed)
                store.show("Target weight updated.")
            } else {
                store.show(result.message ?? "Invalid target weight.")
            }
        case .water:
            let parsed = Validators.parseInt(editText)
            let result = Validators.waterTarget(parsed)
            if result.isValid, let parsed {
                store.setWaterTarget(parsed)
                store.show("Water target updated.")
            } else {
                store.show(result.message ?? "Invalid target.")
            }
        case .steps:
            let parsed = Validators.parseInt(editText)
            let result = Validators.steps(parsed)
            if result.isValid, let parsed {
                store.setStepGoal(parsed)
                store.show("Step goal updated.")
            } else {
                store.show(result.message ?? "Invalid goal.")
            }
        case .calories:
            if editText.isEmpty {
                store.setCalorieOverride(nil)
                store.show("Using the estimated calorie target again.")
            } else if let parsed = Validators.parseDecimal(editText), (1_000...5_000).contains(parsed) {
                store.setCalorieOverride(parsed)
                store.show("Calorie target set to \(Int(parsed)) kcal.")
            } else {
                store.show("Enter a calorie target between 1,000 and 5,000, or leave it blank.")
            }
        }
        self.editing = nil
    }

    private func export(_ build: @escaping () throws -> URL) {
        busy = true
        do {
            exportURL = try build()
        } catch {
            store.show("Export failed.")
        }
        busy = false
    }

    private func handleImport(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            busy = true
            Task {
                // Security-scoped access is required for files picked outside the sandbox.
                let accessed = url.startAccessingSecurityScopedResource()
                defer { if accessed { url.stopAccessingSecurityScopedResource() } }

                guard let data = try? Data(contentsOf: url) else {
                    store.show("That file could not be read.")
                    busy = false
                    return
                }
                let outcome = await store.restoreBackup(from: data)
                switch outcome {
                case .success(let summary):
                    store.show(
                        "Restored \(summary.weightLogs) weigh-ins, \(summary.mealEntries) meal items, "
                        + "\(summary.workouts) workouts."
                    )
                case .failure:
                    store.show("That file could not be restored. Your existing data is untouched.")
                }
                busy = false
            }
        case .failure:
            store.show("No file was selected.")
        }
    }

    private func openSystemSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}

struct ExportedFile: Identifiable {
    let url: URL
    var id: String { url.absoluteString }
}

/// `UIActivityViewController` wrapper, used for the generated export files.
struct ShareSheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
