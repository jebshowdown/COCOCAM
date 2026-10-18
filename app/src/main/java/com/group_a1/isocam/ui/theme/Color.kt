package com.group_a1.isocam.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Coconut Colors
val LightBackground = Color(0xFFF6EEE3)
val LightOnBackground = Color(0xFF2E2118)
val LightSurface = Color(0xFFFBF6EE)
val LightOnSurface = Color(0xFF2E2118)
val LightSurfaceContainer = Color(0xFFEFE3D3)
val LightSurfaceContainerHigh = Color(0xFFE9DAC7)
val LightSurfaceVariant = Color(0xFFE8DACB)
val LightOnSurfaceVariant = Color(0xFF6E5846)
val LightPrimary = Color(0xFF8B5E3C)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE6CFB6)
val LightOnPrimaryContainer = Color(0xFF3B2412)
val LightSecondary = Color(0xFF966B43)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE2C9AC)
val LightOnSecondaryContainer = Color(0xFF3B2412)
val LightOutline = Color(0xFFB89F88)
val LightOutlineVariant = Color(0xFFDCC9B4)
val LightError = Color(0xFFB3261E)
val LightHeaderTint = Color(0xFFE3CDB0)

// Dark Coconut Colors
val DarkBackground = Color(0xFF1A120D)
val DarkOnBackground = Color(0xFFF1E4D6)
val DarkSurface = Color(0xFF231913)
val DarkOnSurface = Color(0xFFF1E4D6)
val DarkSurfaceContainer = Color(0xFF2B1F17)
val DarkSurfaceContainerHigh = Color(0xFF33251C)
val DarkSurfaceVariant = Color(0xFF3A2B21)
val DarkOnSurfaceVariant = Color(0xFFCBB6A2)
val DarkPrimary = Color(0xFFD9A877)
val DarkOnPrimary = Color(0xFF3A2410)
val DarkPrimaryContainer = Color(0xFF5A3B24)
val DarkOnPrimaryContainer = Color(0xFFF5DFC5)
val DarkSecondary = Color(0xFFC7A27F)
val DarkOnSecondary = Color(0xFF33200F)
val DarkSecondaryContainer = Color(0xFF4B3425)
val DarkOnSecondaryContainer = Color(0xFFF5DFC5)
val DarkOutline = Color(0xFF8F7A68)
val DarkOutlineVariant = Color(0xFF4A3A2E)
val DarkError = Color(0xFFFFB4AB)
val DarkHeaderTint = Color(0xFF4A3324)

// Rainbow Dolphins Colors
val DolphinBackground = Color(0xFF1B1240)
val DolphinOnBackground = Color(0xFFFFF4FF)
val DolphinSurface = Color(0xFF261A57)
val DolphinOnSurface = Color(0xFFFFF4FF)
val DolphinSurfaceContainer = Color(0xFF2F2268)
val DolphinSurfaceContainerHigh = Color(0xFF3A2B7A)
val DolphinSurfaceVariant = Color(0xFF3A2B7A)
val DolphinOnSurfaceVariant = Color(0xFFD9C8F5)

val DolphinPrimary = Color(0xFF4FD8E8) // Dolphin aqua
val DolphinOnPrimary = Color(0xFF04252B)
val DolphinPrimaryContainer = Color(0xFF15566E)
val DolphinOnPrimaryContainer = Color(0xFFCFF6FF)

val DolphinSecondary = Color(0xFFFF6FB5) // Hot pink
val DolphinOnSecondary = Color(0xFF3A0A22)
val DolphinSecondaryContainer = Color(0xFF7A2A66)
val DolphinOnSecondaryContainer = Color(0xFFFFD9EE)

val DolphinTertiary = Color(0xFFFFB347) // Sunset orange
val DolphinOnTertiary = Color(0xFF3D2100)
val DolphinTertiaryContainer = Color(0xFF7A4A12)
val DolphinOnTertiaryContainer = Color(0xFFFFE5C2)

val DolphinOutline = Color(0xFF9B87D6)
val DolphinOutlineVariant = Color(0xFF4B3C8C)
val DolphinError = Color(0xFFFF8A8A)
val DolphinOnError = Color(0xFF3B0A0A)

val LightCoconutColorScheme = lightColorScheme(
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError
)

val DarkCoconutColorScheme = darkColorScheme(
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError
)

val RainbowDolphinsColorScheme = darkColorScheme(
    background = DolphinBackground,
    onBackground = DolphinOnBackground,
    surface = DolphinSurface,
    onSurface = DolphinOnSurface,
    surfaceContainer = DolphinSurfaceContainer,
    surfaceContainerHigh = DolphinSurfaceContainerHigh,
    surfaceVariant = DolphinSurfaceVariant,
    onSurfaceVariant = DolphinOnSurfaceVariant,
    primary = DolphinPrimary,
    onPrimary = DolphinOnPrimary,
    primaryContainer = DolphinPrimaryContainer,
    onPrimaryContainer = DolphinOnPrimaryContainer,
    secondary = DolphinSecondary,
    onSecondary = DolphinOnSecondary,
    secondaryContainer = DolphinSecondaryContainer,
    onSecondaryContainer = DolphinOnSecondaryContainer,
    tertiary = DolphinTertiary,
    onTertiary = DolphinOnTertiary,
    tertiaryContainer = DolphinTertiaryContainer,
    onTertiaryContainer = DolphinOnTertiaryContainer,
    outline = DolphinOutline,
    outlineVariant = DolphinOutlineVariant,
    error = DolphinError,
    onError = DolphinOnError
)

@Immutable
data class IsoCamExtras(
    val headerTint: Color = Color.Unspecified,
    val rainbow: List<Color> = emptyList(),
    val sunsetHeader: List<Color> = emptyList(),
    val glow: Color = Color.Unspecified,
    val sparkle: Color = Color.Unspecified
)

val DolphinRainbow = listOf(
    Color(0xFFFF5E7E),
    Color(0xFFFF9A3C),
    Color(0xFFFFE14D),
    Color(0xFF5BE3A1),
    Color(0xFF4FA8FF),
    Color(0xFFB77CFF)
)

val DolphinSunsetHeader = listOf(
    Color(0xFF5B3FA8),
    Color(0xFFE85DA0),
    Color(0xFFFF9A3C),
    Color(0xFF1B1240)
)

val DolphinGlow = Color(0xFFFFD84D)
val DolphinSparkle = Color(0xFFFFFFFF).copy(alpha = 0.85f)

val LightCoconutExtras = IsoCamExtras(
    headerTint = LightHeaderTint,
    rainbow = emptyList(),
    sunsetHeader = listOf(LightHeaderTint, LightHeaderTint, LightBackground),
    glow = LightPrimary.copy(alpha = 0.15f),
    sparkle = Color.White.copy(alpha = 0.6f)
)

val DarkCoconutExtras = IsoCamExtras(
    headerTint = DarkHeaderTint,
    rainbow = emptyList(),
    sunsetHeader = listOf(DarkHeaderTint, DarkHeaderTint, DarkBackground),
    glow = DarkPrimary.copy(alpha = 0.15f),
    sparkle = Color.White.copy(alpha = 0.6f)
)

val RainbowDolphinsExtras = IsoCamExtras(
    headerTint = Color(0xFF5B3FA8),
    rainbow = DolphinRainbow,
    sunsetHeader = DolphinSunsetHeader,
    glow = DolphinGlow,
    sparkle = DolphinSparkle
)

val LocalIsoCamExtras = staticCompositionLocalOf { IsoCamExtras() }
