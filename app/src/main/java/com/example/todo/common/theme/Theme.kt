package com.example.todo.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MustDoColors.PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E6FF),
    onPrimaryContainer = MustDoColors.PrimaryDark,
    secondary = MustDoColors.Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0E0),
    onSecondaryContainer = Color(0xFFB71C1C),
    tertiary = MustDoColors.Info,
    tertiaryContainer = Color(0xFFE0F7FA),
    background = MustDoColors.LightBackground,
    onBackground = MustDoColors.LightTextPrimary,
    surface = MustDoColors.LightSurface,
    onSurface = MustDoColors.LightTextPrimary,
    surfaceVariant = MustDoColors.LightSurfaceVariant,
    onSurfaceVariant = MustDoColors.LightTextSecondary,
    surfaceContainerLow = MustDoColors.LightCard,
    surfaceContainer = MustDoColors.LightCard,
    surfaceContainerHigh = MustDoColors.LightCardElevated,
    outline = MustDoColors.LightBorder,
    outlineVariant = MustDoColors.LightBorder,
    error = MustDoColors.Accent,
    onError = Color.White,
)

@Composable
fun MustDoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    useDynamicColor: Boolean = false,
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val dynamicDark = darkColorScheme(
        primary = MustDoColors.Primary,
        onPrimary = Color.White,
        primaryContainer = MustDoColors.PrimaryContainer,
        onPrimaryContainer = MustDoColors.PrimaryLight,
        secondary = MustDoColors.Accent,
        onSecondary = Color.White,
        secondaryContainer = MustDoColors.AccentContainer,
        onSecondaryContainer = MustDoColors.AccentLight,
        tertiary = MustDoColors.AIAccent,
        tertiaryContainer = MustDoColors.PrimaryContainer,
        background = MustDoColors.DarkBackground,
        onBackground = MustDoColors.DarkTextPrimary,
        surface = MustDoColors.DarkSurface,
        onSurface = MustDoColors.DarkTextPrimary,
        surfaceVariant = MustDoColors.DarkSecondaryBackground,
        onSurfaceVariant = MustDoColors.DarkTextSecondary,
        surfaceContainerLow = MustDoColors.DarkSurface,
        surfaceContainer = MustDoColors.DarkSurface,
        surfaceContainerHigh = MustDoColors.DarkSurfaceElevated,
        outline = MustDoColors.DarkBorder,
        outlineVariant = MustDoColors.DarkBorder,
        error = MustDoColors.Danger,
        onError = Color.White,
    )

    val dynamicAmoled = darkColorScheme(
        primary = MustDoColors.Primary,
        onPrimary = Color.White,
        primaryContainer = MustDoColors.PrimaryContainer,
        onPrimaryContainer = MustDoColors.PrimaryLight,
        secondary = MustDoColors.Accent,
        onSecondary = Color.White,
        secondaryContainer = MustDoColors.AccentContainer,
        onSecondaryContainer = MustDoColors.AccentLight,
        tertiary = MustDoColors.Info,
        tertiaryContainer = MustDoColors.PrimaryContainer,
        background = MustDoColors.AmoledBackground,
        onBackground = MustDoColors.DarkTextPrimary,
        surface = MustDoColors.AmoledSurface,
        onSurface = MustDoColors.DarkTextPrimary,
        surfaceVariant = MustDoColors.AmoledSurfaceVariant,
        onSurfaceVariant = MustDoColors.DarkTextSecondary,
        surfaceContainerLow = MustDoColors.AmoledCard,
        surfaceContainer = MustDoColors.AmoledCard,
        surfaceContainerHigh = MustDoColors.AmoledCardElevated,
        outline = MustDoColors.AmoledBorder,
        outlineVariant = MustDoColors.AmoledBorder,
        error = MustDoColors.Accent,
        onError = Color.White,
    )

    var colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && isAmoled -> dynamicAmoled
        darkTheme -> dynamicDark
        else -> LightColorScheme
    }

    if (accentColor != null && !useDynamicColor) {
        // Simple blend to create containers
        val container = accentColor.copy(alpha = 0.2f)
        val onContainer = accentColor
        colorScheme = colorScheme.copy(
            primary = accentColor,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = accentColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                val isLightBars = !darkTheme || MustDoColors.currentTheme == "CREAM"
                isAppearanceLightStatusBars = isLightBars
                isAppearanceLightNavigationBars = isLightBars
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MustDoTypography,
        content = content
    )
}
