import SwiftUI
import Observation
import FitBudgetCore

/// Every push-able destination in the app.
enum Route: Hashable {
    case water
    case weight
    case steps
    case budget
    case monthlyReport
    case settings
    case reminders
    case foods
    case privacy
    case about
    case workoutSession(templateID: String)
}

enum AppTab: Hashable {
    case home, diet, workout, progress, profile
}

/// Owns tab selection and one navigation path per tab, so a notification can deep-link into the
/// right place without disturbing the other tabs.
@MainActor
@Observable
final class Router {

    var selectedTab: AppTab = .home

    var homePath: [Route] = []
    var dietPath: [Route] = []
    var workoutPath: [Route] = []
    var progressPath: [Route] = []
    var profilePath: [Route] = []

    /// Called for notification taps. Routes come from `ReminderKind.route`.
    func handleDeepLink(_ route: String) {
        switch route {
        case "diet":
            selectedTab = .diet
            dietPath = []
        case "workout":
            selectedTab = .workout
            workoutPath = []
        case "water":
            selectedTab = .home
            homePath = [.water]
        case "weight":
            selectedTab = .progress
            progressPath = [.weight]
        case "steps":
            selectedTab = .home
            homePath = [.steps]
        case "progress":
            selectedTab = .progress
            progressPath = []
        case "profile":
            selectedTab = .profile
            profilePath = []
        default:
            selectedTab = .home
            homePath = []
        }
    }

    func push(_ route: Route, on tab: AppTab? = nil) {
        let target = tab ?? selectedTab
        switch target {
        case .home: homePath.append(route)
        case .diet: dietPath.append(route)
        case .workout: workoutPath.append(route)
        case .progress: progressPath.append(route)
        case .profile: profilePath.append(route)
        }
    }

    func resetPaths() {
        homePath = []
        dietPath = []
        workoutPath = []
        progressPath = []
        profilePath = []
    }
}

/// Shared destination builder so every tab resolves routes identically.
struct RouteDestination: View {
    let route: Route

    var body: some View {
        switch route {
        case .water: WaterView()
        case .weight: WeightView()
        case .steps: StepsView()
        case .budget: BudgetView()
        case .monthlyReport: MonthlyReportView()
        case .settings: SettingsView()
        case .reminders: RemindersView()
        case .foods: FoodsView()
        case .privacy: PrivacyView()
        case .about: AboutView()
        case .workoutSession(let templateID): WorkoutSessionView(templateID: templateID)
        }
    }
}
