package com.example.todo.common.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class ThemePalette(
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryContainer: Color,
    val aiAccent: Color,
    val accent: Color,
    val accentLight: Color,
    val accentContainer: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val background: Color,
    val secondaryBackground: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val disabled: Color
)

val themePalettes = mapOf(
    "ORANGE" to ThemePalette(
        primary = Color(0xFFFF7A00),
        primaryLight = Color(0xFFFFA040),
        primaryDark = Color(0xFFE65100),
        primaryContainer = Color(0xFF3E1F00),
        aiAccent = Color(0xFF4FC3F7),
        accent = Color(0xFFFFD54F),
        accentLight = Color(0xFFFFE082),
        accentContainer = Color(0xFF3E3200),
        success = Color(0xFF00E676),
        warning = Color(0xFFFFB300),
        danger = Color(0xFFFF5252),
        info = Color(0xFF4FC3F7),
        background = Color(0xFF0D0D10),
        secondaryBackground = Color(0xFF17181D),
        surface = Color(0xFF17181D),
        surfaceElevated = Color(0xFF20222A),
        border = Color(0xFF2A2D37),
        textPrimary = Color(0xFFF5F5F5),
        textSecondary = Color(0xFF9AA0A6),
        disabled = Color(0xFF555964)
    ),
    "PURPLE" to ThemePalette(
        primary = Color(0xFF7C6CFF),
        primaryLight = Color(0xFFA393FF),
        primaryDark = Color(0xFF4F46E5),
        primaryContainer = Color(0xFF1A1740),
        aiAccent = Color(0xFF00D9FF),
        accent = Color(0xFFFF6B6B),
        accentLight = Color(0xFFFF8A8A),
        accentContainer = Color(0xFF3D1A1A),
        success = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        info = Color(0xFF00D9FF),
        background = Color(0xFF09090B),
        secondaryBackground = Color(0xFF111118),
        surface = Color(0xFF15151D),
        surfaceElevated = Color(0xFF1D1D27),
        border = Color(0xFF2A2A36),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFA1A1AA),
        disabled = Color(0xFF6B7280)
    ),
    "RED" to ThemePalette(
        primary = Color(0xFFFF3B30),
        primaryLight = Color(0xFFFF6961),
        primaryDark = Color(0xFFD32F2F),
        primaryContainer = Color(0xFF3D1414),
        aiAccent = Color(0xFF4FC3F7),
        accent = Color(0xFFFFD60A),
        accentLight = Color(0xFFFFE066),
        accentContainer = Color(0xFF3E3200),
        success = Color(0xFF34C759),
        warning = Color(0xFFFFD60A),
        danger = Color(0xFFFF3B30),
        info = Color(0xFF4FC3F7),
        background = Color(0xFF0B0B0B),
        secondaryBackground = Color(0xFF141414),
        surface = Color(0xFF141414),
        surfaceElevated = Color(0xFF1C1C1E),
        border = Color(0xFF2C2C2E),
        textPrimary = Color(0xFFF2F2F7),
        textSecondary = Color(0xFF8E8E93),
        disabled = Color(0xFF636366)
    ),
    "BLUE" to ThemePalette(
        primary = Color(0xFF007AFF),
        primaryLight = Color(0xFF64D2FF),
        primaryDark = Color(0xFF0056B3),
        primaryContainer = Color(0xFF0A2540),
        aiAccent = Color(0xFF5856D6),
        accent = Color(0xFF5856D6),
        accentLight = Color(0xFFBF5AF2),
        accentContainer = Color(0xFF2C1C3D),
        success = Color(0xFF34C759),
        warning = Color(0xFFFFCC00),
        danger = Color(0xFFFF3B30),
        info = Color(0xFF007AFF),
        background = Color(0xFF0A0A0A),
        secondaryBackground = Color(0xFF1C1C1E),
        surface = Color(0xFF1C1C1E),
        surfaceElevated = Color(0xFF2C2C2E),
        border = Color(0xFF3A3A3C),
        textPrimary = Color(0xFFF2F2F7),
        textSecondary = Color(0xFF8E8E93),
        disabled = Color(0xFF48484A)
    ),
    "MONOCHROME" to ThemePalette(
        primary = Color(0xFF00E5FF),
        primaryLight = Color(0xFF80F2FF),
        primaryDark = Color(0xFF00B2CC),
        primaryContainer = Color(0xFF00333D),
        aiAccent = Color(0xFF00E5FF),
        accent = Color(0xFF00E5FF),
        accentLight = Color(0xFF80F2FF),
        accentContainer = Color(0xFF00333D),
        success = Color(0xFF00E5BB),
        warning = Color(0xFFFFD600),
        danger = Color(0xFFFF3B30),
        info = Color(0xFF00E5FF),
        background = Color(0xFF0B0B0C),
        secondaryBackground = Color(0xFF17181D),
        surface = Color(0xFF17181D),
        surfaceElevated = Color(0xFF202124),
        border = Color(0xFF2A2A31),
        textPrimary = Color(0xFFF5F5F5),
        textSecondary = Color(0xFFA1A1AA),
        disabled = Color(0xFF555964)
    ),
    "EMERALD" to ThemePalette(
        primary = Color(0xFF00C853),
        primaryLight = Color(0xFF5EFD82),
        primaryDark = Color(0xFF009624),
        primaryContainer = Color(0xFF00330C),
        aiAccent = Color(0xFF00E676),
        accent = Color(0xFFFFD54F),
        accentLight = Color(0xFFFFE082),
        accentContainer = Color(0xFF3E3200),
        success = Color(0xFF69F0AE),
        warning = Color(0xFFFFD54F),
        danger = Color(0xFFFF5252),
        info = Color(0xFF00E676),
        background = Color(0xFF0A0D0B),
        secondaryBackground = Color(0xFF141A16),
        surface = Color(0xFF141A16),
        surfaceElevated = Color(0xFF1E2A22),
        border = Color(0xFF26332B),
        textPrimary = Color(0xFFE8F5E9),
        textSecondary = Color(0xFFA7B7A9),
        disabled = Color(0xFF26332B)
    ),
    "CREAM" to ThemePalette(
        primary = Color(0xFF3A6FF7),
        primaryLight = Color(0xFF6B8EFF),
        primaryDark = Color(0xFF1E40AF),
        primaryContainer = Color(0xFFDBEAFE),
        aiAccent = Color(0xFFFF8A3D),
        accent = Color(0xFF00B8D4),
        accentLight = Color(0xFF33C6DD),
        accentContainer = Color(0xFFE0F7FA),
        success = Color(0xFF22C55E),
        warning = Color(0xFFF59E0B),
        danger = Color(0xFFEF4444),
        info = Color(0xFF3A6FF7),
        background = Color(0xFFFAF7F2),
        secondaryBackground = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFF1F5F9),
        border = Color(0xFFE5E7EB),
        textPrimary = Color(0xFF1F2937),
        textSecondary = Color(0xFF6B7280),
        disabled = Color(0xFFD1D5DB)
    ),
    "MIDNIGHT" to ThemePalette(
        primary = Color(0xFF39FF14),
        primaryLight = Color(0xFF80FF72),
        primaryDark = Color(0xFF00C800),
        primaryContainer = Color(0xFF003300),
        aiAccent = Color(0xFFFF00FF),
        accent = Color(0xFFFF00FF),
        accentLight = Color(0xFFFF66FF),
        accentContainer = Color(0xFF330033),
        success = Color(0xFF39FF14),
        warning = Color(0xFFFF0000),
        danger = Color(0xFFFF0033),
        info = Color(0xFF00FFFF),
        background = Color(0xFF000000),
        secondaryBackground = Color(0xFF080808),
        surface = Color(0xFF080808),
        surfaceElevated = Color(0xFF121212),
        border = Color(0xFF1A1A1A),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF888888),
        disabled = Color(0xFF333333)
    )
)

// Premium Dark Palette - Inspired by Linear/Notion
object MustDoColors {
    // "ORANGE", "PURPLE", "RED", "BLUE", "MONOCHROME", "EMERALD", "CREAM", "MIDNIGHT"
    var currentTheme by mutableStateOf("ORANGE")

    // Convenience check kept for backward compat
    val isPurpleTheme: Boolean get() = currentTheme == "PURPLE"
    
    private val currentPalette: ThemePalette
        get() = themePalettes[currentTheme] ?: themePalettes["ORANGE"]!!

    // Primary brand
    val Primary: Color get() = currentPalette.primary
    val PrimaryLight: Color get() = currentPalette.primaryLight
    val PrimaryDark: Color get() = currentPalette.primaryDark
    val PrimaryContainer: Color get() = currentPalette.primaryContainer
    
    // AI Accent
    val AIAccent: Color get() = currentPalette.aiAccent

    // Accent
    val Accent: Color get() = currentPalette.accent
    val AccentLight: Color get() = currentPalette.accentLight
    val AccentContainer: Color get() = currentPalette.accentContainer

    // Status Colors
    val Success: Color get() = currentPalette.success
    val Warning: Color get() = currentPalette.warning
    val Danger: Color get() = currentPalette.danger
    val Info: Color get() = currentPalette.info

    // Dark backgrounds
    val DarkBackground: Color get() = currentPalette.background
    val DarkSecondaryBackground: Color get() = currentPalette.secondaryBackground
    val DarkSurface: Color get() = currentPalette.surface
    val DarkSurfaceElevated: Color get() = currentPalette.surfaceElevated
    val DarkBorder: Color get() = currentPalette.border

    // AMOLED backgrounds
    val AmoledBackground = Color(0xFF000000)
    val AmoledSurface = Color(0xFF0D0D10)
    val AmoledSurfaceVariant = Color(0xFF17181D)
    val AmoledCard = Color(0xFF17181D)
    val AmoledCardElevated = Color(0xFF20222A)
    val AmoledBorder = Color(0xFF2A2D37)

    // Light backgrounds (Fallback)
    val LightBackground = Color(0xFFF8F9FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF0F1F5)
    val LightCard = Color(0xFFFFFFFF)
    val LightCardElevated = Color(0xFFF5F5FA)
    val LightBorder = Color(0xFFE2E4EA)

    // Text colors - Dark theme
    val DarkTextPrimary: Color get() = currentPalette.textPrimary
    val DarkTextSecondary: Color get() = currentPalette.textSecondary
    val DarkDisabled: Color get() = currentPalette.disabled

    // Text colors - Light theme
    val LightTextPrimary = Color(0xFF1A1A2E)
    val LightTextSecondary = Color(0xFF6B6B80)
    val LightTextTertiary = Color(0xFF9494A8)

    // Priority colors
    val PriorityLow = Color(0xFF34D399) // Success
    val PriorityMedium = Color(0xFF00D9FF) // Info / Blue-Cyan
    val PriorityHigh = Color(0xFFFBBF24) // Warning / Orange
    val PriorityUrgent = Color(0xFFF87171) // Danger / Red

    // Glassmorphism
    val GlassWhite = Color(0x1AFFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
    val GlassOverlay = Color(0x0DFFFFFF)

    // Chart colors
    val ChartPalette: List<Color>
        get() = listOf(
            Primary,
            Accent,
            Success,
            Warning,
            Info,
            Color(0xFFFF922B),
            Color(0xFFA855F7)
        )

    // Dynamic Accents
    val Accents: List<Color>
        get() = listOf(
            Primary, 
            Color(0xFFF43F5E), 
            Success, 
            Warning, 
            Info, 
            Accent, 
            Color(0xFFA855F7), 
            Color(0xFF14B8A6), 
            Color(0xFFFF922B), 
            Color(0xFF38BDF8)  
        )
}
