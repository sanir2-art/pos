package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PosPrimaryDark,
    onPrimary = PosOnPrimaryDark,
    primaryContainer = PosPrimaryContainerDark,
    onPrimaryContainer = PosOnPrimaryContainerDark,
    secondary = PosSecondaryDark,
    onSecondary = PosOnSecondaryDark,
    secondaryContainer = PosSecondaryContainerDark,
    onSecondaryContainer = PosOnSecondaryContainerDark,
    tertiary = PosTertiaryDark,
    onTertiary = PosOnTertiaryDark,
    tertiaryContainer = PosTertiaryContainerDark,
    onTertiaryContainer = PosOnTertiaryContainerDark,
    background = PosBackgroundDark,
    onBackground = PosOnBackgroundDark,
    surface = PosSurfaceDark,
    onSurface = PosOnSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onSurfaceVariant = PosOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PosPrimaryLight,
    onPrimary = PosOnPrimaryLight,
    primaryContainer = PosPrimaryContainerLight,
    onPrimaryContainer = PosOnPrimaryContainerLight,
    secondary = PosSecondaryLight,
    onSecondary = PosOnSecondaryLight,
    secondaryContainer = PosSecondaryContainerLight,
    onSecondaryContainer = PosOnSecondaryContainerLight,
    tertiary = PosTertiaryLight,
    onTertiary = PosOnTertiaryLight,
    tertiaryContainer = PosTertiaryContainerLight,
    onTertiaryContainer = PosOnTertiaryContainerLight,
    background = PosBackgroundLight,
    onBackground = PosOnBackgroundLight,
    surface = PosSurfaceLight,
    onSurface = PosOnSurfaceLight,
    surfaceVariant = PosSurfaceVariantLight,
    onSurfaceVariant = PosOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
