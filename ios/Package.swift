// swift-tools-version: 5.9
import PackageDescription

// FitBudgetCore holds every calculation, validation rule, seed dataset and the meal-plan
// generator. It depends on Foundation only - no SwiftUI, SwiftData, UIKit or CoreMotion - so the
// exact same logic that ships in the iOS app can be built and unit-tested on any platform,
// including the Linux CI container.
let package = Package(
    name: "FitBudgetCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v13)
    ],
    products: [
        .library(name: "FitBudgetCore", targets: ["FitBudgetCore"])
    ],
    targets: [
        .target(
            name: "FitBudgetCore",
            path: "Sources/FitBudgetCore"
        ),
        .testTarget(
            name: "FitBudgetCoreTests",
            dependencies: ["FitBudgetCore"],
            path: "Tests/FitBudgetCoreTests"
        )
    ]
)
