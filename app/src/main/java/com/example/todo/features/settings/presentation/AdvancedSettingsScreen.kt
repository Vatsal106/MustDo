package com.example.todo.features.settings.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme

@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBackupHistory by remember { mutableStateOf(false) }
    var backupErrorDetails by remember { mutableStateOf<String?>(null) }

    // File picker launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackup(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadImportPreview(it) }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadBackupHistory()
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.backupMessage) {
        state.backupMessage?.let { msg ->
            if (msg.contains("failed", ignoreCase = true) || msg.contains("error", ignoreCase = true)) {
                backupErrorDetails = msg
            } else {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearMessage()
        }
    }

    SettingsSubPageScaffold(
        title = "Advanced Settings",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {


            // --- Backup & Restore ---
            item {
                SectionHeader(
                    title = "BACKUP & RESTORE",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Health status card
            item {
                SharedSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Backup Health Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val success = state.lastBackupSuccess
                        val lastTime = state.lastBackupTime
                        val error = state.lastBackupError

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (success) MustDoColors.Success else MustDoColors.Accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (success) "Last Backup Successful" else "Backup Status: Failed / Not Run",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (error != null) {
                            Text(
                                text = "⚠ Error: $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MustDoColors.Accent
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Last Backup:", style = MaterialTheme.typography.bodySmall)
                            Text(formatTimestamp(lastTime), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stored Backups:", style = MaterialTheme.typography.bodySmall)
                            Text("${state.backupHistory.size} / 7", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Backup Storage Size:", style = MaterialTheme.typography.bodySmall)
                            Text(formatSize(state.backupStorageUsage), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Auto Backups Status:", style = MaterialTheme.typography.bodySmall)
                            Text("Daily Backups Enabled", style = MaterialTheme.typography.bodySmall, color = MustDoColors.Success, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Backup & Restore Action Buttons
            item {
                SharedSettingsCard {
                    SharedSettingsButton(
                        label = "Create Local Backup Now",
                        icon = Icons.Outlined.Backup,
                        onClick = { viewModel.createManualBackup() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsButton(
                        label = "Export Backup File (SAF)",
                        icon = Icons.Outlined.Upload,
                        onClick = { exportLauncher.launch("mustdo_backup.zip") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsButton(
                        label = "Import Backup File (SAF)",
                        icon = Icons.Outlined.Download,
                        onClick = { importLauncher.launch(arrayOf("application/zip", "application/json")) }
                    )
                }
            }

            // Backup history section
            if (state.backupHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBackupHistory = !showBackupHistory }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Backup History (Downloads/MustDo/Backups)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (showBackupHistory) "Hide History" else "Show History",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MustDoColors.Primary
                        )
                    }
                }

                if (showBackupHistory) {
                    items(state.backupHistory) { backup ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clickable {
                                    val uri = if (backup.filePath.startsWith("content://")) {
                                        Uri.parse(backup.filePath)
                                    } else {
                                        Uri.fromFile(java.io.File(backup.filePath))
                                    }
                                    viewModel.loadImportPreview(uri)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MustDoColors.Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = backup.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Size: ${formatSize(backup.fileSizeBytes)} • Version: ${backup.backupVersion} • App: v${backup.appVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Stats ---
            item {
                SectionHeader(
                    title = "STATS",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                SharedSettingsCard {
                    SharedSettingsInfo(label = "Total XP", value = "${state.totalXp}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsInfo(label = "Level", value = "${state.currentLevel}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsInfo(label = "Current Streak", value = "${state.currentStreak} days")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsInfo(label = "Longest Streak", value = "${state.longestStreak} days")
                }
            }

            // --- Danger Zone ---
            item {
                SectionHeader(
                    title = "DANGER ZONE",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MustDoColors.Accent.copy(alpha = 0.08f)
                    )
                ) {
                    SharedSettingsButton(
                        label = "Reset All Data",
                        icon = Icons.Outlined.DeleteForever,
                        tint = MustDoColors.Accent,
                        onClick = { viewModel.showResetConfirm() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // Reset confirmation dialog
    if (state.showResetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetConfirm() },
            title = { Text("Reset All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all tasks, focus sessions, categories, achievements, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetAllData() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MustDoColors.Accent)
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetConfirm() }) { Text("Cancel") }
            }
        )
    }

    // Backup preview dialog
    state.backupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImportPreview() },
            title = { Text("Restore Backup?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to restore the backup: ${preview.fileName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("• Version: ${preview.backupVersion} (App v${preview.appVersion})", style = MaterialTheme.typography.bodySmall)
                            Text("• Tasks: ${preview.taskCount}", style = MaterialTheme.typography.bodySmall)
                            Text("• Subtasks: ${preview.subTaskCount}", style = MaterialTheme.typography.bodySmall)
                            Text("• Categories: ${preview.categoryCount}", style = MaterialTheme.typography.bodySmall)
                            Text("• Focus Sessions: ${preview.focusSessionCount}", style = MaterialTheme.typography.bodySmall)
                            Text("• Achievements: ${preview.achievementCount}", style = MaterialTheme.typography.bodySmall)
                            Text("• Reminders: ${preview.reminderCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠ WARNING: Restoring will overwrite all current data. An emergency backup will be saved automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MustDoColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmImportRestore(context) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MustDoColors.Primary)
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImportPreview() }) { Text("Cancel") }
            }
        )
    }

    if (backupErrorDetails != null) {
        AlertDialog(
            onDismissRequest = { backupErrorDetails = null },
            title = { Text("Restore Failed", fontWeight = FontWeight.Bold) },
            text = { Text(backupErrorDetails ?: "") },
            confirmButton = {
                TextButton(
                    onClick = { backupErrorDetails = null }
                ) { Text("OK", color = MustDoColors.Primary) }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun AdvancedSettingsPreview() {
    MustDoTheme(darkTheme = true) {
        SettingsSubPageScaffold(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    SectionHeader(
                        title = "BACKUP & RESTORE",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    SharedSettingsCard {
                        SharedSettingsButton(
                            label = "Create Local Backup Now",
                            icon = Icons.Outlined.Backup,
                            onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SharedSettingsButton(
                            label = "Export Backup File (SAF)",
                            icon = Icons.Outlined.Upload,
                            onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SharedSettingsButton(
                            label = "Import Backup File (SAF)",
                            icon = Icons.Outlined.Download,
                            onClick = {}
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "STATS",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    SharedSettingsCard {
                        SharedSettingsInfo(label = "Total XP", value = "1250")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SharedSettingsInfo(label = "Level", value = "12")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SharedSettingsInfo(label = "Current Streak", value = "5 days")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SharedSettingsInfo(label = "Longest Streak", value = "14 days")
                    }
                }
                item {
                    SectionHeader(
                        title = "DANGER ZONE",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MustDoColors.Accent.copy(alpha = 0.08f)
                        )
                    ) {
                        SharedSettingsButton(
                            label = "Reset All Data",
                            icon = Icons.Outlined.DeleteForever,
                            tint = MustDoColors.Accent,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
