# ToDoApp Backup System Documentation

This document explains the backup and restore system of the ToDoApp, describing the data structures, serialization details, compatibility mappings, and functions for both the **Manual File Picker Backup** and the **Automatic Local Backup**.

---

## 1. Overview of the System
The application implements two parallel backup architectures:
1. **Manual Backup & Restore (Settings)**: Allows users to export and import backups to/from a file location of their choice using the Android Storage Access Framework (SAF) URI system.
2. **Automatic Local Backup (Background)**: Detects, writes, and rotates backups automatically inside the `Downloads/MustDo/Backups` directory using ZIP files (`backup_yyyy_MM_dd_HH_mm.zip`) containing the Room database payload + attachments + voice notes.

---

## 2. Data Structures & Serialization Mappings

To ensure backward compatibility across all builds (including release builds obfuscated by R8/Proguard), all model fields are annotated with `@SerializedName` using alternate obfuscated names.

### A. Manual Backup Data (`BackupData`)
Defined in [SettingsViewModel.kt](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/features/settings/presentation/SettingsViewModel.kt), this model is used during manual JSON export and import.

| Field Name | Type | Serialized Name | Obfuscated Alternate Name |
| :--- | :--- | :--- | :--- |
| `tasks` | `List<TaskEntity>` | `"tasks"` | `"a"` |
| `categories` | `List<CategoryEntity>` | `"categories"` | `"b"` |
| `focusSessions` | `List<FocusSessionEntity>` | `"focusSessions"` | `"c"` |
| `achievements` | `List<AchievementEntity>` | `"achievements"` | `"d"` |

---

### B. Automatic Backup Payload (`BackupPayload`)
Defined in [BackupPayload.kt](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/core/backup/BackupPayload.kt), this is the file structure used by [BackupManager.kt](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/core/backup/BackupManager.kt) for background auto-restores.

| Field Name | Type | Serialized Name |
| :--- | :--- | :--- |
| `timestamp` | `Long` | `"timestamp"` |
| `tasks` | `List<TaskEntity>` | `"tasks"` |
| `subTasks` | `List<SubTaskEntity>` | `"subTasks"` |
| `categories` | `List<CategoryEntity>` | `"categories"` |
| `focusSessions` | `List<FocusSessionEntity>` | `"focusSessions"` |
| `achievements` | `List<AchievementEntity>` | `"achievements"` |

---

### C. Entity Field Mapping Compatibility Table
Each database entity includes alternate names mapping to the obfuscated R8 outputs (`a`, `b`, `c`, etc.):

#### 1. [TaskEntity](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/core/database/entity/TaskEntity.kt)
* `id` &rarr; `@SerializedName("id", alternate = ["a"])`
* `title` &rarr; `@SerializedName("title", alternate = ["b"])`
* `description` &rarr; `@SerializedName("description", alternate = ["c"])`
* `priority` &rarr; `@SerializedName("priority", alternate = ["d"])`
* `categoryId` &rarr; `@SerializedName("categoryId", alternate = ["e"])`
* `dueDateMillis` &rarr; `@SerializedName("dueDateMillis", alternate = ["f"])`
* `dueTimeMillis` &rarr; `@SerializedName("dueTimeMillis", alternate = ["g"])`
* `tags` &rarr; `@SerializedName("tags", alternate = ["h"])`
* `notes` &rarr; `@SerializedName("notes", alternate = ["i"])`
* `reminderTimeMillis` &rarr; `@SerializedName("reminderTimeMillis", alternate = ["j"])`
* `estimatedMinutes` &rarr; `@SerializedName("estimatedMinutes", alternate = ["k"])`
* `recurrence` &rarr; `@SerializedName("recurrence", alternate = ["l"])`
* `status` &rarr; `@SerializedName("status", alternate = ["m"])`
* `dependencyTaskId` &rarr; `@SerializedName("dependencyTaskId", alternate = ["n"])`
* `createdDateMillis` &rarr; `@SerializedName("createdDateMillis", alternate = ["o"])`
* `completedDateMillis` &rarr; `@SerializedName("completedDateMillis", alternate = ["p"])`

#### 2. [CategoryEntity](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/core/database/entity/CategoryEntity.kt)
* `id` &rarr; `@SerializedName("id", alternate = ["a"])`
* `name` &rarr; `@SerializedName("name", alternate = ["b"])`
* `colorHex` &rarr; `@SerializedName("colorHex", alternate = ["c"])`
* `iconName` &rarr; `@SerializedName("iconName", alternate = ["d"])`
* `isSystem` &rarr; `@SerializedName("isSystem", alternate = ["e"])`
* `orderIndex` &rarr; `@SerializedName("orderIndex", alternate = ["f"])`

#### 3. [FocusSessionEntity](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/core/database/entity/FocusSessionEntity.kt)
* `id` &rarr; `@SerializedName("id", alternate = ["a"])`
* `taskId` &rarr; `@SerializedName("taskId", alternate = ["b"])`
* `sessionType` &rarr; `@SerializedName("sessionType", alternate = ["c"])`
* `startTimeMillis` &rarr; `@SerializedName("startTimeMillis", alternate = ["d"])`
* `durationMinutes` &rarr; `@SerializedName("durationMinutes", alternate = ["e"])`
* `isCompleted` &rarr; `@SerializedName("isCompleted", alternate = ["f"])`

#### 4. [AchievementEntity](file:///d:/vatsal_IT/Android/ToDoApp/app/src/main/java/com/example/todo/core/database/entity/AchievementEntity.kt)
* `id` &rarr; `@SerializedName("id", alternate = ["a"])`
* `title` &rarr; `@SerializedName("title", alternate = ["b"])`
* `description` &rarr; `@SerializedName("description", alternate = ["c"])`
* `iconName` &rarr; `@SerializedName("iconName", alternate = ["d"])`
* `conditionType` &rarr; `@SerializedName("conditionType", alternate = ["e"])`
* `conditionValue` &rarr; `@SerializedName("conditionValue", alternate = ["f"])`
* `isUnlocked` &rarr; `@SerializedName("isUnlocked", alternate = ["g"])`
* `unlockedTimeMillis` &rarr; `@SerializedName("unlockedTimeMillis", alternate = ["h"])`

---

## 3. Detailed Import & Export Logic

### A. Manual Backup Functions (`SettingsViewModel`)

#### Exporting Manual Backup (`exportBackup`)
1. Fetches current records for tasks, categories, focus sessions, and achievements from database repositories.
2. Packages them into a `BackupData` object.
3. Converts the object to JSON using `Gson().toJson(backup)`.
4. Writes the JSON payload directly into the OutputStream resolved from the user's selected file URI.
5. Updates UI state to display `"Backup exported successfully!"` or `"Export failed: <message>"`.

#### Importing Manual Backup (`importBackup`)
1. Opens an InputStream for the chosen file URI and parses the JSON text content.
2. Deserializes the JSON into a `BackupData` instance.
3. Clears all existing task, focus session, and achievement tables to prevent duplicate IDs.
4. Inserts the imported tasks, categories, focus sessions, and achievements into their respective repositories.
5. Updates UI state to display `"Backup imported successfully!"` or `"Import failed: <message>"`.

---

### B. Automatic Local Backup Functions (`BackupManager`)

#### Exporting Local Backup (`performAutoBackup`)
1. Verifies database task density (skips backup if tasks list is empty).
2. Generates a ZIP archive file named `backup_yyyy_MM_dd_HH_mm.zip`.
3. Writes the ZIP package inside the `Downloads/MustDo/Backups/` shared directory using `writeBackupToSharedDirectory`:
   - Uses `MediaStore.Downloads` for Android Q (API 29) & higher.
   - Falls back to raw file streams for older versions.
4. Leverages `backupEngine.exportToZip(outputStream)` to package the JSON payload, task attachments, and voice notes into the archive.
5. Rotates auto-backups, retaining the latest 7 ZIP files and deleting older ones.

#### Importing Local Backup & History Parsing
1. Reads local ZIP files or legacy JSON files from `Downloads/MustDo/Backups/`.
2. If parsing a ZIP file, opens a `ZipInputStream` to locate and extract the internal metadata inside `backup.json`.
3. Validates structural checks and schema version via `BackupValidator` and `BackupMigrationManager`.
4. Performs database transactions on `AppDatabase` executing removals and restorations sequentially to satisfy foreign key constraints.
5. Copies media attachments and voice notes back to internal directories (`task_attachments/` and `voice_notes/`).
