package com.example.todo.features.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable

@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppearanceSettingsContent(
        themeMode = state.themeMode,
        amoledMode = state.amoledMode,
        useDynamicColor = state.useDynamicColor,
        accentColor = state.accentColor,
        onThemeModeChange = viewModel::setThemeMode,
        onAmoledModeChange = viewModel::setAmoledMode,
        onDynamicColorChange = viewModel::setUseDynamicColor,
        onAccentColorChange = viewModel::setAccentColor,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun AppearanceSettingsContent(
    themeMode: String = "SYSTEM",
    amoledMode: Boolean = false,
    useDynamicColor: Boolean = false,
    accentColor: String = "",
    onThemeModeChange: (String) -> Unit = {},
    onAmoledModeChange: (Boolean) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onAccentColorChange: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    SettingsSubPageScaffold(
        title = "Appearance",
        onNavigateBack = onNavigateBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                SectionHeader(
                    title = "THEME",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                SharedSettingsCard {
                    SharedSettingsRadioGroup(
                        label = "Theme",
                        icon = Icons.Outlined.DarkMode,
                        options = listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System"),
                        selected = themeMode,
                        onSelect = onThemeModeChange
                    )
                }
            }

            if (themeMode == "DARK" || themeMode == "SYSTEM") {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SharedSettingsCard {
                        SharedSettingsSwitch(
                            label = "AMOLED Dark Mode",
                            icon = Icons.Outlined.DarkMode,
                            checked = amoledMode,
                            onCheckedChange = onAmoledModeChange
                        )
                    }
                }
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SharedSettingsCard {
                        SharedSettingsSwitch(
                            label = "Dynamic Colors",
                            icon = Icons.Outlined.DarkMode,
                            checked = useDynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }
                }
            }

            if (!useDynamicColor) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "ACCENT COLOR",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    SharedSettingsCard {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(com.example.todo.common.theme.MustDoColors.Accents.size) { index ->
                                val color = com.example.todo.common.theme.MustDoColors.Accents[index]
                                val hexString = String.format("#%06X", (0xFFFFFF and color.value.toInt()))
                                val isSelected = accentColor == hexString
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { onAccentColorChange(hexString) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun AppearanceSettingsPreview() {
    MustDoTheme(darkTheme = true) {
        AppearanceSettingsContent()
    }
}
