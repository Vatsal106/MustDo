package com.example.todo.features.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoTheme

@Composable
fun TimerFocusSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TimerFocusSettingsContent(
        pomodoroDuration = state.pomodoroDuration,
        breakDuration = state.breakDuration,
        deepWorkDuration = state.deepWorkDuration,
        onPomodoroDurationChange = { viewModel.setPomodoroDuration(it.toInt()) },
        onBreakDurationChange = { viewModel.setBreakDuration(it.toInt()) },
        onDeepWorkDurationChange = { viewModel.setDeepWorkDuration(it.toInt()) },
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun TimerFocusSettingsContent(
    pomodoroDuration: Int = 25,
    breakDuration: Int = 5,
    deepWorkDuration: Int = 90,
    onPomodoroDurationChange: (Float) -> Unit = {},
    onBreakDurationChange: (Float) -> Unit = {},
    onDeepWorkDurationChange: (Float) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    SettingsSubPageScaffold(
        title = "Timer & Focus",
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
                    title = "TIMER DURATIONS",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
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

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun TimerFocusSettingsPreview() {
    MustDoTheme(darkTheme = true) {
        TimerFocusSettingsContent()
    }
}
