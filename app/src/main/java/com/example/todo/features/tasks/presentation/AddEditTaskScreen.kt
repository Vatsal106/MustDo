package com.example.todo.features.tasks.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.*
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFocus: (String) -> Unit = {},
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newSubTaskText by remember { mutableStateOf("") }
    var showAddReminderSheet by remember { mutableStateOf(false) }
    var showCustomReminderDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var showManualContactDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showRenameAttachmentDialog by remember { mutableStateOf<AttachmentPayload?>(null) }
    var renameValue by remember { mutableStateOf("") }

    // Contact Picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.onContactPicked(uri, context)
            }
        }
    }

    // File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addAttachmentFromUri(it, context) }
    }

    // Audio recording permission launcher
    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording(context)
        } else {
            Toast.makeText(context, "Audio recording permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "Edit Task" else "New Task",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTask() },
                        enabled = state.title.isNotBlank()
                    ) {
                        Text(
                            "Save",
                            color = if (state.title.isNotBlank()) MustDoColors.Primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title *") },
                    placeholder = { Text("What needs to be done?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MustDoColors.Primary
                    )
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    placeholder = { Text("Add more details...") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MustDoColors.Primary
                    )
                )
            }

            // Priority
            item {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { priority ->
                        val color = when (priority) {
                            Priority.LOW -> MustDoColors.PriorityLow
                            Priority.MEDIUM -> MustDoColors.PriorityMedium
                            Priority.HIGH -> MustDoColors.PriorityHigh
                            Priority.URGENT -> MustDoColors.PriorityUrgent
                        }
                        val selected = state.priority == priority.name

                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updatePriority(priority.name) },
                            label = { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }
            }

            // Category
            item {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { viewModel.updateCategory(null) },
                        label = { Text("None") }
                    )
                    state.categories.forEach { category ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(category.colorHex))
                        } catch (e: Exception) {
                            MustDoColors.Primary
                        }
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.updateCategory(category.id) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }
            }

            // Due Date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MustDoColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (state.dueDateMillis != null)
                                    DateUtils.formatDate(state.dueDateMillis)
                                else "Set due date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.dueDateMillis != null)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (state.dueDateMillis != null) {
                        IconButton(onClick = { viewModel.updateDueDate(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear date")
                        }
                    }
                }
            }

            // Reminders Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔔 Reminders",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showAddReminderSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = MustDoColors.Primary)
                    }
                }
            }

            if (state.reminders.isEmpty()) {
                item {
                    Text(
                        text = "No reminders scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                itemsIndexed(
                    items = state.reminders,
                    key = { _, reminder -> reminder.id }
                ) { _, reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = if (reminder.isEnabled) MustDoColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = reminder.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Switch(
                                checked = reminder.isEnabled,
                                onCheckedChange = { viewModel.toggleReminder(reminder.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MustDoColors.Primary
                                ),
                                thumbContent = if (reminder.isEnabled) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    }
                                } else null
                            )
                            IconButton(onClick = { viewModel.removeReminder(reminder.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete reminder",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Estimated time
            item {
                OutlinedTextField(
                    value = state.estimatedMinutes?.toString() ?: "",
                    onValueChange = { value ->
                        viewModel.updateEstimatedMinutes(value.toIntOrNull())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Estimated Time (minutes)") },
                    placeholder = { Text("e.g. 30") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MustDoColors.Primary)
                )
            }

            // Recurrence
            item {
                Text(
                    text = "Recurrence",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Recurrence.entries.forEach { rec ->
                        FilterChip(
                            selected = state.recurrence == rec.name,
                            onClick = { viewModel.updateRecurrence(rec.name) },
                            label = {
                                Text(rec.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MustDoColors.Primary.copy(alpha = 0.15f),
                                selectedLabelColor = MustDoColors.Primary
                            )
                        )
                    }
                }
                
                AnimatedVisibility(visible = state.recurrence != Recurrence.NONE.name) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-reschedule from completion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "If enabled, the next due date is calculated from when you complete the task instead of the original due date.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.autoReschedule,
                                onCheckedChange = viewModel::updateAutoReschedule,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MustDoColors.Primary
                                )
                            )
                        }
                        
                        if (state.isEditing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { viewModel.skipTask() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skip This Occurrence")
                            }
                        }
                    }
                }
            }


            // Tags
            item {
                OutlinedTextField(
                    value = state.tags,
                    onValueChange = viewModel::updateTags,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tags") },
                    placeholder = { Text("work, urgent, meeting (comma-separated)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    leadingIcon = {
                        Icon(Icons.Outlined.Label, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MustDoColors.Primary)
                )
            }

            // Focus Time Integration
            if (state.isEditing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MustDoColors.Primary.copy(alpha = 0.1f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MustDoColors.Primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MustDoColors.Primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Focus Time",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (state.totalFocusMinutes > 0) 
                                            "${state.totalFocusMinutes} minutes logged" 
                                            else "No time logged yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { onNavigateToFocus(state.id) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MustDoColors.Primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Focus Now")
                            }
                        }
                    }
                }
            }

            // PRODUCTIVITY UPGRADE: COLLAPSIBLE RESOURCES SECTION
            item {
                var resourcesExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { resourcesExpanded = !resourcesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CardTravel, contentDescription = null, tint = MustDoColors.Primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Task Resources",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (resourcesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (resourcesExpanded) "Collapse" else "Expand"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Counts row: 👤 X  📎 Y  🎤 Z  📍 W
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = "Contacts", modifier = Modifier.size(16.dp), tint = MustDoColors.Primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.contacts.size}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attachments", modifier = Modifier.size(16.dp), tint = MustDoColors.Primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.attachments.size}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Notes", modifier = Modifier.size(16.dp), tint = MustDoColors.Primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.voiceNotes.size}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = "Locations", modifier = Modifier.size(16.dp), tint = MustDoColors.Primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.locations.size}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        AnimatedVisibility(visible = resourcesExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // 1. Contacts Subsection
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Contacts Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Row {
                                            IconButton(onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                                    contactPickerLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open contact picker", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Contacts, contentDescription = "Pick Contact", tint = MustDoColors.Primary)
                                            }
                                            IconButton(onClick = { showManualContactDialog = true }) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Contact Manually", tint = MustDoColors.Primary)
                                            }
                                        }
                                    }

                                    if (state.contacts.isEmpty()) {
                                        Text("No contacts linked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        state.contacts.forEach { contact ->
                                            Card(
                                                onClick = {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No dialer app found", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                enabled = contact.phone.isNotEmpty(),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(contact.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                        if (contact.company.isNotEmpty()) {
                                                            Text(contact.company, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        if (contact.phone.isNotEmpty()) {
                                                            Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        if (contact.email.isNotEmpty()) {
                                                            Text(contact.email, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (contact.phone.isNotEmpty()) {
                                                            IconButton(onClick = {
                                                                try {
                                                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "No app to make calls", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }) {
                                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = MustDoColors.Primary)
                                                            }
                                                        }
                                                        if (contact.email.isNotEmpty()) {
                                                            IconButton(onClick = {
                                                                try {
                                                                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")))
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "No app to send email", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }) {
                                                                Icon(Icons.Default.Email, contentDescription = "Email", tint = MustDoColors.Primary)
                                                            }
                                                        }
                                                        IconButton(onClick = { viewModel.removeContact(contact) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // 2. Attachments Subsection
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Media & Attachments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload File", tint = MustDoColors.Primary)
                                        }
                                    }

                                    if (state.attachments.isEmpty()) {
                                        Text("No files attached.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        state.attachments.forEach { filePayload ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MustDoColors.Primary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(filePayload.fileName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                        Text("${filePayload.fileSize / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(onClick = {
                                                             try {
                                                                 val file = File(filePayload.filePath)
                                                                 if (!file.exists()) {
                                                                     Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
                                                                     return@IconButton
                                                                 }
                                                                 val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                                 val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                     setDataAndType(uri, filePayload.mimeType)
                                                                     addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                 }
                                                                 context.startActivity(intent)
                                                             } catch (e: Exception) {
                                                                 Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
                                                             }
                                                         }) {
                                                            Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = MustDoColors.Primary)
                                                        }
                                                        IconButton(onClick = {
                                                            try {
                                                                val file = File(filePayload.filePath)
                                                                if (!file.exists()) {
                                                                    Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
                                                                    return@IconButton
                                                                }
                                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                                    type = filePayload.mimeType
                                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                }
                                                                context.startActivity(Intent.createChooser(intent, "Share Attachment"))
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }) {
                                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MustDoColors.Primary)
                                                        }
                                                        IconButton(onClick = {
                                                            renameValue = filePayload.fileName
                                                            showRenameAttachmentDialog = filePayload
                                                        }) {
                                                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MustDoColors.Primary)
                                                        }
                                                        IconButton(onClick = { viewModel.removeAttachment(filePayload) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // 3. Voice Notes Subsection
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Voice Recordings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (state.isRecording) {
                                            Button(
                                                onClick = { viewModel.stopRecording(context) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Stop")
                                            }
                                        } else {
                                            IconButton(onClick = {
                                                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }) {
                                                Icon(Icons.Default.Mic, contentDescription = "Record", tint = MustDoColors.Primary)
                                            }
                                        }
                                    }

                                    if (state.voiceNotes.isEmpty()) {
                                        Text("No voice notes recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        state.voiceNotes.forEach { voice ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            val isPlayingThis = state.currentlyPlayingFilePath == voice.filePath && state.isPlaying
                                                            IconButton(onClick = { viewModel.playVoiceNote(voice.filePath) }) {
                                                                Icon(
                                                                    imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                    contentDescription = if (isPlayingThis) "Pause" else "Play",
                                                                    tint = MustDoColors.Primary
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Audio Note", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                        }

                                                        Row {
                                                            Text(
                                                                text = "${(voice.duration / 1000) / 60}:${String.format("%02d", (voice.duration / 1000) % 60)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.align(Alignment.CenterVertically)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            IconButton(onClick = { viewModel.removeVoiceNote(voice) }) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }

                                                    if (state.currentlyPlayingFilePath == voice.filePath) {
                                                        com.example.todo.common.components.LiquidSlider(
                                                            value = state.playbackPositionMillis.toFloat(),
                                                            onValueChange = { viewModel.seekVoiceNote(it.toLong()) },
                                                            valueRange = 0f..state.playbackDurationMillis.toFloat()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // 4. Locations Subsection
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Locations Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { showAddLocationDialog = true }) {
                                            Icon(Icons.Default.AddLocation, contentDescription = "Add Location", tint = MustDoColors.Primary)
                                        }
                                    }

                                    if (state.locations.isEmpty()) {
                                        Text("No locations linked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        state.locations.forEach { location ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(location.locationName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                        if (location.address.isNotEmpty()) {
                                                            Text(location.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        if (location.locationLink.isNotEmpty()) {
                                                            Text(location.locationLink, style = MaterialTheme.typography.bodySmall, color = MustDoColors.Primary)
                                                        }
                                                    }
                                                    Row {
                                                        if (location.locationLink.isNotEmpty()) {
                                                            IconButton(onClick = {
                                                                try {
                                                                    val formattedUrl = if (!location.locationLink.startsWith("http://") && !location.locationLink.startsWith("https://")) {
                                                                        "https://" + location.locationLink
                                                                    } else location.locationLink
                                                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                                                                    context.startActivity(mapIntent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Cannot open location link", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }) {
                                                                Icon(Icons.Default.Link, contentDescription = "Open Location Link", tint = MustDoColors.Primary)
                                                            }
                                                        }
                                                        IconButton(onClick = { viewModel.removeLocation(location) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notes Editor
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes Summary") },
                    placeholder = { Text("Additional general notes...") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    leadingIcon = {
                        Icon(Icons.Outlined.Notes, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MustDoColors.Primary)
                )
            }

            // SMART NOTE BLOCKS EDITOR
            item {
                Text(
                    text = "📝 Smart Note Blocks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { viewModel.addNoteBlock("TEXT") }) { Text("+ Text") }
                    Button(onClick = { viewModel.addNoteBlock("CHECKLIST") }) { Text("+ Todo") }
                    Button(onClick = { viewModel.addNoteBlock("LINK") }) { Text("+ Link") }
                    Button(onClick = { viewModel.addNoteBlock("QUOTE") }) { Text("+ Quote") }
                    Button(onClick = { viewModel.addNoteBlock("DIVIDER") }) { Text("+ Divider") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.noteBlocks.forEachIndexed { index, block ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    when (block.blockType) {
                                        "TEXT" -> {
                                            OutlinedTextField(
                                                value = block.content,
                                                onValueChange = { viewModel.updateNoteBlockContent(block.id, it) },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("Type note text...") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent
                                                )
                                            )
                                        }
                                        "CHECKLIST" -> {
                                            var isChecked by remember { mutableStateOf(block.content.startsWith("[x] ")) }
                                            val textVal = if (isChecked) block.content.removePrefix("[x] ") else block.content.removePrefix("[ ] ")

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        isChecked = checked
                                                        val prefix = if (checked) "[x] " else "[ ] "
                                                        viewModel.updateNoteBlockContent(block.id, prefix + textVal)
                                                    }
                                                )
                                                OutlinedTextField(
                                                    value = textVal,
                                                    onValueChange = {
                                                        val prefix = if (isChecked) "[x] " else "[ ] "
                                                        viewModel.updateNoteBlockContent(block.id, prefix + it)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    placeholder = { Text("Todo item...") },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }
                                        "LINK" -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Link, contentDescription = null, tint = MustDoColors.Primary)
                                                OutlinedTextField(
                                                    value = block.content,
                                                    onValueChange = { viewModel.updateNoteBlockContent(block.id, it) },
                                                    modifier = Modifier.weight(1f),
                                                    placeholder = { Text("URL / Web link...") },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                                if (block.content.isNotEmpty()) {
                                                    IconButton(onClick = {
                                                        try {
                                                            val formattedUrl = if (!block.content.startsWith("http://") && !block.content.startsWith("https://")) {
                                                                "https://" + block.content
                                                            } else block.content
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Invalid web URL", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.OpenInNew, contentDescription = "Go", tint = MustDoColors.Primary)
                                                    }
                                                }
                                            }
                                        }
                                        "QUOTE" -> {
                                            Row {
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(56.dp)
                                                        .background(MustDoColors.Primary)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                OutlinedTextField(
                                                    value = block.content,
                                                    onValueChange = { viewModel.updateNoteBlockContent(block.id, it) },
                                                    modifier = Modifier.weight(1f),
                                                    placeholder = { Text("Quote...") },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }
                                        "DIVIDER" -> {
                                            HorizontalDivider(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.moveNoteBlock(index, index - 1) },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveNoteBlock(index, index + 1) },
                                        enabled = index < state.noteBlocks.size - 1
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.removeNoteBlock(block.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Block", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subtasks
            item {
                Text(
                    text = "Subtasks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(
                items = state.subTasks,
                key = { _, subTask -> subTask.id }
            ) { _, subTask ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = subTask.isCompleted,
                        onCheckedChange = { viewModel.toggleSubTask(subTask.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MustDoColors.Primary
                        )
                    )
                    Text(
                        text = subTask.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                        color = if (subTask.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.removeSubTask(subTask.id) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Add subtask input
            item {
                OutlinedTextField(
                    value = newSubTaskText,
                    onValueChange = { newSubTaskText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add subtask...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newSubTaskText.isNotBlank()) {
                                viewModel.addSubTask(newSubTaskText.trim())
                                newSubTaskText = ""
                            }
                        }
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MustDoColors.Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                )
            }

            // TIMELINE FEED
            if (state.activities.isNotEmpty()) {
                item {
                    Text(
                        text = "🕒 Activity Timeline",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.activities.forEachIndexed { idx, activity ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MustDoColors.Primary)
                                    )
                                    if (idx < state.activities.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(32.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = activity.details,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = DateUtils.formatDateTime(activity.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacer
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDueDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("Confirm", color = MustDoColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Add Reminder Bottom Sheet
    if (showAddReminderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddReminderSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val presets = listOf(
                    "At due time" to {
                        val trigger = state.dueDateMillis ?: System.currentTimeMillis()
                        TaskReminderEntity(
                            taskId = state.id,
                            reminderType = "EXACT_TIME",
                            triggerTimestamp = trigger,
                            label = "At due time"
                        )
                    },
                    "15 minutes before" to {
                        TaskReminderEntity(
                            taskId = state.id,
                            reminderType = "BEFORE_DUE_DATE",
                            triggerTimestamp = (state.dueDateMillis ?: System.currentTimeMillis()) - (15 * 60 * 1000L),
                            offsetMinutes = 15,
                            label = "15 min before"
                        )
                    },
                    "1 hour before" to {
                        TaskReminderEntity(
                            taskId = state.id,
                            reminderType = "BEFORE_DUE_DATE",
                            triggerTimestamp = (state.dueDateMillis ?: System.currentTimeMillis()) - (60 * 60 * 1000L),
                            offsetMinutes = 60,
                            label = "1 hour before"
                        )
                    },
                    "1 day before" to {
                        TaskReminderEntity(
                            taskId = state.id,
                            reminderType = "BEFORE_DUE_DATE",
                            triggerTimestamp = (state.dueDateMillis ?: System.currentTimeMillis()) - (24 * 60 * 60 * 1000L),
                            offsetMinutes = 1440,
                            label = "1 day before"
                        )
                    },
                    "Daily recurring at 9 AM" to {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        TaskReminderEntity(
                            taskId = state.id,
                            reminderType = "RECURRING",
                            triggerTimestamp = cal.timeInMillis,
                            repeatPattern = "DAILY",
                            label = "Daily at 9:00 AM"
                        )
                    },
                    "Every 30 minutes" to {
                        TaskReminderEntity(
                            taskId = state.id,
                            reminderType = "CUSTOM_INTERVAL",
                            triggerTimestamp = System.currentTimeMillis() + (30 * 60 * 1000L),
                            customIntervalMinutes = 30,
                            label = "Every 30 min until completed"
                        )
                    }
                )

                presets.forEach { (name, creator) ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addReminder(creator())
                                showAddReminderSheet = false
                            },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MustDoColors.Primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Custom Reminder Option
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddReminderSheet = false
                            showCustomReminderDialog = true
                        },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = MustDoColors.Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Custom reminder...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Custom Reminder Configuration Dialog
    if (showCustomReminderDialog) {
        var valueText by remember { mutableStateOf("15") }
        var selectedUnit by remember { mutableStateOf("MIN_BEFORE") }

        AlertDialog(
            onDismissRequest = { showCustomReminderDialog = false },
            title = { Text("Custom Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set custom offset before due date or a recurring interval:", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it.filter { c -> c.isDigit() } },
                        label = { Text("Value (Number)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Unit / Type", style = MaterialTheme.typography.labelMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "MIN_BEFORE" to "Minutes before",
                            "HOURS_BEFORE" to "Hours before",
                            "DAYS_BEFORE" to "Days before",
                            "INTERVAL" to "Every X minutes (Interval)"
                        ).forEach { (unit, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUnit = unit }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUnit == unit,
                                    onClick = { selectedUnit = unit }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = valueText.toIntOrNull()
                        if (num != null && num > 0) {
                            val reminder = when (selectedUnit) {
                                "MIN_BEFORE" -> {
                                    val offset = num * 60 * 1000L
                                    val trigger = (state.dueDateMillis ?: System.currentTimeMillis()) - offset
                                    com.example.todo.core.database.entity.TaskReminderEntity(
                                        taskId = state.id,
                                        reminderType = "BEFORE_DUE_DATE",
                                        triggerTimestamp = trigger,
                                        offsetMinutes = num,
                                        label = "$num min before"
                                    )
                                }
                                "HOURS_BEFORE" -> {
                                    val offset = num * 60 * 60 * 1000L
                                    val trigger = (state.dueDateMillis ?: System.currentTimeMillis()) - offset
                                    com.example.todo.core.database.entity.TaskReminderEntity(
                                        taskId = state.id,
                                        reminderType = "BEFORE_DUE_DATE",
                                        triggerTimestamp = trigger,
                                        offsetMinutes = num * 60,
                                        label = "$num hours before"
                                    )
                                }
                                "DAYS_BEFORE" -> {
                                    val offset = num * 24 * 60 * 60 * 1000L
                                    val trigger = (state.dueDateMillis ?: System.currentTimeMillis()) - offset
                                    com.example.todo.core.database.entity.TaskReminderEntity(
                                        taskId = state.id,
                                        reminderType = "BEFORE_DUE_DATE",
                                        triggerTimestamp = trigger,
                                        offsetMinutes = num * 1440,
                                        label = "$num days before"
                                    )
                                }
                                else -> {
                                    val trigger = System.currentTimeMillis() + (num * 60 * 1000L)
                                    com.example.todo.core.database.entity.TaskReminderEntity(
                                        taskId = state.id,
                                        reminderType = "CUSTOM_INTERVAL",
                                        triggerTimestamp = trigger,
                                        customIntervalMinutes = num,
                                        label = "Every $num min until completed"
                                    )
                                }
                            }
                            viewModel.addReminder(reminder)
                            showCustomReminderDialog = false
                        }
                    }
                ) { Text("Add", color = MustDoColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Manual Contact Dialog
    if (showManualContactDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualContactDialog = false },
            title = { Text("Add Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addContact(ContactPayload(name = name, phone = phone, email = email, company = company))
                            showManualContactDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showManualContactDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Location Dialog
    if (showAddLocationDialog) {
        var name by remember { mutableStateOf("") }
        var locationLink by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false },
            title = { Text("Add Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Location Name") })
                    OutlinedTextField(value = locationLink, onValueChange = { locationLink = it }, label = { Text("Location Link") })
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address (Optional)") })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addLocation(LocationPayload(locationName = name, locationLink = locationLink, address = address))
                            showAddLocationDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Attachment Dialog
    showRenameAttachmentDialog?.let { filePayload ->
        AlertDialog(
            onDismissRequest = { showRenameAttachmentDialog = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Filename") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameValue.isNotBlank()) {
                            viewModel.renameAttachment(filePayload, renameValue)
                            showRenameAttachmentDialog = null
                        }
                    },
                    enabled = renameValue.isNotBlank()
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameAttachmentDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun AddEditTaskScreenPreview() {
    MustDoTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Task",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("What needs to be done?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MustDoColors.Primary)
                ) {
                    Text("Save Task", color = Color.White)
                }
            }
        }
    }
}

