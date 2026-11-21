package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = EmeraldVerified,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = CrimsonAccent,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonContainer,
    onTertiaryContainer = OnCrimsonContainer,
    background = ParchmentBackground,
    onBackground = InkPrimary,
    surface = ParchmentSurface,
    onSurface = InkPrimary,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = ParchmentOutline,
    outlineVariant = Color(0xFFE5DFD4)
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = OnSkyPrimaryDark,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = Color(0xFFD6E7F5),
    secondary = EmeraldDark,
    onSecondary = Color(0xFF003824),
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = Color(0xFFD0F2E4),
    tertiary = CrimsonDark,
    onTertiary = Color(0xFF5C0A15),
    tertiaryContainer = CrimsonContainerDark,
    onTertiaryContainer = Color(0xFFFFDAD9),
    background = MidnightBackground,
    onBackground = WarmInkLight,
    surface = MidnightSurface,
    onSurface = WarmInkLight,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = WarmInkMuted,
    outline = MidnightOutline,
    outlineVariant = Color(0xFF253447)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DevoirsTNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
