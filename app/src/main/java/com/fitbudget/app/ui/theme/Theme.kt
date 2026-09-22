package com.fitbudget.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fitbudget.app.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary,
    tertiaryContainer = BlueContainer,
    onTertiaryContainer = BlueOnTertiaryContainer,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerLight,
    surfaceContainerLow = SurfaceLight,
    outline = OutlineLight,
    outlineVariant = SurfaceVariantLight,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorLight
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = GreenOnContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark,
    tertiary = BlueTertiaryDark,
    onTertiary = BlueOnTertiaryDark,
    tertiaryContainer = BlueContainerDark,
    onTertiaryContainer = BlueOnTertiaryContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerDark,
    surfaceContainerLow = SurfaceDark,
    outline = OutlineDark,
    outlineVariant = SurfaceVariantDark,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorDark
)

@Composable
fun FitBudgetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitTypography,
        content = content
    )
}

/** True when the effective scheme is dark; used for status-bar icon colours. */
@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}
