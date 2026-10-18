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
val LightSecondary = Color(0xFF966B43) // Adjusted for >= 4.5:1 contrast against white text
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

@Immutable
data class IsoCamExtras(
    val headerTint: Color = Color.Unspecified
)

val LightCoconutExtras = IsoCamExtras(headerTint = LightHeaderTint)
val DarkCoconutExtras = IsoCamExtras(headerTint = DarkHeaderTint)

val LocalIsoCamExtras = staticCompositionLocalOf { IsoCamExtras() }
