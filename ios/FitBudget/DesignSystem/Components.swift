import SwiftUI
import FitBudgetCore

// MARK: - Section header

struct SectionHeader: View {
    let title: String
    var subtitle: String?
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                if let subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.subheadline.weight(.semibold))
            }
        }
    }
}

// MARK: - Metric row

struct MetricRow: View {
    let label: String
    let value: String
    var valueColor: Color = .primary

    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer(minLength: 12)
            Text(value)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(valueColor)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Stat card

struct StatCard: View {
    let title: String
    let value: String
    let systemImage: String
    let accent: Color
    var caption: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(accent)
                    .frame(width: 30, height: 30)
                    .background(accent.opacity(0.16))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Text(value).metricStyle().lineLimit(1).minimumScaleFactor(0.7)
            if let caption {
                Text(caption)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .fitCard(padding: 14)
    }
}

// MARK: - Progress ring

struct ProgressRing<Content: View>: View {
    let fraction: Double
    var size: CGFloat = 120
    var lineWidth: CGFloat = 12
    var color: Color = FitTheme.brand
    @ViewBuilder var content: () -> Content

    private var clamped: Double { min(max(fraction, 0), 1) }

    var body: some View {
        ZStack {
            Circle()
                .stroke(FitTheme.subtleCard, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            Circle()
                .trim(from: 0, to: clamped)
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.45), value: clamped)
            content()
        }
        .frame(width: size, height: size)
        .accessibilityElement(children: .combine)
        .accessibilityValue(Formatters.percent(clamped))
    }
}

extension ProgressRing where Content == EmptyView {
    init(fraction: Double, size: CGFloat = 120, lineWidth: CGFloat = 12, color: Color = FitTheme.brand) {
        self.init(fraction: fraction, size: size, lineWidth: lineWidth, color: color) { EmptyView() }
    }
}

// MARK: - Labelled progress bar

struct LabeledProgressBar: View {
    let label: String
    let valueText: String
    let fraction: Double
    var color: Color = FitTheme.brand

    private var clamped: Double { min(max(fraction, 0), 1) }

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(label).font(.subheadline)
                Spacer(minLength: 8)
                Text(valueText).font(.subheadline.weight(.semibold))
            }
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Capsule().fill(FitTheme.subtleCard)
                    Capsule()
                        .fill(color)
                        .frame(width: max(geometry.size.width * clamped, clamped > 0 ? 8 : 0))
                        .animation(.easeOut(duration: 0.4), value: clamped)
                }
            }
            .frame(height: 8)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(label): \(valueText)")
    }
}

// MARK: - Streak chip

struct StreakChip: View {
    let label: String
    let days: Int

    private var active: Bool { days > 0 }

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "flame.fill")
                .font(.caption)
                .foregroundStyle(active ? FitTheme.streak : Color.secondary)
            VStack(alignment: .leading, spacing: 0) {
                Text(active ? "\(days) day\(days == 1 ? "" : "s")" : "—")
                    .font(.subheadline.weight(.bold))
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(active ? FitTheme.streak.opacity(0.15) : FitTheme.subtleCard)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

// MARK: - Empty state

struct EmptyStateView: View {
    let systemImage: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(.secondary)
                .frame(width: 60, height: 60)
                .background(FitTheme.subtleCard)
                .clipShape(Circle())
            Text(title).font(.headline).multilineTextAlignment(.center)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.bordered)
                    .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .padding(.horizontal, 16)
    }
}

// MARK: - Disclaimer

struct DisclaimerCard: View {
    var text: String = "Nutrition estimates are approximate. For medical conditions or special "
        + "dietary needs, consult a qualified healthcare professional."

    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(.secondary)
            .fitCard(background: FitTheme.subtleCard, padding: 14)
    }
}

// MARK: - Action banner

struct ActionBanner: View {
    let title: String
    let message: String
    let actionTitle: String
    let action: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "bell.badge.fill")
                .foregroundStyle(FitTheme.budget)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(message).font(.caption).foregroundStyle(.secondary)
                Button(actionTitle, action: action)
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .padding(.top, 4)
            }
            Spacer(minLength: 0)
        }
        .fitCard(background: FitTheme.budget.opacity(0.12), padding: 14)
    }
}

// MARK: - Numeric field

struct NumberField: View {
    let title: String
    @Binding var text: String
    var suffix: String?
    var supportingText: String?
    var errorMessage: String?
    var allowsDecimal: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                TextField(title, text: $text)
                    .keyboardType(allowsDecimal ? .decimalPad : .numberPad)
                    .textFieldStyle(.plain)
                if let suffix {
                    Text(suffix).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(FitTheme.subtleCard)
            .clipShape(RoundedRectangle(cornerRadius: FitTheme.controlCorner, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: FitTheme.controlCorner, style: .continuous)
                    .stroke(errorMessage == nil ? Color.clear : Color.red, lineWidth: 1)
            }

            if let message = errorMessage ?? supportingText {
                Text(message)
                    .font(.caption2)
                    .foregroundStyle(errorMessage == nil ? Color.secondary : Color.red)
            }
        }
    }
}

struct PlainField: View {
    let title: String
    @Binding var text: String
    var supportingText: String?
    var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            TextField(title, text: $text)
                .textFieldStyle(.plain)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(FitTheme.subtleCard)
                .clipShape(RoundedRectangle(cornerRadius: FitTheme.controlCorner, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: FitTheme.controlCorner, style: .continuous)
                        .stroke(errorMessage == nil ? Color.clear : Color.red, lineWidth: 1)
                }
            if let message = errorMessage ?? supportingText {
                Text(message)
                    .font(.caption2)
                    .foregroundStyle(errorMessage == nil ? Color.secondary : Color.red)
            }
        }
    }
}

// MARK: - Quantity stepper

struct QuantityStepper: View {
    @Binding var quantity: Double
    var step: Double = 0.5
    var range: ClosedRange<Double> = 0.5...20

    var body: some View {
        HStack(spacing: 12) {
            Button {
                quantity = min(max(quantity - step, range.lowerBound), range.upperBound)
            } label: {
                Image(systemName: "minus")
            }
            .buttonStyle(.bordered)
            .disabled(quantity - step < range.lowerBound)

            Text(Formatters.quantity(quantity))
                .font(.headline)
                .frame(minWidth: 44)

            Button {
                quantity = min(max(quantity + step, range.lowerBound), range.upperBound)
            } label: {
                Image(systemName: "plus")
            }
            .buttonStyle(.bordered)
            .disabled(quantity + step > range.upperBound)
        }
    }
}

// MARK: - Weekday picker

struct WeekdayPicker: View {
    @Binding var mask: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                ForEach(1...7, id: \.self) { iso in
                    let selected = WeekdayMask.contains(mask, isoWeekday: iso)
                    Button {
                        mask = WeekdayMask.toggling(mask, isoWeekday: iso)
                    } label: {
                        Text(WeekdayMask.shortName(isoWeekday: iso).prefix(1))
                            .font(.caption.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 32)
                            .background(selected ? FitTheme.brand.opacity(0.18) : FitTheme.subtleCard)
                            .foregroundStyle(selected ? FitTheme.brand : Color.secondary)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            Text(WeekdayMask.describe(mask))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Checklist row

struct ChecklistRow: View {
    let item: ChecklistItem

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: item.done ? "checkmark.square.fill" : "square")
                .foregroundStyle(item.done ? FitTheme.brand : Color.secondary)
            Text(item.label).font(.subheadline)
            Spacer(minLength: 8)
            if let detail = item.detail {
                Text(detail).font(.caption2).foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 3)
    }
}
