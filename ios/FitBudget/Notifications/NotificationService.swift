import Foundation
import UserNotifications
import FitBudgetCore
import OSLog

/// A reminder as the scheduler sees it (decoupled from SwiftData).
struct ReminderPlan {
    let kind: ReminderKind
    let enabled: Bool
    let hour: Int
    let minute: Int
    let daysMask: Int
    let intervalMinutes: Int
    let soundEnabled: Bool
}

/// What the user tapped on a delivered notification.
enum NotificationAction {
    case markMealEaten(MealType)
    case addWater(Int)
    case open(route: String)
}

enum NotificationIdentifiers {
    static let mealCategory = "FITBUDGET_MEAL"
    static let waterCategory = "FITBUDGET_WATER"
    static let markEaten = "FITBUDGET_MARK_EATEN"
    static let addWater250 = "FITBUDGET_ADD_WATER_250"
    static let addWater500 = "FITBUDGET_ADD_WATER_500"
    static let routeKey = "route"
    static let kindKey = "kind"
}

/// Schedules the local reminders with `UNUserNotificationCenter`.
///
/// Every reminder becomes one or more repeating calendar notifications, which iOS keeps delivering
/// after a restart or reboot without any background work from the app. The whole schedule is
/// rebuilt from the database whenever a reminder changes, so it can never drift.
@MainActor
final class NotificationService {

    private static let logger = Logger(subsystem: "com.fitbudget.app", category: "Notifications")

    /// iOS allows 64 pending requests per app; stay well below it.
    private static let maxRequests = 60

    private let center: UNUserNotificationCenter

    var authorizationStatus: UNAuthorizationStatus = .notDetermined
    var pendingCount: Int = 0

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    var isAuthorized: Bool {
        authorizationStatus == .authorized || authorizationStatus == .provisional
            || authorizationStatus == .ephemeral
    }

    // MARK: - Permission

    func registerCategories() {
        let markEaten = UNNotificationAction(
            identifier: NotificationIdentifiers.markEaten,
            title: "Mark eaten",
            options: []
        )
        let add250 = UNNotificationAction(
            identifier: NotificationIdentifiers.addWater250,
            title: "+250 ml",
            options: []
        )
        let add500 = UNNotificationAction(
            identifier: NotificationIdentifiers.addWater500,
            title: "+500 ml",
            options: []
        )
        let mealCategory = UNNotificationCategory(
            identifier: NotificationIdentifiers.mealCategory,
            actions: [markEaten],
            intentIdentifiers: [],
            options: []
        )
        let waterCategory = UNNotificationCategory(
            identifier: NotificationIdentifiers.waterCategory,
            actions: [add250, add500],
            intentIdentifiers: [],
            options: []
        )
        center.setNotificationCategories([mealCategory, waterCategory])
    }

    func refreshStatus() async {
        let settings = await center.notificationSettings()
        authorizationStatus = settings.authorizationStatus
        pendingCount = await center.pendingNotificationRequests().count
    }

    @discardableResult
    func requestAuthorization() async -> Bool {
        do {
            let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge])
            await refreshStatus()
            return granted
        } catch {
            Self.logger.error("Authorization failed: \(error.localizedDescription, privacy: .public)")
            await refreshStatus()
            return false
        }
    }

    // MARK: - Scheduling

    func cancelAll() async {
        center.removeAllPendingNotificationRequests()
        await refreshStatus()
    }

    /// Rebuilds the entire pending schedule.
    func reschedule(
        reminders: [ReminderPlan],
        wakeMinutes: Int,
        sleepMinutes: Int,
        masterEnabled: Bool
    ) async {
        center.removeAllPendingNotificationRequests()

        guard masterEnabled else {
            await refreshStatus()
            return
        }
        await refreshStatus()
        guard isAuthorized else {
            // Nothing to schedule until the user allows notifications; the UI surfaces a banner.
            return
        }

        var scheduled = 0
        for plan in reminders.sorted(by: { $0.kind.sortIndex < $1.kind.sortIndex }) where plan.enabled {
            let slots = slots(for: plan, wakeMinutes: wakeMinutes, sleepMinutes: sleepMinutes)
            let weekdays = weekdayComponents(for: plan.daysMask)
            guard !slots.isEmpty, !weekdays.isEmpty else { continue }

            for (slotIndex, minutes) in slots.enumerated() {
                for weekday in weekdays {
                    guard scheduled < Self.maxRequests else {
                        Self.logger.warning("Reached the pending notification limit; stopping.")
                        await refreshStatus()
                        return
                    }
                    let identifier = [
                        "reminder", plan.kind.rawValue, "\(slotIndex)",
                        weekday.map(String.init) ?? "daily"
                    ].joined(separator: ".")

                    var components = DateComponents()
                    components.hour = minutes / 60
                    components.minute = minutes % 60
                    if let weekday { components.weekday = weekday }

                    let content = UNMutableNotificationContent()
                    content.title = plan.kind.notificationTitle
                    content.body = plan.kind.notificationBody
                    content.sound = plan.soundEnabled ? .default : nil
                    content.interruptionLevel = .active
                    content.userInfo = [
                        NotificationIdentifiers.routeKey: plan.kind.route,
                        NotificationIdentifiers.kindKey: plan.kind.rawValue
                    ]
                    if plan.kind.mealType != nil {
                        content.categoryIdentifier = NotificationIdentifiers.mealCategory
                    } else if plan.kind == .water {
                        content.categoryIdentifier = NotificationIdentifiers.waterCategory
                    }

                    let request = UNNotificationRequest(
                        identifier: identifier,
                        content: content,
                        trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
                    )
                    do {
                        try await center.add(request)
                        scheduled += 1
                    } catch {
                        Self.logger.error(
                            "Could not schedule \(identifier, privacy: .public): \(error.localizedDescription, privacy: .public)"
                        )
                    }
                }
            }
        }
        await refreshStatus()
    }

    /// All times of day a reminder can fire, ascending.
    private func slots(for plan: ReminderPlan, wakeMinutes: Int, sleepMinutes: Int) -> [Int] {
        let base = min(max(plan.hour, 0), 23) * 60 + min(max(plan.minute, 0), 59)
        guard plan.kind.isInterval, plan.intervalMinutes > 0 else { return [base] }

        let interval = min(max(plan.intervalMinutes, 15), 720)
        let windowStart = min(max(wakeMinutes, 0), 23 * 60)
        // A sleep time at or before the wake time means "after midnight"; clamp to end of day.
        let windowEnd = sleepMinutes <= windowStart ? 23 * 60 + 59 : min(sleepMinutes, 1_439)

        var result: [Int] = []
        var minute = max(base, windowStart)
        while minute <= windowEnd && result.count < 12 {
            result.append(minute)
            minute += interval
        }
        return result.isEmpty ? [windowStart] : result
    }

    /// `nil` means "every day" (one repeating daily trigger, which keeps the request count low).
    /// Otherwise one `UNCalendarNotificationTrigger` weekday per selected day.
    private func weekdayComponents(for daysMask: Int) -> [Int?] {
        let normalised = daysMask & WeekdayMask.all
        if normalised == WeekdayMask.all { return [nil] }
        if normalised == 0 { return [] }
        // ISO 1 = Monday … 7 = Sunday  ->  UN 1 = Sunday … 7 = Saturday
        return WeekdayMask.isoWeekdays(in: normalised).map { iso in
            iso == 7 ? 1 : iso + 1
        }
    }

    /// Next fire date for a reminder, used by the Reminders screen.
    func nextTriggerDate(
        for plan: ReminderPlan,
        wakeMinutes: Int,
        sleepMinutes: Int,
        from reference: Date = Date()
    ) -> Date? {
        guard plan.enabled else { return nil }
        let calendar = DayCalendar.calendar()
        let slots = slots(for: plan, wakeMinutes: wakeMinutes, sleepMinutes: sleepMinutes)
        let mask = plan.daysMask & WeekdayMask.all
        guard !slots.isEmpty, mask != 0 else { return nil }

        for offset in 0...8 {
            guard let date = calendar.date(byAdding: .day, value: offset, to: reference) else { continue }
            let day = DayCalendar.dayKey(for: date, calendar: calendar)
            guard WeekdayMask.contains(mask, isoWeekday: DayCalendar.isoWeekday(of: day, calendar: calendar))
            else { continue }

            let startOfDay = calendar.startOfDay(for: date)
            for minutes in slots {
                guard let candidate = calendar.date(byAdding: .minute, value: minutes, to: startOfDay)
                else { continue }
                if candidate > reference { return candidate }
            }
        }
        return nil
    }
}

/// Bridges `UNUserNotificationCenter` callbacks into the SwiftUI layer.
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {

    /// Set once the store exists; receives taps and inline actions.
    var onAction: ((NotificationAction) -> Void)?

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        // Reminders are useful even while the app is open.
        [.banner, .sound, .list]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        let route = userInfo[NotificationIdentifiers.routeKey] as? String ?? "home"
        let kind = ReminderKind.from(userInfo[NotificationIdentifiers.kindKey] as? String)
        let handler = onAction

        switch response.actionIdentifier {
        case NotificationIdentifiers.markEaten:
            if let mealType = kind.mealType {
                await MainActor.run { handler?(.markMealEaten(mealType)) }
            }
        case NotificationIdentifiers.addWater250:
            await MainActor.run { handler?(.addWater(250)) }
        case NotificationIdentifiers.addWater500:
            await MainActor.run { handler?(.addWater(500)) }
        default:
            await MainActor.run { handler?(.open(route: route)) }
        }
    }
}
