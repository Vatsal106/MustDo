package com.example.todo.features.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import java.util.Calendar
import com.example.todo.common.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var timePickerDialogTarget by remember { mutableStateOf<String?>(null) } // "quiet_start", "quiet_end", "daily_agenda", "weekly_review"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notification Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Master Toggle
            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SettingsSwitch(
                        label = "Enable Notifications",
                        icon = Icons.Outlined.Notifications,
                        checked = state.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled
                    )
                }
            }

            if (state.notificationsEnabled) {
                // --- Quiet Hours ---
                item {
                    SectionHeader(
                        title = "QUIET HOURS",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SettingsSwitch(
                            label = "Quiet Hours",
                            icon = Icons.Outlined.DoNotDisturbOn,
                            checked = state.quietHoursEnabled,
                            onCheckedChange = viewModel::setQuietHoursEnabled
                        )

                        if (state.quietHoursEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsClickableRow(
                                label = "Start Time",
                                icon = Icons.Outlined.AccessTime,
                                value = DateUtils.formatTime24To12(state.quietHoursStart),
                                onClick = { timePickerDialogTarget = "quiet_start" }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsClickableRow(
                                label = "End Time",
                                icon = Icons.Outlined.AccessTimeFilled,
                                value = DateUtils.formatTime24To12(state.quietHoursEnd),
                                onClick = { timePickerDialogTarget = "quiet_end" }
                            )
                        }
                    }
                }

                // --- Task Reminders ---
                item {
                    SectionHeader(
                        title = "TASK REMINDERS",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SettingsSwitch(
                            label = "Sound",
                            icon = Icons.Outlined.VolumeUp,
                            checked = state.notificationSoundEnabled,
                            onCheckedChange = viewModel::setNotificationSoundEnabled
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitch(
                            label = "Vibration",
                            icon = Icons.Outlined.Vibration,
                            checked = state.notificationVibrationEnabled,
                            onCheckedChange = viewModel::setNotificationVibrationEnabled
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitch(
                            label = "Group Notifications",
                            icon = Icons.Outlined.Layers,
                            checked = state.groupNotifications,
                            onCheckedChange = viewModel::setGroupNotifications
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSlider(
                            label = "Default Snooze",
                            icon = Icons.Outlined.Snooze,
                            value = state.defaultSnoozeMinutes,
                            range = 5f..60f,
                            suffix = "min",
                            onValueChange = { viewModel.setDefaultSnoozeMinutes(it.toInt()) }
                        )
                    }
                }

                // --- Daily Agenda ---
                item {
                    SectionHeader(
                        title = "DAILY AGENDA DIGEST",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SettingsSwitch(
                            label = "Enable Daily Agenda",
                            icon = Icons.Outlined.CalendarToday,
                            checked = state.dailyAgendaEnabled,
                            onCheckedChange = viewModel::setDailyAgendaEnabled
                        )

                        if (state.dailyAgendaEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsSwitch(
                                label = "Unified Notification",
                                icon = Icons.Outlined.Layers,
                                checked = state.dailyAgendaUnified,
                                onCheckedChange = viewModel::setDailyAgendaUnified
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsClickableRow(
                                label = "Morning Digest Time",
                                icon = Icons.Outlined.AccessTime,
                                value = DateUtils.formatTime24To12(state.dailyAgendaTime),
                                onClick = { timePickerDialogTarget = "daily_agenda" }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsClickableRow(
                                label = "Noon Digest Time",
                                icon = Icons.Outlined.AccessTime,
                                value = DateUtils.formatTime24To12(state.dailyAgendaTimeSecondary),
                                onClick = { timePickerDialogTarget = "daily_agenda_secondary" }
                            )
                        }
                    }
                }

                // --- Weekly Review ---
                item {
                    SectionHeader(
                        title = "WEEKLY REVIEW & PLANNING",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SettingsSwitch(
                            label = "Enable Weekly Planning",
                            icon = Icons.Outlined.AssignmentTurnedIn,
                            checked = state.weeklyReviewEnabled,
                            onCheckedChange = viewModel::setWeeklyReviewEnabled
                        )

                        if (state.weeklyReviewEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsDropdownRow(
                                label = "Day of week",
                                icon = Icons.Outlined.CalendarMonth,
                                value = state.weeklyReviewDay,
                                options = listOf(
                                    1 to "Sunday",
                                    2 to "Monday",
                                    3 to "Tuesday",
                                    4 to "Wednesday",
                                    5 to "Thursday",
                                    6 to "Friday",
                                    7 to "Saturday"
                                ),
                                onSelect = viewModel::setWeeklyReviewDay
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsClickableRow(
                                label = "Time to send",
                                icon = Icons.Outlined.AccessTime,
                                value = DateUtils.formatTime24To12(state.weeklyReviewTime),
                                onClick = { timePickerDialogTarget = "weekly_review" }
                            )
                        }
                    }
                }

                // --- Overdue Reminders ---
                item {
                    SectionHeader(
                        title = "OVERDUE REMINDERS",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SettingsSwitch(
                            label = "Overdue Alerts",
                            icon = Icons.Outlined.WarningAmber,
                            checked = state.overdueReminderEnabled,
                            onCheckedChange = viewModel::setOverdueReminderEnabled
                        )

                        if (state.overdueReminderEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsDropdownRow(
                                label = "Frequency",
                                icon = Icons.Outlined.Loop,
                                value = state.overdueReminderFrequency,
                                options = listOf(
                                    "HOURLY" to "Hourly",
                                    "TWICE_DAILY" to "Twice Daily",
                                    "DAILY" to "Daily"
                                ),
                                onSelect = viewModel::setOverdueReminderFrequency
                            )
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    timePickerDialogTarget?.let { target ->
        val currentTime = when (target) {
            "quiet_start" -> state.quietHoursStart
            "quiet_end" -> state.quietHoursEnd
            "daily_agenda" -> state.dailyAgendaTime
            "daily_agenda_secondary" -> state.dailyAgendaTimeSecondary
            "weekly_review" -> state.weeklyReviewTime
            else -> "08:00"
        }
        val parts = currentTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { timePickerDialogTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        when (target) {
                            "quiet_start" -> viewModel.setQuietHoursStart(formatted)
                            "quiet_end" -> viewModel.setQuietHoursEnd(formatted)
                            "daily_agenda" -> viewModel.setDailyAgendaTime(formatted)
                            "daily_agenda_secondary" -> viewModel.setDailyAgendaTimeSecondary(formatted)
                            "weekly_review" -> viewModel.setWeeklyReviewTime(formatted)
                        }
                        timePickerDialogTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MustDoColors.Primary)
                ) { 
                    Text("Confirm") 
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { timePickerDialogTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MustDoColors.Primary)
                ) { 
                    Text("Cancel") 
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = MustDoColors.Primary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        periodSelectorUnselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large,
        content = content
    )
}

@Composable
private fun SettingsSwitch(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MustDoColors.Primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MustDoColors.Primary)
        )
    }
}

@Composable
private fun SettingsClickableRow(
    label: String,
    icon: ImageVector,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MustDoColors.Primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MustDoColors.Primary
        )
    }
}

@Composable
private fun <T> SettingsDropdownRow(
    label: String,
    icon: ImageVector,
    value: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = options.firstOrNull { it.first == value }?.second ?: value.toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MustDoColors.Primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MustDoColors.Primary
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (option, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    icon: ImageVector,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MustDoColors.Primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "$value $suffix",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MustDoColors.Primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        com.example.todo.common.components.LiquidSlider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val minVal = range.start.toInt()
            val maxVal = range.endInclusive.toInt()
            val midVal = ((minVal + maxVal) / 2)
            
            Text(
                text = "$minVal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "$midVal $suffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "$maxVal $suffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun NotificationSettingsScreenPreview() {
    MustDoTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

