package com.fitbudget.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography is intentionally restrained: a few clear sizes, strong weights for numbers, and no
 * oversized decorative text.
 */
private val default = Typography()

val FitTypography = Typography(
    displaySmall = default.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = default.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = default.titleLarge.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = default.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = default.bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = default.bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = default.labelMedium.copy(fontWeight = FontWeight.Medium)
)

/** Big tabular-ish number style used on the dashboard cards. */
val MetricTextStyle = TextStyle(
    fontSize = 26.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp
)
