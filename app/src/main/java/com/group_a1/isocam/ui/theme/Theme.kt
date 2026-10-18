package com.group_a1.isocam.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    System,
    LightCoconut,
    DarkCoconut
}

object ThemeController {
    private const val PREFS_NAME = "isocam_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    var mode by mutableStateOf(ThemeMode.System)
        private set

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.System.name) ?: ThemeMode.System.name
        mode = try {
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.System
        }
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, newMode.name).apply()
    }
}

@Composable
fun IsoCamTheme(
    mode: ThemeMode = ThemeController.mode,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.LightCoconut -> false
        ThemeMode.DarkCoconut -> true
    }

    val colorScheme = if (darkTheme) DarkCoconutColorScheme else LightCoconutColorScheme
    val extras = if (darkTheme) DarkCoconutExtras else LightCoconutExtras

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalIsoCamExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes,
            typography = Typography,
            content = content
        )
    }
}
