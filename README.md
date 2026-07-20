# MustDo - Premium Android Productivity App

MustDo is a state-of-the-art task management and productivity application built with Jetpack Compose, Kotlin, Room Database, and Hilt. It blends robust task scheduling with a gamified progression system to keep you motivated and structured.

---

## Key Features & Functionalities

### 1. Smart Task Management
*   **Flexible Task Creation & Editing:** Add titles, descriptions, due dates, and recurrence rules (Daily, Weekly, Monthly).
*   **Priority Matrix:** Label tasks as **Low**, **Medium**, **High**, or **Urgent** with visual color indicators.
*   **Subtask Checklists:** Break down larger tasks into smaller checkable milestones within the details view.
*   **Categorization:** Organize tasks using standard System Categories (Work, Personal, Health, Learning, Finance) or custom categories with hex color selectors.
*   **Archiving:** Swipe tasks right/left to archive or complete them, keeping your active list clean.
*   **Quick Action Menu:** Long-press any task card in the list to trigger a custom bottom sheet menu allowing instant status resets, moving categories, toggling archives, viewing/updating notes, or deleting.

### 2. Date-Wise Structured Agenda
*   **Chronological Lists:** Tasks are grouped automatically into clean sections separated by dates (`dd/MM/yy`), with a dedicated "No Due Date" section at the bottom.
*   **Interactive Search & Filters:** Search dynamically through titles and tags, or filter tasks instantly by status (Pending, In Progress, Completed, Archived) or Category.

### 3. Gamification & Progression (XP & Achievements)
*   **Leveling System:** Earn Experience Points (XP) by completing tasks. Higher priority tasks award more XP.
*   **Daily Streak Tracking:** Keep your productivity active. The app tracks consecutively active days and displays fire streaks.
*   **Unlockable Achievements:** Complete conditions (e.g., first task completed, reaching Level 5, maintaining a 30-day streak) to unlock badges with custom animations.
*   **Productivity Dashboard:** View your XP progress bar, current level, today's completion rates, and motivational quotes.

### 4. Interactive Focus Timer
*   **Focus Sessions:** Dedicated timer to block out distractions and track focused work time.
*   **Analytics:** Visual stats showing total focused minutes, historical trends, and average daily durations.

### 5. Smart Notifications & Daily Alarms
*   **Periodic Check-ins:** The app triggers notification alarms twice a day (once in the morning and once in the afternoon) outlining your due tasks.
*   **Actionable Notifications:** Complete tasks or snooze them directly from the Android status bar drawer without opening the application.

### 6. Local Backup & Automated Restore
*   **Background Backups:** Powered by `WorkManager`, the app serializes all tasks, subtasks, categories, focus sessions, and achievements into a JSON payload every **2 days**.
*   **Secure Mobile Storage:** Saves backups cleanly to `Downloads/MustdoBackup/mustdo_backup.json` to survive uninstallation.
*   **Reinstall Autodetect:** On a fresh reinstall, the app automatically checks the directory and offers a **single-tap restore dialog** to bring back your entire database history safely.

---

## Technical Stack & Architecture

*   **UI Framework:** Jetpack Compose (Material 3 components and premium dark mode aesthetics).
*   **Database:** Room ORM with SQLite (reactive flow queries, safe foreign-key cascade actions, and transaction-safe migrations).
*   **Dependency Injection:** Hilt (Dagger) for modular decoupled architecture.
*   **Async Operations:** Coroutines & Flows for seamless background database queries and reactive UI updates.
*   **Background Work:** WorkManager for scheduling period-based backups and check-ins.
