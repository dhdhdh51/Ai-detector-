import Foundation

public struct WaterSnapshot: Equatable, Sendable {
    public let consumedMl: Int
    public let targetMl: Int

    public init(consumedMl: Int, targetMl: Int) {
        self.consumedMl = consumedMl
        self.targetMl = targetMl
    }

    public var remainingMl: Int { max(targetMl - consumedMl, 0) }
    public var isGoalMet: Bool { targetMl > 0 && consumedMl >= targetMl }
    public var fraction: Double {
        guard targetMl > 0 else { return 0 }
        return min(max(Double(consumedMl) / Double(targetMl), 0), 1)
    }
    public var percent: Int { Int((fraction * 100).rounded()) }
    /// How many 250 ml glasses are still needed.
    public var glassesRemaining: Int { Int((Double(remainingMl) / 250).rounded(.up)) }
}

public enum WaterCalculator {

    public static let quickAmountsMl = [250, 500, 750, 1_000]

    public static let defaultTargetMl = 2_500

    public static func snapshot(consumedMl: Int, targetMl: Int) -> WaterSnapshot {
        WaterSnapshot(consumedMl: max(consumedMl, 0), targetMl: max(targetMl, 0))
    }

    public static func total(_ amountsMl: [Int]) -> Int {
        amountsMl.filter { $0 > 0 }.reduce(0, +)
    }

    /// Average daily intake across the supplied days (days with no log count as 0).
    public static func averageMl(_ dailyTotals: [Int]) -> Int {
        guard !dailyTotals.isEmpty else { return 0 }
        return dailyTotals.reduce(0, +) / dailyTotals.count
    }

    /// Share of days where the target was met, 0...1.
    public static func adherenceFraction(dailyTotals: [Int], targetMl: Int) -> Double {
        guard !dailyTotals.isEmpty, targetMl > 0 else { return 0 }
        return Double(dailyTotals.filter { $0 >= targetMl }.count) / Double(dailyTotals.count)
    }
}
