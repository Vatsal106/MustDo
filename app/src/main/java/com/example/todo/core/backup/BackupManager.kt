package com.example.todo.core.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.todo.core.datastore.UserPreferencesManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: UserPreferencesManager,
    private val backupEngine: BackupEngine,
    private val db: com.example.todo.core.database.AppDatabase
) {
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun refreshBackupHistory() {
        refreshTrigger.tryEmit(Unit)
    }

    fun getBackupHistoryFlow(): Flow<List<BackupInfo>> = callbackFlow {
        // Emit initial list
        trySend(getBackupHistory())

        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folderName)

        val hasFilePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        var observer: android.os.FileObserver? = null

        if (hasFilePermission) {
            if (!dir.exists()) {
                try {
                    dir.mkdirs()
                } catch (e: Exception) {
                    Log.e("BackupManager", "Failed to create directory for observer", e)
                }
            }

            if (dir.exists()) {
                val onChange: (String?) -> Unit = { path ->
                    if (path == null || !path.startsWith(".")) {
                        launch { trySend(getBackupHistory()) }
                    }
                }

                observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    object : android.os.FileObserver(dir, CREATE or DELETE or MOVED_TO or MOVED_FROM or CLOSE_WRITE) {
                        override fun onEvent(event: Int, path: String?) {
                            onChange(path)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    object : android.os.FileObserver(dir.absolutePath, CREATE or DELETE or MOVED_TO or MOVED_FROM or CLOSE_WRITE) {
                        override fun onEvent(event: Int, path: String?) {
                            onChange(path)
                        }
                    }
                }

                try {
                    observer.startWatching()
                    Log.d("BackupManager", "Started watching backup directory: ${dir.absolutePath}")
                } catch (e: Exception) {
                    Log.e("BackupManager", "Failed to start FileObserver", e)
                }
            }
        }

        val triggerJob = launch {
            refreshTrigger.collect {
                trySend(getBackupHistory())
            }
        }

        awaitClose {
            triggerJob.cancel()
            try {
                observer?.stopWatching()
                Log.d("BackupManager", "Stopped watching backup directory")
            } catch (e: Exception) {
                Log.e("BackupManager", "Error stopping FileObserver", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    private val gson = Gson()
    private val folderName = "MustDo/Backups"

    suspend fun performAutoBackup(isManual: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val tasks = db.taskDao().getAllTasks()
        if (tasks.isEmpty() && !isManual) {
            Log.d("BackupManager", "Skipping auto backup: Database has no tasks")
            return@withContext true
        }
        val time = System.currentTimeMillis()
        try {
            val dateStr = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault()).format(Date(time))
            val fileName = "backup_$dateStr.zip"
            
            val success = writeBackupToSharedDirectory(fileName, "application/zip") { outputStream ->
                backupEngine.exportToZip(outputStream)
            }
            if (success) {
                rotateBackups()
                preferencesManager.setBackupStatus(time, true, null)
                refreshBackupHistory()
            } else {
                preferencesManager.setBackupStatus(time, false, "Failed to write backup file to directory")
            }
            success
        } catch (e: Exception) {
            Log.e("BackupManager", "Auto backup failed", e)
            preferencesManager.setBackupStatus(time, false, e.message ?: "Unknown error")
            false
        }
    }

    suspend fun getBackupHistory(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<BackupInfo>()
        try {
            val hasFilePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folderName)
            if (hasFilePermission && dir.exists()) {
                dir.listFiles { _, name -> (name.endsWith(".json") || name.endsWith(".zip")) && !name.startsWith(".") }?.forEach { file ->
                    try {
                        val json = if (file.name.endsWith(".zip")) {
                            var jsonText: String? = null
                            file.inputStream().use { stream ->
                                java.util.zip.ZipInputStream(stream).use { zis ->
                                    var entry = zis.nextEntry
                                    while (entry != null) {
                                        if (entry.name == "backup.json") {
                                            jsonText = zis.bufferedReader().readText()
                                            break
                                        }
                                        zis.closeEntry()
                                        entry = zis.nextEntry
                                    }
                                }
                            }
                            jsonText ?: throw Exception("backup.json not found in zip")
                        } else {
                            file.readText()
                        }
                        val info = getBackupInfoFromJson(json, file.name, file.absolutePath, file.length())
                        if (info != null) {
                            list.add(info)
                        }
                    } catch (e: Exception) {
                        Log.e("BackupManager", "Error reading backup file ${file.name}", e)
                    }
                }
            }

            // If list is empty (e.g. Android 13+ where direct file API on Downloads is blocked or permission is not granted),
            // fallback to MediaStore to query owned files
            if (list.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_ADDED
                )
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("%$folderName%")
                
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        if (name.startsWith(".")) continue
                        val size = cursor.getLong(sizeColumn)
                        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        
                        try {
                            resolver.openInputStream(uri)?.use { stream ->
                                val json = if (name.endsWith(".zip")) {
                                    var jsonText: String? = null
                                    java.util.zip.ZipInputStream(stream).use { zis ->
                                        var entry = zis.nextEntry
                                        while (entry != null) {
                                            if (entry.name == "backup.json") {
                                                jsonText = zis.bufferedReader().readText()
                                                break
                                            }
                                            zis.closeEntry()
                                            entry = zis.nextEntry
                                        }
                                    }
                                    jsonText ?: throw Exception("backup.json not found in zip")
                                } else {
                                    stream.bufferedReader().readText()
                                }
                                val info = getBackupInfoFromJson(json, name, uri.toString(), size)
                                if (info != null) {
                                    list.add(info)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BackupManager", "Error reading backup file $name", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error fetching backup history", e)
        }
        
        // Sort history: newest first
        list.sortedByDescending { it.createdAt }
    }

    suspend fun rotateBackups() = withContext(Dispatchers.IO) {
        try {
            val history = getBackupHistory()
            if (history.size > 7) {
                // Delete older files beyond the latest 7
                val filesToDelete = history.subList(7, history.size)
                filesToDelete.forEach { info ->
                    try {
                        val path = info.filePath
                        if (path.startsWith("content://")) {
                            context.contentResolver.delete(Uri.parse(path), null, null)
                            Log.d("BackupManager", "Deleted content URI: $path")
                        } else {
                            val file = File(path)
                            if (file.exists()) {
                                val deleted = file.delete()
                                Log.d("BackupManager", "Deleted file path directly: $path, success: $deleted")
                                if (!deleted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    deleteFileFromMediaStore(path)
                                }
                            } else {
                                Log.w("BackupManager", "File path did not exist: $path")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    deleteFileFromMediaStore(path)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BackupManager", "Failed to delete backup during rotation: ${info.fileName}", e)
                    }
                }
                refreshBackupHistory()
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Backup rotation error", e)
        }
    }

    private fun deleteFileFromMediaStore(absolutePath: String) {
        try {
            val resolver = context.contentResolver
            val file = File(absolutePath)
            val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.MediaColumns.DATA} = ?"
            val selectionArgs = arrayOf(file.absolutePath)
            resolver.delete(uri, selection, selectionArgs)
            Log.d("BackupManager", "Deleted via MediaStore fallback query: $absolutePath")
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to delete from MediaStore: $absolutePath", e)
        }
    }

    suspend fun getBackupInfoFromUri(uri: Uri): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            var jsonText: String? = null
            var size = 0L
            var fileName = "Imported Backup File"
            
            val isContent = uri.scheme == "content"
            
            if (isContent) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                        if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex) ?: "Imported Backup File"
                        }
                    }
                }
            } else {
                val path = uri.path ?: uri.toString()
                val cleanPath = if (path.startsWith("file://")) {
                    Uri.parse(path).path ?: path.substring(7)
                } else {
                    path
                }
                val file = File(cleanPath)
                if (file.exists()) {
                    size = file.length()
                    fileName = file.name
                }
            }

            // Helper to open input stream
            val openStream = {
                if (isContent) {
                    context.contentResolver.openInputStream(uri)
                } else {
                    val path = uri.path ?: uri.toString()
                    val cleanPath = if (path.startsWith("file://")) {
                        Uri.parse(path).path ?: path.substring(7)
                    } else {
                        path
                    }
                    File(cleanPath).inputStream()
                }
            }

            // Try opening as ZIP first
            try {
                openStream()?.use { stream ->
                    java.util.zip.ZipInputStream(stream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "backup.json") {
                                jsonText = zis.bufferedReader().readText()
                                break
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("BackupManager", "Not a zip file, trying raw json: ${e.message}")
            }

            // Fallback to raw JSON reading if zip extraction failed or was skipped
            if (jsonText == null) {
                openStream()?.use { stream ->
                    jsonText = stream.bufferedReader().readText()
                }
            }

            if (jsonText != null) {
                if (size == 0L) {
                    size = jsonText!!.toByteArray(Charsets.UTF_8).size.toLong()
                }
                getBackupInfoFromJson(jsonText!!, fileName, uri.toString(), size)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to parse info from Uri: $uri", e)
            null
        }
    }

    fun getBackupInfoFromJson(json: String, name: String, path: String, size: Long): BackupInfo? {
        return try {
            val element = JsonParser.parseString(json)
            if (!element.isJsonObject) return null
            val obj = element.asJsonObject
            
            val version = obj.get("version")?.asInt ?: 0
            val appVersion = obj.get("appVersion")?.asString ?: "1.0.0"
            val createdAt = obj.get("createdAt")?.asLong ?: 0L
            
            val taskCount = obj.getAsJsonArray("tasks")?.size() ?: 0
            val categoryCount = obj.getAsJsonArray("categories")?.size() ?: 0
            val subTaskCount = obj.getAsJsonArray("subTasks")?.size() ?: 0
            val focusSessionCount = obj.getAsJsonArray("focusSessions")?.size() ?: 0
            val achievementCount = obj.getAsJsonArray("achievements")?.size() ?: 0
            val reminderCount = obj.getAsJsonArray("reminders")?.size() ?: 0

            // Run simple validation structure check
            val validation = BackupValidator.validate(json)

            BackupInfo(
                fileName = name,
                filePath = path,
                fileSizeBytes = size,
                createdAt = createdAt,
                backupVersion = version,
                appVersion = appVersion,
                taskCount = taskCount,
                categoryCount = categoryCount,
                subTaskCount = subTaskCount,
                focusSessionCount = focusSessionCount,
                achievementCount = achievementCount,
                reminderCount = reminderCount,
                isValid = validation.isValid,
                validationMessage = validation.message
            )
        } catch (e: Exception) {
            BackupInfo(
                fileName = name,
                filePath = path,
                fileSizeBytes = size,
                createdAt = 0,
                backupVersion = 0,
                appVersion = "Unknown",
                taskCount = 0,
                categoryCount = 0,
                subTaskCount = 0,
                focusSessionCount = 0,
                achievementCount = 0,
                reminderCount = 0,
                isValid = false,
                validationMessage = "Corrupt JSON: ${e.message}"
            )
        }
    }

    private suspend fun writeBackupToSharedDirectory(
        fileName: String,
        mimeType: String,
        writeAction: suspend (java.io.OutputStream) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/$folderName")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        writeAction(outputStream)
                    }
                    true
                } else {
                    false
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folderName)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.outputStream().use { outputStream ->
                    writeAction(outputStream)
                }
                true
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error writing to shared directory", e)
            false
        }
    }
}
