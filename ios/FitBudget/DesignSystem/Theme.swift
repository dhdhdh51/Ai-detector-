import SwiftUI
import UIKit
import FitBudgetCore

/// FitBudget's visual identity.
///
/// Surfaces use the system semantic colours so light and dark mode adapt natively, while the brand
/// and tracker accents are fixed so "water" is always blue and "budget" always amber - exactly like
/// the Android build.
enum FitTheme {

    // MARK: - Brand

    static let brand = Color(red: 0.0, green: 0.529, blue: 0.353)        // #00875A
    static let brandDark = Color(red: 0.373, green: 0.859, blue: 0.655)  // #5FDBA7

    /// Brand colour that stays legible in both schemes.
    static func brand(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? brandDark : brand
    }

    // MARK: - Tracker accents

    static let diet = Color(red: 0.0, green: 0.659, blue: 0.420)     // #00A86B
    static let budget = Color(red: 0.906, green: 0.573, blue: 0.090) // #E79217
    static let water = Color(red: 0.184, green: 0.525, blue: 1.0)    // #2F86FF
    static let steps = Color(red: 0.486, green: 0.361, blue: 1.0)    // #7C5CFF
    static let workout = Color(red: 1.0, green: 0.420, blue: 0.290)  // #FF6B4A
    static let weight = Color(red: 0.0, green: 0.690, blue: 0.651)   // #00B0A6
    static let streak = Color(red: 1.0, green: 0.541, blue: 0.239)   // #FF8A3D

    // MARK: - Surfaces

    static let background = Color(uiColor: .systemGroupedBackground)
    static let card = Color(uiColor: .secondarySystemGroupedBackground)
    static let subtleCard = Color(uiColor: .tertiarySystemGroupedBackground)
    static let separator = Color(uiColor: .separator)

    // MARK: - Metrics

    static let cardCorner: CGFloat = 20
    static let controlCorner: CGFloat = 14
    static let cardPadding: CGFloat = 16
    static let sectionSpacing: CGFloat = 12
}

extension ThemeMode {
    /// `nil` hands control back to the system setting.
    var preferredColorScheme: ColorScheme? {
        switch self {
        case .light: return .light
        case .dark: return .dark
        case .system: return nil
        }
    }
}

extension Text {
    /// Large tabular-feeling number used on the dashboard cards.
    func metricStyle() -> Text {
        font(.system(size: 26, weight: .bold, design: .rounded))
    }
}

extension View {
    /// The app's standard rounded container.
    func fitCard(
        background: Color = FitTheme.card,
        padding: CGFloat = FitTheme.cardPadding
    ) -> some View {
        self
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: FitTheme.cardCorner, style: .continuous))
    }
}
