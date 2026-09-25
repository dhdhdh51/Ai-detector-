import SwiftUI
import Charts
import FitBudgetCore

/// A single charted point. Dates (not day numbers) are used on the x-axis so Swift Charts can
/// format the labels for the user's locale.
struct ChartPoint: Identifiable {
    let id: Int
    let date: Date
    let value: Double

    init(day: DayKey, value: Double) {
        self.id = day.value
        self.date = DayCalendar.date(from: day)
        self.value = value
    }
}

/// Weight trend with an optional dashed target line.
struct TrendLineChart: View {
    let points: [ChartPoint]
    var color: Color = FitTheme.weight
    var targetValue: Double?
    var height: CGFloat = 180
    var valueLabel: String = "kg"

    private var bounds: (min: Double, max: Double) {
        let values = points.map(\.value) + (targetValue.map { [$0] } ?? [])
        guard let low = values.min(), let high = values.max() else { return (0, 1) }
        let padding = max((high - low) * 0.15, 0.5)
        return (low - padding, high + padding)
    }

    var body: some View {
        if points.isEmpty {
            EmptyView()
        } else {
            Chart {
                ForEach(points) { point in
                    AreaMark(
                        x: .value("Date", point.date),
                        y: .value(valueLabel, point.value)
                    )
                    .foregroundStyle(color.opacity(0.15))
                    .interpolationMethod(.monotone)

                    LineMark(
                        x: .value("Date", point.date),
                        y: .value(valueLabel, point.value)
                    )
                    .foregroundStyle(color)
                    .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                    .interpolationMethod(.monotone)

                    PointMark(
                        x: .value("Date", point.date),
                        y: .value(valueLabel, point.value)
                    )
                    .foregroundStyle(color)
                    .symbolSize(28)
                }

                if let targetValue {
                    RuleMark(y: .value("Target", targetValue))
                        .foregroundStyle(FitTheme.budget)
                        .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                        .annotation(position: .top, alignment: .leading) {
                            Text("Target")
                                .font(.caption2)
                                .foregroundStyle(FitTheme.budget)
                        }
                }
            }
            .chartYScale(domain: bounds.min...bounds.max)
            .chartXAxis {
                AxisMarks(preset: .aligned, values: .automatic(desiredCount: 4)) {
                    AxisValueLabel(format: .dateTime.day().month(.abbreviated))
                }
            }
            .chartYAxis {
                AxisMarks(position: .leading, values: .automatic(desiredCount: 4))
            }
            .frame(height: height)
        }
    }
}

/// Daily bars (spending, steps, checklist) with an optional goal/limit line.
struct DailyBarChart: View {
    let points: [ChartPoint]
    var color: Color = FitTheme.budget
    var overLimitColor: Color = .red
    var limitValue: Double?
    var height: CGFloat = 180
    var valueLabel: String = "Value"

    var body: some View {
        if points.isEmpty {
            EmptyView()
        } else {
            Chart {
                ForEach(points) { point in
                    BarMark(
                        x: .value("Date", point.date, unit: .day),
                        y: .value(valueLabel, point.value)
                    )
                    .foregroundStyle(isOverLimit(point.value) ? overLimitColor : color)
                    .cornerRadius(3)
                }

                if let limitValue, limitValue > 0 {
                    RuleMark(y: .value("Limit", limitValue))
                        .foregroundStyle(.secondary)
                        .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                }
            }
            .chartXAxis {
                AxisMarks(preset: .aligned, values: .automatic(desiredCount: 4)) {
                    AxisValueLabel(format: .dateTime.day().month(.abbreviated))
                }
            }
            .chartYAxis {
                AxisMarks(position: .leading, values: .automatic(desiredCount: 4))
            }
            .frame(height: height)
        }
    }

    private func isOverLimit(_ value: Double) -> Bool {
        guard let limitValue, limitValue > 0 else { return false }
        return value > limitValue
    }
}

/// Compact seven-day strip used on the dashboard.
struct MiniBarStrip: View {
    let values: [Int]
    let goal: Int
    var color: Color = FitTheme.steps
    var height: CGFloat = 44

    var body: some View {
        let maxValue = max(values.max() ?? 0, goal, 1)
        HStack(alignment: .bottom, spacing: 6) {
            ForEach(Array(values.enumerated()), id: \.offset) { _, value in
                let fraction = min(Double(value) / Double(maxValue), 1)
                ZStack(alignment: .bottom) {
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(FitTheme.subtleCard)
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(color)
                        .frame(height: max(height * fraction, value > 0 ? 3 : 0))
                }
                .frame(height: height)
            }
        }
    }
}
