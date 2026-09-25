import SwiftUI
import UIKit
import FitBudgetCore

struct RemindersView: View {

    @Environment(AppStore.self) private var store

    @State private var expanded: Set<String> = []
    @State private var timeEditing: ReminderKind?
    @State private var editTime = Date()

    private let intervalOptions = [60, 90, 120, 180, 240]

    var body: some View {
        List {
            Section {
                Toggle("All reminders", isOn: masterBinding)
                Text("Reminders are scheduled on this device only. iOS keeps delivering them after a restart or reboot.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if store.notifications.authorizationStatus == .denied {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Notifications are blocked").font(.subheadline.weight(.semibold))
                        Text("FitBudget cannot deliver reminders until notifications are allowed in iOS Settings.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Button("Open iOS Settings") { openSystemSettings() }
                            .font(.footnote)
                    }
                }
            } else if store.notifications.authorizationStatus == .notDetermined {
                Section {
                    Button("Allow notifications") {
                        Task {
                            _ = await store.notifications.requestAuthorization()
                            await store.rescheduleReminders()
                        }
                    }
                }
            }

            Section {
                ForEach(store.reminders) { reminder in
                    reminderRow(reminder)
                }
            } header: {
                Text("Schedule")
            } footer: {
                Text(
                    "Water reminders repeat between "
                    + Formatters.time(minutesOfDay: store.profile?.wakeMinutes ?? 390)
                    + " and "
                    + Formatters.time(minutesOfDay: store.profile?.sleepMinutes ?? 1_350)
                    + " (your wake and sleep times). "
                    + "\(store.notifications.pendingCount) notifications are currently scheduled."
                )
            }
        }
        .navigationTitle("Reminders")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Re-sync") {
                    Task {
                        await store.rescheduleReminders()
                        store.show("All reminders re-scheduled.")
                    }
                }
            }
        }
        .sheet(item: $timeEditing) { kind in
            ReminderTimeSheet(kind: kind, initial: editTime) { hour, minute in
                Task {
                    await store.updateReminder(kind) { record in
                        record.hour = hour
                        record.minute = minute
                    }
                    store.show("\(kind.label) reminder set to \(Formatters.time(hour: hour, minute: minute)).")
                }
                timeEditing = nil
            }
        }
        .task { await store.notifications.refreshStatus() }
    }

    private var masterBinding: Binding<Bool> {
        Binding(
            get: { store.settings.remindersEnabled },
            set: { value in Task { await store.setRemindersEnabled(value) } }
        )
    }

    private func reminderRow(_ reminder: ReminderRecord) -> some View {
        let kind = reminder.kind
        let isExpanded = expanded.contains(kind.rawValue)

        return VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(kind.label).font(.subheadline.weight(.semibold))
                    Text(scheduleLabel(reminder))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(nextTriggerLabel(reminder))
                        .font(.caption2)
                        .foregroundStyle(
                            reminder.enabled && store.settings.remindersEnabled
                                ? FitTheme.brand
                                : Color.secondary
                        )
                }
                Spacer(minLength: 8)
                Toggle("", isOn: Binding(
                    get: { reminder.enabled && store.settings.remindersEnabled },
                    set: { value in
                        Task { await store.updateReminder(kind) { $0.enabled = value } }
                    }
                ))
                .labelsHidden()
                .disabled(!store.settings.remindersEnabled)
            }

            HStack(spacing: 12) {
                Button("Change time") {
                    editTime = timeDate(hour: reminder.hour, minute: reminder.minute)
                    timeEditing = kind
                }
                .font(.caption)

                Button(isExpanded ? "Hide options" : "More options") {
                    if isExpanded {
                        expanded.remove(kind.rawValue)
                    } else {
                        expanded.insert(kind.rawValue)
                    }
                }
                .font(.caption)
            }

            if isExpanded {
                VStack(alignment: .leading, spacing: 10) {
                    if kind.isInterval {
                        Text("Repeat every").font(.caption.weight(.semibold))
                        HStack(spacing: 6) {
                            ForEach(intervalOptions, id: \.self) { minutes in
                                Button {
                                    Task {
                                        await store.updateReminder(kind) { $0.intervalMinutes = minutes }
                                    }
                                } label: {
                                    Text(minutes % 60 == 0 ? "\(minutes / 60)h" : "\(minutes)m")
                                        .font(.caption2.weight(.semibold))
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                        .background(
                                            reminder.intervalMinutes == minutes
                                                ? FitTheme.brand.opacity(0.18)
                                                : FitTheme.subtleCard
                                        )
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    } else {
                        Text("Repeat on").font(.caption.weight(.semibold))
                        WeekdayPicker(mask: Binding(
                            get: { reminder.daysMask },
                            set: { value in
                                guard value & WeekdayMask.all != 0 else {
                                    store.show("Pick at least one day, or switch the reminder off.")
                                    return
                                }
                                Task { await store.updateReminder(kind) { $0.daysMask = value } }
                            }
                        ))
                    }

                    Toggle("Sound", isOn: Binding(
                        get: { reminder.soundEnabled },
                        set: { value in
                            Task { await store.updateReminder(kind) { $0.soundEnabled = value } }
                        }
                    ))
                    .font(.subheadline)

                    Text("Notification: \"\(kind.notificationTitle) — \(kind.notificationBody)\"")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text("Tapping it opens the \(kind.route) screen.")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 2)
            }
        }
        .padding(.vertical, 4)
    }

    private func scheduleLabel(_ reminder: ReminderRecord) -> String {
        let kind = reminder.kind
        if kind.isInterval {
            let hours = reminder.intervalMinutes / 60
            let minutes = reminder.intervalMinutes % 60
            let every = minutes == 0 ? "\(hours)h" : "\(hours)h \(minutes)m"
            return "From \(Formatters.time(hour: reminder.hour, minute: reminder.minute)) · every \(every)"
        }
        return Formatters.time(hour: reminder.hour, minute: reminder.minute)
            + " · " + WeekdayMask.describe(reminder.daysMask)
    }

    private func nextTriggerLabel(_ reminder: ReminderRecord) -> String {
        guard store.settings.remindersEnabled, reminder.enabled else { return "Not scheduled" }
        let plan = ReminderPlan(
            kind: reminder.kind,
            enabled: reminder.enabled,
            hour: reminder.hour,
            minute: reminder.minute,
            daysMask: reminder.daysMask,
            intervalMinutes: reminder.intervalMinutes,
            soundEnabled: reminder.soundEnabled
        )
        guard let next = store.notifications.nextTriggerDate(
            for: plan,
            wakeMinutes: store.profile?.wakeMinutes ?? 390,
            sleepMinutes: store.profile?.sleepMinutes ?? 1_350
        ) else { return "Not scheduled" }

        let calendar = DayCalendar.calendar()
        let day = DayCalendar.dayKey(for: next, calendar: calendar)
        let components = calendar.dateComponents([.hour, .minute], from: next)
        let dayLabel: String
        switch day.value - store.todayKey.value {
        case 0: dayLabel = "today"
        case 1: dayLabel = "tomorrow"
        default: dayLabel = Formatters.shortDate(day)
        }
        return "Next: \(dayLabel) at \(Formatters.time(hour: components.hour ?? 0, minute: components.minute ?? 0))"
    }

    private func timeDate(hour: Int, minute: Int) -> Date {
        var components = DateComponents()
        components.hour = hour
        components.minute = minute
        return DayCalendar.calendar().date(from: components) ?? Date()
    }

    private func openSystemSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}

struct ReminderTimeSheet: View {

    let kind: ReminderKind
    let initial: Date
    let onSave: (Int, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var time: Date

    init(kind: ReminderKind, initial: Date, onSave: @escaping (Int, Int) -> Void) {
        self.kind = kind
        self.initial = initial
        self.onSave = onSave
        _time = State(initialValue: initial)
    }

    var body: some View {
        NavigationStack {
            VStack {
                DatePicker("", selection: $time, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                Spacer()
            }
            .padding()
            .navigationTitle("\(kind.label) reminder")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Set") {
                        let components = DayCalendar.calendar()
                            .dateComponents([.hour, .minute], from: time)
                        onSave(components.hour ?? 8, components.minute ?? 0)
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
