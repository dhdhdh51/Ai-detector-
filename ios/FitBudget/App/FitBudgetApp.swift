import SwiftUI
import SwiftData
import UserNotifications
import FitBudgetCore
import OSLog

@main
struct FitBudgetApp: App {

    private static let logger = Logger(subsystem: "com.fitbudget.app", category: "App")

    @State private var store: AppStore
    @State private var router = Router()
    private let notificationDelegate = NotificationDelegate()

    init() {
        let container = Self.makeContainer()
        let store = AppStore(container: container)
        _store = State(initialValue: store)

        UNUserNotificationCenter.current().delegate = notificationDelegate
        store.notifications.registerCategories()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
                .environment(router)
                .modelContainer(store.container)
                .preferredColorScheme(store.settings.themeMode.preferredColorScheme)
                .task {
                    // Wire notification taps and inline actions now that the store exists.
                    notificationDelegate.onAction = { [store, router] action in
                        Task { @MainActor in
                            switch action {
                            case .markMealEaten(let mealType):
                                store.setMealCompleted(
                                    day: store.todayKey,
                                    mealType: mealType,
                                    completed: true
                                )
                            case .addWater(let amount):
                                store.addWater(amount)
                            case .open(let route):
                                router.handleDeepLink(route)
                            }
                        }
                    }
                    await store.notifications.refreshStatus()
                    await store.bootstrap()
                }
        }
    }

    /// Builds the SwiftData container, falling back to a fresh store only if the existing file is
    /// unreadable - never silently, and never in a way that loses data on a normal app update.
    private static func makeContainer() -> ModelContainer {
        let schema = Schema(FitBudgetSchema.models)
        let configuration = ModelConfiguration("FitBudget", schema: schema)
        do {
            return try ModelContainer(for: schema, configurations: configuration)
        } catch {
            logger.critical(
                "Persistent store could not be opened: \(error.localizedDescription, privacy: .public)"
            )
            do {
                // Last resort so the app still launches; the user is told in Settings → About.
                return try ModelContainer(
                    for: schema,
                    configurations: ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
                )
            } catch {
                fatalError("Unable to create even an in-memory store: \(error)")
            }
        }
    }
}
