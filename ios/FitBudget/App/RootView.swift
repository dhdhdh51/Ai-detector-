import SwiftUI
import FitBudgetCore

/// Decides between onboarding and the main tab bar, and keeps the data fresh across the day
/// boundary and app activations.
struct RootView: View {

    @Environment(AppStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if !store.isReady {
                LaunchPlaceholder()
            } else if store.profile?.onboardingComplete == true {
                MainTabView()
            } else {
                OnboardingView()
            }
        }
        .animation(.easeInOut(duration: 0.25), value: store.isReady)
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active, store.isReady else { return }
            Task {
                await store.notifications.refreshStatus()
                await store.onForeground()
            }
        }
        .overlay(alignment: .bottom) { MessageToast() }
    }
}

/// Shown for the split second between the launch screen and the first data load.
private struct LaunchPlaceholder: View {
    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "figure.run.circle.fill")
                .font(.system(size: 56))
                .foregroundStyle(FitTheme.brand)
            Text("FitBudget").font(.title2.bold())
            Text("₹100 a Day. Better Every Day.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            ProgressView().padding(.top, 8)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(FitTheme.background)
    }
}

/// Lightweight snackbar equivalent for the store's transient messages.
private struct MessageToast: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        if let message = store.message {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.black.opacity(0.85))
                .clipShape(Capsule())
                .padding(.bottom, 8)
                .shadow(radius: 8, y: 2)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .task(id: message) {
                    try? await Task.sleep(for: .seconds(2.5))
                    store.message = nil
                }
        }
    }
}

struct MainTabView: View {

    @Environment(Router.self) private var router

    var body: some View {
        TabView(selection: Binding(
            get: { router.selectedTab },
            set: { router.selectedTab = $0 }
        )) {
            NavigationStack(path: Binding(
                get: { router.homePath },
                set: { router.homePath = $0 }
            )) {
                HomeView()
                    .navigationDestination(for: Route.self) { RouteDestination(route: $0) }
            }
            .tabItem { Label("Home", systemImage: "house.fill") }
            .tag(AppTab.home)

            NavigationStack(path: Binding(
                get: { router.dietPath },
                set: { router.dietPath = $0 }
            )) {
                DietView()
                    .navigationDestination(for: Route.self) { RouteDestination(route: $0) }
            }
            .tabItem { Label("Diet", systemImage: "fork.knife") }
            .tag(AppTab.diet)

            NavigationStack(path: Binding(
                get: { router.workoutPath },
                set: { router.workoutPath = $0 }
            )) {
                WorkoutListView()
                    .navigationDestination(for: Route.self) { RouteDestination(route: $0) }
            }
            .tabItem { Label("Workout", systemImage: "figure.strengthtraining.traditional") }
            .tag(AppTab.workout)

            NavigationStack(path: Binding(
                get: { router.progressPath },
                set: { router.progressPath = $0 }
            )) {
                ProgressTabView()
                    .navigationDestination(for: Route.self) { RouteDestination(route: $0) }
            }
            .tabItem { Label("Progress", systemImage: "chart.line.uptrend.xyaxis") }
            .tag(AppTab.progress)

            NavigationStack(path: Binding(
                get: { router.profilePath },
                set: { router.profilePath = $0 }
            )) {
                ProfileView()
                    .navigationDestination(for: Route.self) { RouteDestination(route: $0) }
            }
            .tabItem { Label("Profile", systemImage: "person.fill") }
            .tag(AppTab.profile)
        }
        .tint(FitTheme.brand)
    }
}
