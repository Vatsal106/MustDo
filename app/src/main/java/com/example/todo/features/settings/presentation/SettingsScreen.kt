package com.example.todo.features.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToTimerFocus: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        themeMode = state.themeMode,
        colorTheme = state.colorTheme,
        pomodoroDuration = state.pomodoroDuration,
        breakDuration = state.breakDuration,
        deepWorkDuration = state.deepWorkDuration,
        onThemeModeChange = viewModel::setThemeMode,
        onColorThemeChange = viewModel::setColorTheme,
        onPomodoroDurationChange = { viewModel.setPomodoroDuration(it.toInt()) },
        onBreakDurationChange = { viewModel.setBreakDuration(it.toInt()) },
        onDeepWorkDurationChange = { viewModel.setDeepWorkDuration(it.toInt()) },
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToAchievements = onNavigateToAchievements,
        onNavigateToAdvanced = onNavigateToAdvanced,
        onNavigateToAbout = onNavigateToAbout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    themeMode: String = "SYSTEM",
    colorTheme: String = "ORANGE",
    pomodoroDuration: Int = 25,
    breakDuration: Int = 5,
    deepWorkDuration: Int = 90,
    onThemeModeChange: (String) -> Unit = {},
    onColorThemeChange: (String) -> Unit = {},
    onPomodoroDurationChange: (Float) -> Unit = {},
    onBreakDurationChange: (Float) -> Unit = {},
    onDeepWorkDurationChange: (Float) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Theme Selection Section ---
            item {
                Text(
                    text = "THEME",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                SharedSettingsCard {
                    SharedSettingsRadioGroup(
                        label = "App Theme",
                        icon = Icons.Outlined.DarkMode,
                        options = listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System"),
                        selected = themeMode,
                        onSelect = onThemeModeChange
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "COLOR BRAND PALETTE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                ThemePaletteSelector(
                    selected = colorTheme,
                    onSelect = onColorThemeChange
                )
            }

            // --- Timer Settings Section ---
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TIMER & FOCUS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                SharedSettingsCard {
                    SharedSettingsSlider(
                        label = "Pomodoro Duration",
                        icon = Icons.Outlined.Timer,
                        value = pomodoroDuration,
                        range = 5f..60f,
                        suffix = "min",
                        onValueChange = onPomodoroDurationChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsSlider(
                        label = "Break Duration",
                        icon = Icons.Outlined.Coffee,
                        value = breakDuration,
                        range = 1f..30f,
                        suffix = "min",
                        onValueChange = onBreakDurationChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsSlider(
                        label = "Deep Work Duration",
                        icon = Icons.Outlined.Psychology,
                        value = deepWorkDuration,
                        range = 30f..180f,
                        suffix = "min",
                        onValueChange = onDeepWorkDurationChange
                    )
                }
            }

            // --- General Section ---
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NOTIFICATIONS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                SharedSettingsCard {
                    SettingsMenuRow(
                        icon = Icons.Outlined.Notifications,
                        iconTint = MustDoColors.Warning,
                        title = "Notification Settings",
                        subtitle = "Reminders, sounds, quiet hours",
                        onClick = onNavigateToNotifications
                    )
                }
            }

            // --- Organization Section ---
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ORGANIZATION",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            item {
                SharedSettingsCard {
                    SettingsMenuRow(
                        icon = Icons.Outlined.Label,
                        iconTint = MustDoColors.Success,
                        title = "Categories & Labels",
                        subtitle = "Manage task categories",
                        onClick = onNavigateToCategories
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsMenuRow(
                        icon = Icons.Outlined.EmojiEvents,
                        iconTint = MustDoColors.Warning,
                        title = "Gamification Hub",
                        subtitle = "Level, XP, Streaks & Badges",
                        onClick = onNavigateToAchievements
                    )
                }
            }

            // --- Advanced Section ---
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ADVANCED & ABOUT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            item {
                SharedSettingsCard {
                    SettingsMenuRow(
                        icon = Icons.Outlined.Settings,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = "Advanced Settings",
                        subtitle = "Backup, stats, reset options",
                        onClick = onNavigateToAdvanced
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsMenuRow(
                        icon = Icons.Outlined.Info,
                        iconTint = MustDoColors.Info,
                        title = "About",
                        subtitle = "Version, rate, share, feedback",
                        onClick = onNavigateToAbout
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun SettingsScreenPreview() {
    MustDoTheme(darkTheme = true) {
        SettingsScreenContent()
    }
}

// ─── Theme Palette Picker ────────────────────────────────────────────────────

private data class ThemeOption(
    val key: String,
    val name: String,
    val subtitle: String,
    val primaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textColor: Color
)

private val themeOptions = listOf(
    ThemeOption(
        key = "ORANGE",
        name = "Premium Orange",
        subtitle = "Warm & Bold",
        primaryColor = Color(0xFFFF7A00),
        accentColor = Color(0xFFFFD54F),
        backgroundColor = Color(0xFF0D0D10),
        surfaceColor = Color(0xFF17181D),
        textColor = Color(0xFFF5F5F5)
    ),
    ThemeOption(
        key = "PURPLE",
        name = "Classic Purple",
        subtitle = "Calm & Premium",
        primaryColor = Color(0xFF7C6CFF),
        accentColor = Color(0xFF00D9FF),
        backgroundColor = Color(0xFF09090B),
        surfaceColor = Color(0xFF15151D),
        textColor = Color(0xFFF8FAFC)
    ),
    ThemeOption(
        key = "RED",
        name = "Nothing OS",
        subtitle = "Bold & Minimal",
        primaryColor = Color(0xFFFF3B30),
        accentColor = Color(0xFFFFD60A),
        backgroundColor = Color(0xFF0B0B0B),
        surfaceColor = Color(0xFF141414),
        textColor = Color(0xFFF2F2F7)
    ),
    ThemeOption(
        key = "BLUE",
        name = "Apple Inspired",
        subtitle = "Clean & Timeless",
        primaryColor = Color(0xFF007AFF),
        accentColor = Color(0xFF5856D6),
        backgroundColor = Color(0xFF0A0A0A),
        surfaceColor = Color(0xFF1C1C1E),
        textColor = Color(0xFFF2F2F7)
    ),
    ThemeOption(
        key = "MONOCHROME",
        name = "Monochrome",
        subtitle = "Minimal & Elegant",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF00E5BB),
        backgroundColor = Color(0xFF0B0B0C),
        surfaceColor = Color(0xFF17181D),
        textColor = Color(0xFFF5F5F5)
    ),
    ThemeOption(
        key = "EMERALD",
        name = "Emerald",
        subtitle = "Growth & Calm",
        primaryColor = Color(0xFF00C853),
        accentColor = Color(0xFF00E676),
        backgroundColor = Color(0xFF0A0D0B),
        surfaceColor = Color(0xFF141A16),
        textColor = Color(0xFFE8F5E9)
    ),
    ThemeOption(
        key = "CREAM",
        name = "Warm Cream",
        subtitle = "Soft & Inviting (Light)",
        primaryColor = Color(0xFF3A6FF7),
        accentColor = Color(0xFFFF8A3D),
        backgroundColor = Color(0xFFFAF7F2),
        surfaceColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF1F2937)
    ),
    ThemeOption(
        key = "MIDNIGHT",
        name = "Midnight Abyss",
        subtitle = "Amoled & Neon",
        primaryColor = Color(0xFF39FF14),
        accentColor = Color(0xFFFF00FF),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF080808),
        textColor = Color(0xFFFFFFFF)
    )
)

@Composable
private fun ThemePaletteSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(themeOptions, key = { it.key }) { option ->
            ThemePaletteCard(
                option = option,
                isSelected = selected == option.key,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(option.key) 
                }
            )
        }
    }
}

@Composable
private fun ThemePaletteCard(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) option.primaryColor else Color.Transparent,
        animationSpec = tween(300),
        label = "border"
    )

    Card(
        modifier = Modifier
            .width(140.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = option.surfaceColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color swatch row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                option.primaryColor,
                                option.accentColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Color dot previews
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    option.primaryColor,
                    option.accentColor,
                    option.backgroundColor
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Theme name
            Text(
                text = option.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = option.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle
            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = option.textColor.copy(alpha = 0.5f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
