import Foundation

public struct BudgetSnapshot: Equatable, Sendable {
    public let budget: Double
    public let spent: Double
    /// Cost of items planned for today but not ticked off yet.
    public let pendingPlannedCost: Double

    public init(budget: Double, spent: Double, pendingPlannedCost: Double = 0) {
        self.budget = budget
        self.spent = spent
        self.pendingPlannedCost = pendingPlannedCost
    }

    public var remaining: Double { budget - spent }
    public var isOverBudget: Bool { spent > budget }
    public var overBy: Double { max(spent - budget, 0) }
    public var usedFraction: Double {
        guard budget > 0 else { return 0 }
        return min(max(spent / budget, 0), 1)
    }
    public var projectedSpend: Double { spent + pendingPlannedCost }
    public var projectedOverBudget: Bool { budget > 0 && projectedSpend > budget }
}

public struct MonthlyBudgetSummary: Equatable, Sendable {
    public let daysTracked: Int
    public let totalBudget: Double
    public let totalSpent: Double
    public let daysUnderBudget: Int
    public let daysOverBudget: Int

    public init(
        daysTracked: Int,
        totalBudget: Double,
        totalSpent: Double,
        daysUnderBudget: Int,
        daysOverBudget: Int
    ) {
        self.daysTracked = daysTracked
        self.totalBudget = totalBudget
        self.totalSpent = totalSpent
        self.daysUnderBudget = daysUnderBudget
        self.daysOverBudget = daysOverBudget
    }

    public var averageDailySpend: Double {
        guard daysTracked > 0 else { return 0 }
        return totalSpent / Double(daysTracked)
    }

    public var remaining: Double { totalBudget - totalSpent }

    /// Share of tracked days that finished at or under budget, 0...1.
    public var adherenceFraction: Double {
        guard daysTracked > 0 else { return 0 }
        return Double(daysUnderBudget) / Double(daysTracked)
    }

    public var adherencePercent: Int { Int((adherenceFraction * 100).rounded()) }

    public static let empty = MonthlyBudgetSummary(
        daysTracked: 0,
        totalBudget: 0,
        totalSpent: 0,
        daysUnderBudget: 0,
        daysOverBudget: 0
    )
}

public enum BudgetCalculator {

    public static func snapshot(
        budget: Double,
        spent: Double,
        pendingPlannedCost: Double = 0
    ) -> BudgetSnapshot {
        BudgetSnapshot(
            budget: max(budget, 0),
            spent: max(spent, 0),
            pendingPlannedCost: max(pendingPlannedCost, 0)
        )
    }

    /// Aggregates only days that actually have tracked spending.
    public static func monthly(_ summaries: [DaySummary]) -> MonthlyBudgetSummary {
        let tracked = summaries.filter { $0.spent > 0 && $0.budget > 0 }
        return MonthlyBudgetSummary(
            daysTracked: tracked.count,
            totalBudget: tracked.reduce(0) { $0 + $1.budget },
            totalSpent: tracked.reduce(0) { $0 + $1.spent },
            daysUnderBudget: tracked.filter { $0.spent <= $0.budget }.count,
            daysOverBudget: tracked.filter { $0.spent > $0.budget }.count
        )
    }

    /// Cost of one line item = per-serving cost × servings.
    public static func lineCost(costPerServing: Double, quantity: Double) -> Double {
        max(costPerServing, 0) * max(quantity, 0)
    }

    public static func round2(_ value: Double) -> Double {
        (value * 100).rounded() / 100
    }
}
