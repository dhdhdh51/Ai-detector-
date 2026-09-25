import SwiftUI
import FitBudgetCore

struct PrivacyView: View {

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Your data never leaves this device.").font(.headline)
                    Text(
                        "FitBudget has no account, no server and no analytics. Everything you log is "
                        + "stored in a local database on this iPhone."
                    )
                    .font(.subheadline)
                }
                .fitCard(background: FitTheme.brand.opacity(0.10))

                bulletCard(
                    "What is stored locally",
                    [
                        "Your profile: name, date of birth, gender, height, weights, budget, activity level, food preference and wake/sleep times.",
                        "Your logs: weigh-ins, meals and their cost, extra expenses, water, steps, workout sessions.",
                        "Your settings: theme, units, goals, reminder times and food exclusions."
                    ]
                )

                bulletCard(
                    "What FitBudget never does",
                    [
                        "No account creation, sign-in or email address.",
                        "No upload of health, fitness or spending information to any server.",
                        "No analytics, tracking SDKs, advertising identifiers or crash reporting services.",
                        "No use of the internet for any feature — the app works fully offline."
                    ]
                )

                bulletCard(
                    "Permissions and why they are needed",
                    [
                        "Notifications: to show your meal, water, workout, weight and sleep reminders.",
                        "Motion & Fitness: only to read the step count your iPhone already keeps. Optional — manual entry works instead.",
                        "Nothing else is requested. There is no location, contacts, photos or health-record access."
                    ]
                )

                bulletCard(
                    "Sharing and deletion",
                    [
                        "Export and backup files are written to this app's private storage and only leave the device if you pick a target in the iOS share sheet yourself.",
                        "\"Reset all data\" in Settings permanently erases everything from this device.",
                        "Deleting FitBudget removes the local database and all settings."
                    ]
                )

                DisclaimerCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Privacy")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func bulletCard(_ title: String, _ lines: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.subheadline.weight(.semibold))
            ForEach(lines, id: \.self) { line in
                HStack(alignment: .top, spacing: 6) {
                    Text("•")
                    Text(line).font(.subheadline)
                }
            }
        }
        .fitCard()
    }
}

struct AboutView: View {

    @Environment(AppStore.self) private var store

    private var version: String {
        let short = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(short) (\(build))"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                VStack(spacing: 8) {
                    Image("LaunchLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 96, height: 96)
                        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                    Text("FitBudget").font(.title2.bold())
                    Text("₹100 a Day. Better Every Day.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("Version \(version)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)

                bulletCard(
                    "What this app is",
                    [
                        "A budget-first fat-loss companion built around an affordable Indian diet.",
                        "It plans meals from a bundled food database, tracks what you actually eat and spend, and keeps your water, steps, workouts and weight in one place.",
                        "Everything works offline. There is no account and no server."
                    ]
                )

                bulletCard(
                    "How the estimates work",
                    [
                        "BMI = weight (kg) ÷ height (m)².",
                        "Maintenance calories use the Mifflin-St Jeor equation multiplied by your activity level.",
                        "The daily target applies a moderate deficit to maintenance, never below a conservative floor.",
                        "Weekly weight trend assumes roughly 7,700 kcal per kilogram of body fat.",
                        "Progress = (start weight − current weight) ÷ (start weight − target weight) × 100."
                    ]
                )

                VStack(alignment: .leading, spacing: 6) {
                    Text("Important").font(.subheadline.weight(.semibold))
                    Text(
                        "FitBudget does not diagnose, treat or promise any outcome. Nutrition and "
                        + "calorie figures are approximate estimates. For medical conditions, "
                        + "pregnancy, or special dietary needs, consult a qualified healthcare "
                        + "professional."
                    )
                    .font(.subheadline)
                }
                .fitCard(background: FitTheme.budget.opacity(0.12))

                bulletCard(
                    "Credits",
                    [
                        "Built with Swift, SwiftUI, SwiftData, Swift Charts and UserNotifications.",
                        "The calculations, validation and meal-plan generator live in a shared Swift package that is unit-tested on every build.",
                        "All icons and the FitBudget brand mark are drawn in-project; no third-party or copyrighted assets are used."
                    ]
                )
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func bulletCard(_ title: String, _ lines: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.subheadline.weight(.semibold))
            ForEach(lines, id: \.self) { line in
                HStack(alignment: .top, spacing: 6) {
                    Text("•")
                    Text(line).font(.subheadline)
                }
            }
        }
        .fitCard()
    }
}
