package com.example.todo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Tasks : Screen("tasks")
    data object Calendar : Screen("calendar")
    data object Focus : Screen("focus?taskId={taskId}") {
        fun createRoute(taskId: String? = null) =
            if (taskId != null) "focus?taskId=$taskId" else "focus"
    }
    data object Settings : Screen("settings")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    data object AddEditTask : Screen("add_edit_task?taskId={taskId}") {
        fun createRoute(taskId: String? = null) =
            if (taskId != null) "add_edit_task?taskId=$taskId"
            else "add_edit_task"
    }
    data object MonthlyReport : Screen("monthly_report")
    data object CategoryManagement : Screen("category_management")
    data object NotificationSettings : Screen("notification_settings")
    data object AppearanceSettings : Screen("appearance_settings")
    data object TimerFocusSettings : Screen("timer_focus_settings")
    data object CategoriesSettings : Screen("categories_settings")
    data object AchievementsSettings : Screen("achievements_settings")
    data object AdvancedSettings : Screen("advanced_settings")
    data object AboutScreen : Screen("about_screen")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Dashboard.route,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    BottomNavItem(
        route = Screen.Tasks.route,
        label = "Tasks",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    ),
    BottomNavItem(
        route = Screen.Calendar.route,
        label = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    BottomNavItem(
        route = "focus", // Base route for matching
        label = "Focus",
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
