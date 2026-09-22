package com.fitbudget.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Light scheme ----
val GreenPrimary = Color(0xFF00875A)
val GreenOnPrimary = Color(0xFFFFFFFF)
val GreenContainer = Color(0xFFB7F2D8)
val GreenOnContainer = Color(0xFF00281A)

val AmberSecondary = Color(0xFF8A5A00)
val AmberOnSecondary = Color(0xFFFFFFFF)
val AmberContainer = Color(0xFFFFDDA8)
val AmberOnContainer = Color(0xFF2B1700)

val BlueTertiary = Color(0xFF1B5E9C)
val BlueOnTertiary = Color(0xFFFFFFFF)
val BlueContainer = Color(0xFFCFE5FF)
val BlueOnTertiaryContainer = Color(0xFF002F52)

val SurfaceLight = Color(0xFFF7FBF8)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFDCE5DF)
val OnSurfaceLight = Color(0xFF0E1512)
val OnSurfaceVariantLight = Color(0xFF414944)
val OutlineLight = Color(0xFF717974)

// ---- Dark scheme ----
val GreenPrimaryDark = Color(0xFF5FDBA7)
val GreenOnPrimaryDark = Color(0xFF003824)
val GreenContainerDark = Color(0xFF005238)
val GreenOnContainerDark = Color(0xFFB7F2D8)

val AmberSecondaryDark = Color(0xFFF5C26B)
val AmberOnSecondaryDark = Color(0xFF452B00)
val AmberContainerDark = Color(0xFF634000)
val AmberOnContainerDark = Color(0xFFFFDDA8)

val BlueTertiaryDark = Color(0xFF9CCAFF)
val BlueOnTertiaryDark = Color(0xFF003257)
val BlueContainerDark = Color(0xFF00497D)
val BlueOnTertiaryContainerDark = Color(0xFFCFE5FF)

val SurfaceDark = Color(0xFF0C1310)
val SurfaceContainerDark = Color(0xFF16201B)
val SurfaceVariantDark = Color(0xFF3F4944)
val OnSurfaceDark = Color(0xFFDEE5E0)
val OnSurfaceVariantDark = Color(0xFFBFC9C3)
val OutlineDark = Color(0xFF899390)

val ErrorLight = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)

/**
 * Fixed accent colours used by the trackers so that "water" is always blue, "budget" always amber
 * and so on, in both light and dark mode.
 */
object FitAccent {
    val diet = Color(0xFF00A86B)
    val budget = Color(0xFFE79217)
    val water = Color(0xFF2F86FF)
    val steps = Color(0xFF7C5CFF)
    val workout = Color(0xFFFF6B4A)
    val weight = Color(0xFF00B0A6)
    val streak = Color(0xFFFF8A3D)
}
