package com.example.todo.features.tasks.presentation

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.common.util.capitalizeFirstLetter
import com.example.todo.core.database.entity.*
import com.example.todo.features.settings.domain.CategoryRepository
import com.example.todo.features.tasks.domain.TaskRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class AddEditTaskUiState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val priority: String = Priority.MEDIUM.name,
    val categoryId: String? = null,
    val dueDateMillis: Long? = null,
    val dueTimeMillis: Long? = null,
    val tags: String = "",
    val notes: String = "",
    val reminderTimeMillis: Long? = null,
    val estimatedMinutes: Int? = null,
    val recurrence: String = Recurrence.NONE.name,
    val autoReschedule: Boolean = false,
    val status: String = TaskStatus.PENDING.name,
    val dependencyTaskId: String? = null,
    val subTasks: List<SubTaskEntity> = emptyList(),
    val reminders: List<TaskReminderEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val availableTasks: List<TaskEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,

    // Productivity Upgrade Fields
    val contacts: List<ContactPayload> = emptyList(),
    val attachments: List<AttachmentPayload> = emptyList(),
    val voiceNotes: List<VoiceNotePayload> = emptyList(),
    val locations: List<LocationPayload> = emptyList(),
    val noteBlocks: List<TaskNoteBlockEntity> = emptyList(),
    val activities: List<TaskActivityEntity> = emptyList(),
    val totalFocusMinutes: Int = 0,

    // Voice Note State
    val isRecording: Boolean = false,
    val recordingDurationMillis: Long = 0L,
    val currentlyPlayingFilePath: String? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMillis: Long = 0L,
    val playbackDurationMillis: Long = 0L
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: com.example.todo.features.tasks.domain.ReminderRepository,
    private val focusRepository: com.example.todo.features.focus.domain.FocusRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle["taskId"]

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var recordingStartTime = 0L
    private var mediaPlayer: MediaPlayer? = null

    init {
        loadCategories()
        loadAvailableTasks()
        if (taskId != null) {
            loadTask(taskId)
        }
    }

    private fun loadAvailableTasks() {
        viewModelScope.launch {
            taskRepository.observeAllActiveTasks().collect { tasks ->
                val currentTaskId = _uiState.value.id
                _uiState.update { state -> 
                    state.copy(availableTasks = tasks.filter { it.id != currentTaskId })
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.observeAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            val subTasks = taskRepository.getSubTasksByTaskId(taskId)
            val reminders = reminderRepository.getRemindersForTask(taskId)

            val resources = taskRepository.getTaskResources(taskId)
            val noteBlocks = taskRepository.getTaskNoteBlocks(taskId)
            val activities = taskRepository.getTaskActivities(taskId)

            val gson = Gson()
            val contacts = resources.filter { it.resourceType == "CONTACT" }.mapNotNull {
                try { gson.fromJson(it.payloadJson, ContactPayload::class.java) } catch (e: Exception) { null }
            }
            val attachments = resources.filter { it.resourceType == "ATTACHMENT" }.mapNotNull {
                try { gson.fromJson(it.payloadJson, AttachmentPayload::class.java) } catch (e: Exception) { null }
            }
            val voiceNotes = resources.filter { it.resourceType == "VOICE_NOTE" }.mapNotNull {
                try { gson.fromJson(it.payloadJson, VoiceNotePayload::class.java) } catch (e: Exception) { null }
            }
            val locations = resources.filter { it.resourceType == "LOCATION" }.mapNotNull {
                try { gson.fromJson(it.payloadJson, LocationPayload::class.java) } catch (e: Exception) { null }
            }

            _uiState.update {
                it.copy(
                    id = task.id,
                    title = task.title,
                    description = task.description,
                    priority = task.priority,
                    categoryId = task.categoryId,
                    dueDateMillis = task.dueDateMillis,
                    dueTimeMillis = task.dueTimeMillis,
                    tags = task.tags,
                    notes = task.notes,
                    reminderTimeMillis = task.reminderTimeMillis,
                    estimatedMinutes = task.estimatedMinutes,
                    recurrence = task.recurrence,
                    autoReschedule = task.autoReschedule,
                    status = task.status,
                    dependencyTaskId = task.dependencyTaskId,
                    subTasks = subTasks,
                    reminders = reminders,
                    contacts = contacts,
                    attachments = attachments,
                    voiceNotes = voiceNotes,
                    locations = locations,
                    noteBlocks = noteBlocks,
                    activities = activities,
                    isEditing = true,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            focusRepository.observeFocusMinutesForTask(taskId).collect { minutes ->
                _uiState.update { it.copy(totalFocusMinutes = minutes ?: 0) }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title.capitalizeFirstLetter()) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc.capitalizeFirstLetter()) } }
    fun updatePriority(priority: String) { _uiState.update { it.copy(priority = priority) } }
    fun updateCategory(categoryId: String?) { _uiState.update { it.copy(categoryId = categoryId) } }
    fun updateDueDate(millis: Long?) { _uiState.update { it.copy(dueDateMillis = millis) } }
    fun updateDueTime(millis: Long?) { _uiState.update { it.copy(dueTimeMillis = millis) } }
    fun updateTags(tags: String) { _uiState.update { it.copy(tags = tags.capitalizeFirstLetter()) } }
    fun updateNotes(notes: String) { _uiState.update { it.copy(notes = notes.capitalizeFirstLetter()) } }
    fun updateEstimatedMinutes(minutes: Int?) { _uiState.update { it.copy(estimatedMinutes = minutes) } }
    fun updateRecurrence(recurrence: String) { _uiState.update { it.copy(recurrence = recurrence) } }
    fun updateAutoReschedule(autoReschedule: Boolean) { _uiState.update { it.copy(autoReschedule = autoReschedule) } }
    fun updateStatus(status: String) { _uiState.update { it.copy(status = status) } }
    fun updateDependencyTaskId(dependencyTaskId: String?) { _uiState.update { it.copy(dependencyTaskId = dependencyTaskId) } }
    
    fun skipTask() {
        val taskId = _uiState.value.id
        if (_uiState.value.isEditing) {
            viewModelScope.launch {
                taskRepository.skipTask(taskId)
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun addReminder(reminder: TaskReminderEntity) {
        _uiState.update { it.copy(reminders = it.reminders + reminder) }
    }

    fun removeReminder(reminderId: String) {
        _uiState.update { state ->
            state.copy(reminders = state.reminders.filter { it.id != reminderId })
        }
    }

    fun toggleReminder(reminderId: String) {
        _uiState.update { state ->
            state.copy(
                reminders = state.reminders.map {
                    if (it.id == reminderId) it.copy(isEnabled = !it.isEnabled) else it
                }
            )
        }
    }

    fun addSubTask(title: String) {
        val subTask = SubTaskEntity(
            taskId = _uiState.value.id,
            title = title.capitalizeFirstLetter(),
            orderIndex = _uiState.value.subTasks.size
        )
        _uiState.update { it.copy(subTasks = it.subTasks + subTask) }
    }

    fun toggleSubTask(subTaskId: String) {
        _uiState.update { state ->
            state.copy(
                subTasks = state.subTasks.map {
                    if (it.id == subTaskId) it.copy(isCompleted = !it.isCompleted) else it
                }
            )
        }
    }

    fun removeSubTask(subTaskId: String) {
        _uiState.update { state ->
            state.copy(subTasks = state.subTasks.filter { it.id != subTaskId })
        }
    }

    // CONTACT HANDLERS
    fun addContact(contact: ContactPayload) {
        _uiState.update { it.copy(contacts = it.contacts + contact) }
    }

    fun removeContact(contact: ContactPayload) {
        _uiState.update { state -> state.copy(contacts = state.contacts.filter { it != contact }) }
    }

    fun onContactPicked(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                var name = ""
                var phone = ""
                var email = ""
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        
                        name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                        phone = if (phoneIndex >= 0) cursor.getString(phoneIndex) ?: "" else ""
                        val contactId = if (idIndex >= 0) cursor.getString(idIndex) else ""
                        
                        if (contactId.isNotEmpty()) {
                            try {
                                context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                    arrayOf(contactId),
                                    null
                                )?.use { eCursor ->
                                    if (eCursor.moveToFirst()) {
                                        val emailIndex = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                                        if (emailIndex >= 0) {
                                            email = eCursor.getString(emailIndex) ?: ""
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                if (name.isNotEmpty()) {
                    addContact(ContactPayload(name = name, phone = phone, email = email))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ATTACHMENT HANDLERS
    private fun copyUriToLocalStorage(uri: Uri, context: Context): File? {
        val attachmentsDir = File(context.filesDir, "task_attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }
        var fileName = "attachment_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        val destFile = File(attachmentsDir, fileName)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun addAttachmentFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            val file = copyUriToLocalStorage(uri, context) ?: return@launch
            val size = file.length()
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val payload = AttachmentPayload(
                fileName = file.name,
                filePath = file.absolutePath,
                mimeType = mimeType,
                fileSize = size
            )
            _uiState.update { it.copy(attachments = it.attachments + payload) }
        }
    }

    fun removeAttachment(attachment: AttachmentPayload) {
        _uiState.update { state -> state.copy(attachments = state.attachments.filter { it != attachment }) }
        try {
            val file = File(attachment.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun renameAttachment(attachment: AttachmentPayload, newName: String) {
        _uiState.update { state ->
            state.copy(attachments = state.attachments.map {
                if (it == attachment) {
                    val oldFile = File(it.filePath)
                    val newFile = File(oldFile.parentFile, newName)
                    if (oldFile.renameTo(newFile)) {
                        it.copy(fileName = newName, filePath = newFile.absolutePath)
                    } else {
                        it
                    }
                } else it
            })
        }
    }

    // VOICE NOTE HANDLERS
    fun startRecording(context: Context) {
        val voiceDir = File(context.filesDir, "voice_notes")
        if (!voiceDir.exists()) {
            voiceDir.mkdirs()
        }
        val audioFile = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            recordingStartTime = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isRecording = true,
                    recordingDurationMillis = 0L
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(context: Context) {
        val recorder = mediaRecorder ?: return
        try {
            recorder.stop()
            recorder.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        val duration = System.currentTimeMillis() - recordingStartTime
        _uiState.update { it.copy(isRecording = false) }

        val voiceDir = File(context.filesDir, "voice_notes")
        val lastFile = voiceDir.listFiles()?.filter { it.name.startsWith("voice_") }?.maxByOrNull { it.lastModified() }
        if (lastFile != null && lastFile.exists()) {
            val payload = VoiceNotePayload(
                filePath = lastFile.absolutePath,
                duration = duration,
                transcription = "Voice Recording"
            )
            _uiState.update { it.copy(voiceNotes = it.voiceNotes + payload) }
        }
    }

    fun playVoiceNote(filePath: String) {
        viewModelScope.launch {
            if (_uiState.value.currentlyPlayingFilePath == filePath && _uiState.value.isPlaying) {
                mediaPlayer?.pause()
                _uiState.update { it.copy(isPlaying = false) }
            } else if (_uiState.value.currentlyPlayingFilePath == filePath && !_uiState.value.isPlaying) {
                mediaPlayer?.start()
                _uiState.update { it.copy(isPlaying = true) }
                observePlaybackPosition()
            } else {
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    android.widget.Toast.makeText(context, "Audio file not found on device", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(filePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            _uiState.update { it.copy(isPlaying = false, currentlyPlayingFilePath = null, playbackPositionMillis = 0L) }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            currentlyPlayingFilePath = filePath,
                            isPlaying = true,
                            playbackDurationMillis = mediaPlayer?.duration?.toLong() ?: 0L,
                            playbackPositionMillis = 0L
                        )
                    }
                    observePlaybackPosition()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Error playing audio file", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observePlaybackPosition() {
        viewModelScope.launch {
            while (mediaPlayer != null && _uiState.value.isPlaying && _uiState.value.currentlyPlayingFilePath != null) {
                val currentPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                _uiState.update { it.copy(playbackPositionMillis = currentPos) }
                delay(200)
            }
        }
    }

    fun seekVoiceNote(positionMillis: Long) {
        mediaPlayer?.seekTo(positionMillis.toInt())
        _uiState.update { it.copy(playbackPositionMillis = positionMillis) }
    }

    fun stopVoiceNotePlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _uiState.update { it.copy(isPlaying = false, currentlyPlayingFilePath = null) }
    }

    fun removeVoiceNote(voiceNote: VoiceNotePayload) {
        if (_uiState.value.currentlyPlayingFilePath == voiceNote.filePath) {
            stopVoiceNotePlayback()
        }
        _uiState.update { state -> state.copy(voiceNotes = state.voiceNotes.filter { it != voiceNote }) }
        try {
            val file = File(voiceNote.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // LOCATION HANDLERS
    fun addLocation(location: LocationPayload) {
        _uiState.update { it.copy(locations = it.locations + location) }
    }

    fun removeLocation(location: LocationPayload) {
        _uiState.update { state -> state.copy(locations = state.locations.filter { it != location }) }
    }

    // NOTE BLOCK HANDLERS
    fun addNoteBlock(blockType: String) {
        val newBlock = TaskNoteBlockEntity(
            id = UUID.randomUUID().toString(),
            taskId = _uiState.value.id,
            blockType = blockType,
            content = "",
            position = _uiState.value.noteBlocks.size,
            createdAt = System.currentTimeMillis()
        )
        _uiState.update { it.copy(noteBlocks = it.noteBlocks + newBlock) }
    }

    fun updateNoteBlockContent(blockId: String, content: String) {
        _uiState.update { state ->
            state.copy(
                noteBlocks = state.noteBlocks.map {
                    if (it.id == blockId) it.copy(content = content) else it
                }
            )
        }
    }

    fun removeNoteBlock(blockId: String) {
        _uiState.update { state ->
            state.copy(
                noteBlocks = state.noteBlocks.filter { it.id != blockId }
                    .mapIndexed { index, block -> block.copy(position = index) }
            )
        }
    }

    fun moveNoteBlock(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.noteBlocks.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                state.copy(noteBlocks = list.mapIndexed { index, block -> block.copy(position = index) })
            } else state
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            try {
                val task = TaskEntity(
                    id = state.id,
                    title = state.title.trim(),
                    description = state.description.trim(),
                    priority = state.priority,
                    categoryId = state.categoryId,
                    dueDateMillis = state.dueDateMillis,
                    dueTimeMillis = state.dueTimeMillis,
                    tags = state.tags.trim(),
                    notes = state.notes.trim(),
                    reminderTimeMillis = state.reminderTimeMillis,
                    estimatedMinutes = state.estimatedMinutes,
                    recurrence = state.recurrence,
                    autoReschedule = state.autoReschedule,
                    status = state.status,
                    dependencyTaskId = state.dependencyTaskId,
                    createdDateMillis = if (state.isEditing) {
                        taskRepository.getTaskById(state.id)?.createdDateMillis ?: System.currentTimeMillis()
                    } else System.currentTimeMillis()
                )

                // 1. Save Task Entity first to avoid foreign key violations for other tables
                if (state.isEditing) {
                    taskRepository.updateTask(task)
                } else {
                    taskRepository.insertTask(task)
                }

                // 2. Log resource activities if editing
                if (state.isEditing) {
                    val oldResources = taskRepository.getTaskResources(task.id)
                    val oldNoteBlocks = taskRepository.getTaskNoteBlocks(task.id)

                    if (oldNoteBlocks.size != state.noteBlocks.size) {
                        taskRepository.insertTaskActivity(
                            TaskActivityEntity(
                                taskId = task.id,
                                activityType = "EDIT_NOTE_BLOCK",
                                details = "Modified note blocks (Total: ${state.noteBlocks.size})"
                            )
                        )
                    }

                    val oldContactsCount = oldResources.filter { it.resourceType == "CONTACT" }.size
                    if (oldContactsCount != state.contacts.size) {
                        taskRepository.insertTaskActivity(
                            TaskActivityEntity(
                                taskId = task.id,
                                activityType = "EDIT_RESOURCE",
                                details = "Updated contacts (Total: ${state.contacts.size})"
                            )
                        )
                    }

                    val oldAttachmentsCount = oldResources.filter { it.resourceType == "ATTACHMENT" }.size
                    if (oldAttachmentsCount != state.attachments.size) {
                        taskRepository.insertTaskActivity(
                            TaskActivityEntity(
                                taskId = task.id,
                                activityType = "EDIT_RESOURCE",
                                details = "Updated attachments (Total: ${state.attachments.size})"
                            )
                        )
                    }

                    val oldVoiceNotesCount = oldResources.filter { it.resourceType == "VOICE_NOTE" }.size
                    if (oldVoiceNotesCount != state.voiceNotes.size) {
                        taskRepository.insertTaskActivity(
                            TaskActivityEntity(
                                taskId = task.id,
                                activityType = "EDIT_RESOURCE",
                                details = "Updated voice notes (Total: ${state.voiceNotes.size})"
                            )
                        )
                    }

                    val oldLocationsCount = oldResources.filter { it.resourceType == "LOCATION" }.size
                    if (oldLocationsCount != state.locations.size) {
                        taskRepository.insertTaskActivity(
                            TaskActivityEntity(
                                taskId = task.id,
                                activityType = "EDIT_RESOURCE",
                                details = "Updated locations (Total: ${state.locations.size})"
                            )
                        )
                    }
                } else {
                    if (state.contacts.isNotEmpty()) {
                        taskRepository.insertTaskActivity(TaskActivityEntity(taskId = task.id, activityType = "ADD_RESOURCE", details = "Added ${state.contacts.size} contacts"))
                    }
                    if (state.attachments.isNotEmpty()) {
                        taskRepository.insertTaskActivity(TaskActivityEntity(taskId = task.id, activityType = "ADD_RESOURCE", details = "Added ${state.attachments.size} attachments"))
                    }
                    if (state.voiceNotes.isNotEmpty()) {
                        taskRepository.insertTaskActivity(TaskActivityEntity(taskId = task.id, activityType = "ADD_RESOURCE", details = "Added ${state.voiceNotes.size} voice notes"))
                    }
                    if (state.locations.isNotEmpty()) {
                        taskRepository.insertTaskActivity(TaskActivityEntity(taskId = task.id, activityType = "ADD_RESOURCE", details = "Added ${state.locations.size} locations"))
                    }
                }

                // 3. Save subtasks
                state.subTasks.forEach { subTask ->
                    taskRepository.insertSubTask(subTask.copy(taskId = task.id))
                }

                // 4. Save and schedule reminders
                reminderRepository.deleteRemindersForTask(task.id)
                reminderRepository.insertAllReminders(state.reminders.map { it.copy(taskId = task.id) })

                // 5. Save note blocks
                taskRepository.deleteTaskNoteBlocksByTaskId(task.id)
                state.noteBlocks.forEachIndexed { index, block ->
                    taskRepository.insertTaskNoteBlock(block.copy(taskId = task.id, position = index))
                }

                // 6. Save task resources
                taskRepository.deleteTaskResourcesByTaskId(task.id)
                val gson = Gson()
                state.contacts.forEach { contact ->
                    taskRepository.insertTaskResource(
                        TaskResourceEntity(
                            taskId = task.id,
                            resourceType = "CONTACT",
                            payloadJson = gson.toJson(contact)
                        )
                    )
                }
                state.attachments.forEach { attachment ->
                    taskRepository.insertTaskResource(
                        TaskResourceEntity(
                            taskId = task.id,
                            resourceType = "ATTACHMENT",
                            payloadJson = gson.toJson(attachment)
                        )
                    )
                }
                state.voiceNotes.forEach { voice ->
                    taskRepository.insertTaskResource(
                        TaskResourceEntity(
                            taskId = task.id,
                            resourceType = "VOICE_NOTE",
                            payloadJson = gson.toJson(voice)
                        )
                    )
                }
                state.locations.forEach { location ->
                    taskRepository.insertTaskResource(
                        TaskResourceEntity(
                            taskId = task.id,
                            resourceType = "LOCATION",
                            payloadJson = gson.toJson(location)
                        )
                    )
                }

                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
