package com.example.todo.core.backup

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.todo.core.database.AppDatabase
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val repository: BackupRepository
) {
    private val gson = Gson()

    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun exportToString(): String = withContext(Dispatchers.IO) {
        val appVersion = getAppVersion()
        val payload = repository.getBackupPayload(appVersion)
        
        // Convert to standard JSON to calculate checksum
        val tempJson = gson.toJson(payload)
        val checksum = BackupValidator.calculateChecksum(tempJson)
        
        val finalPayload = payload.copy(checksum = checksum)
        
        // Return pretty-printed formatting for manual export, or standard for auto
        gson.newBuilder().setPrettyPrinting().create().toJson(finalPayload)
    }

    private fun openInputStreamForPath(path: String): java.io.InputStream? {
        return if (path.startsWith("content://")) {
            context.contentResolver.openInputStream(android.net.Uri.parse(path))
        } else {
            val cleanPath = if (path.startsWith("file://")) {
                android.net.Uri.parse(path).path ?: path.substring(7)
            } else {
                path
            }
            File(cleanPath).inputStream()
        }
    }

    suspend fun restoreFromFilePath(filePath: String): BackupValidator.ValidationResult = withContext(Dispatchers.IO) {
        val isZip = try {
            if (filePath.startsWith("content://")) {
                val mimeType = context.contentResolver.getType(android.net.Uri.parse(filePath))
                if (mimeType == "application/zip" || mimeType == "application/x-zip-compressed") {
                    true
                } else {
                    openInputStreamForPath(filePath)?.use { stream ->
                        val header = ByteArray(4)
                        val read = stream.read(header)
                        read == 4 &&
                                header[0] == 0x50.toByte() &&
                                header[1] == 0x4B.toByte() &&
                                header[2] == 0x03.toByte() &&
                                header[3] == 0x04.toByte()
                    } ?: false
                }
            } else {
                val cleanPath = if (filePath.startsWith("file://")) {
                    android.net.Uri.parse(filePath).path ?: filePath.substring(7)
                } else {
                    filePath
                }
                val file = File(cleanPath)
                file.name.endsWith(".zip", ignoreCase = true) || file.inputStream().use { stream ->
                    val header = ByteArray(4)
                    val read = stream.read(header)
                    read == 4 &&
                            header[0] == 0x50.toByte() &&
                            header[1] == 0x4B.toByte() &&
                            header[2] == 0x03.toByte() &&
                            header[3] == 0x04.toByte()
                } ?: false
            }
        } catch (e: Exception) {
            filePath.endsWith(".zip", ignoreCase = true)
        }

        if (isZip) {
            try {
                openInputStreamForPath(filePath)?.use { stream ->
                    restoreFromZip(stream)
                } ?: BackupValidator.ValidationResult(false, "Could not open stream for ZIP backup file")
            } catch (e: Exception) {
                BackupValidator.ValidationResult(false, "Failed to read ZIP backup file: ${e.message}")
            }
        } else {
            val json = try {
                openInputStreamForPath(filePath)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw Exception("Could not open stream")
            } catch (e: Exception) {
                return@withContext BackupValidator.ValidationResult(false, "Failed to read backup file: ${e.message}")
            }
            restoreFromString(json)
        }
    }

    suspend fun restoreFromString(backupJson: String): BackupValidator.ValidationResult = withContext(Dispatchers.IO) {
        // 1. Validate JSON structure, checksum, unique constraints, and foreign keys
        val validation = BackupValidator.validate(backupJson)
        if (!validation.isValid) {
            return@withContext validation
        }

        // 2. Parse payload and run version migrations
        val payload = try {
            val rawPayload = gson.fromJson(backupJson, BackupPayload::class.java)
            BackupMigrationManager.migrate(rawPayload)
        } catch (e: Exception) {
            return@withContext BackupValidator.ValidationResult(false, "Migration/Parsing failed: ${e.message}")
        }

        // 3. Create a physical emergency backup of current data on disk if there is existing data
        val emergencyBackupFile = File(context.cacheDir, "emergency_backup.json")
        var emergencyBackupCreated = false
        val currentTasks = db.taskDao().getAllTasks()
        if (currentTasks.isNotEmpty()) {
            try {
                val currentDataJson = exportToString()
                emergencyBackupFile.writeText(currentDataJson)
                emergencyBackupCreated = true
            } catch (e: Exception) {
                Log.e("BackupEngine", "Failed to create emergency backup file", e)
                return@withContext BackupValidator.ValidationResult(false, "Failed to create emergency backup before restore: ${e.message}")
            }
        }

        // 4. Perform database transaction and preferences update
        try {
            db.withTransaction {
                repository.restoreBackupPayload(payload)
            }
            // Verification check of restored data
            val verifyTasks = db.taskDao().getAllTasks()
            val verifyCategories = db.categoryDao().getAllCategories()
            
            // Integrity checks: no duplicate task IDs, categories check
            val taskIds = verifyTasks.map { it.id }.toSet()
            if (taskIds.size != verifyTasks.size) {
                throw IllegalStateException("Restored database failed integrity check: duplicate task IDs")
            }
            
            val categoryIds = verifyCategories.map { it.id }.toSet()
            verifyTasks.forEach { task ->
                if (task.categoryId != null && !categoryIds.contains(task.categoryId)) {
                    throw IllegalStateException("Restored database failed integrity check: task references missing category")
                }
            }

            BackupValidator.ValidationResult(true, "Restore successful")
        } catch (e: Exception) {
            Log.e("BackupEngine", "Restore failed, attempting recovery rollback from emergency file", e)
            if (emergencyBackupCreated && emergencyBackupFile.exists()) {
                try {
                    val emergencyJson = emergencyBackupFile.readText()
                    val emergencyPayload = gson.fromJson(emergencyJson, BackupPayload::class.java)
                    db.withTransaction {
                        repository.restoreBackupPayload(emergencyPayload)
                    }
                    Log.d("BackupEngine", "Recovery rollback completed successfully")
                } catch (rollbackEx: Exception) {
                    Log.e("BackupEngine", "Fatal error: recovery rollback failed", rollbackEx)
                }
            }
            BackupValidator.ValidationResult(false, "Restore failed and rolled back: ${e.message}")
        } finally {
            if (emergencyBackupFile.exists()) {
                emergencyBackupFile.delete()
            }
        }
    }

    suspend fun exportToZip(outputStream: java.io.OutputStream) = withContext(Dispatchers.IO) {
        ZipOutputStream(outputStream).use { zos ->
            val json = exportToString()
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            val attachmentsDir = File(context.filesDir, "task_attachments")
            if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
                attachmentsDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        zos.putNextEntry(ZipEntry("attachments/${file.name}"))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            val voiceDir = File(context.filesDir, "voice_notes")
            if (voiceDir.exists() && voiceDir.isDirectory) {
                voiceDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        zos.putNextEntry(ZipEntry("voice/${file.name}"))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    suspend fun restoreFromZip(inputStream: java.io.InputStream): BackupValidator.ValidationResult = withContext(Dispatchers.IO) {
        var backupJson: String? = null
        val tempDir = File(context.cacheDir, "extracted_backup_temp")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "backup.json") {
                        backupJson = zis.bufferedReader().readText()
                    } else if (name.startsWith("attachments/") && !entry.isDirectory) {
                        val fileName = name.substringAfter("attachments/")
                        val file = File(tempDir, "attachments/$fileName")
                        file.parentFile?.mkdirs()
                        file.outputStream().use { zis.copyTo(it) }
                    } else if (name.startsWith("voice/") && !entry.isDirectory) {
                        val fileName = name.substringAfter("voice/")
                        val file = File(tempDir, "voice/$fileName")
                        file.parentFile?.mkdirs()
                        file.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (backupJson == null) {
                return@withContext BackupValidator.ValidationResult(false, "Invalid backup zip: missing backup.json")
            }

            val dbRestoreResult = restoreFromString(backupJson!!)
            if (!dbRestoreResult.isValid) {
                return@withContext dbRestoreResult
            }

            val attachmentsDir = File(context.filesDir, "task_attachments")
            if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
            val tempAttachments = File(tempDir, "attachments")
            if (tempAttachments.exists()) {
                tempAttachments.listFiles()?.forEach { file ->
                    val dest = File(attachmentsDir, file.name)
                    file.copyTo(dest, overwrite = true)
                }
            }

            val voiceDir = File(context.filesDir, "voice_notes")
            if (!voiceDir.exists()) voiceDir.mkdirs()
            val tempVoice = File(tempDir, "voice")
            if (tempVoice.exists()) {
                tempVoice.listFiles()?.forEach { file ->
                    val dest = File(voiceDir, file.name)
                    file.copyTo(dest, overwrite = true)
                }
            }

            BackupValidator.ValidationResult(true, "Restore from ZIP successful")
        } catch (e: Exception) {
            Log.e("BackupEngine", "ZIP restore failed", e)
            BackupValidator.ValidationResult(false, "ZIP restore failed: ${e.message}")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
