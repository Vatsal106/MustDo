package com.example.todo.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.todo.features.calendar.presentation.CalendarScreen
import com.example.todo.features.dashboard.presentation.DashboardScreen
import com.example.todo.features.focus.presentation.FocusScreen
import com.example.todo.features.settings.presentation.*
import com.example.todo.features.tasks.presentation.AddEditTaskScreen
import com.example.todo.features.tasks.presentation.TasksScreen

@Composable
fun MustDoNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            fadeIn(animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        }
    ) {
        // Bottom Nav Destinations
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.AddEditTask.createRoute(taskId))
                },
                onAddTask = {
                    navController.navigate(Screen.AddEditTask.createRoute())
                },
                onNavigateToReports = {
                    navController.navigate(Screen.MonthlyReport.route)
                }
            )
        }

        composable(Screen.Tasks.route) {
            TasksScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.AddEditTask.createRoute(taskId))
                },
                onAddTask = {
                    navController.navigate(Screen.AddEditTask.createRoute())
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.AddEditTask.createRoute(taskId))
                },
                onAddTask = {
                    navController.navigate(Screen.AddEditTask.createRoute())
                }
            )
        }

        composable(
            route = Screen.Focus.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            FocusScreen(initialTaskId = taskId)
        }

        // Settings Hub
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToAppearance = {
                    navController.navigate(Screen.AppearanceSettings.route)
                },
                onNavigateToTimerFocus = {
                    navController.navigate(Screen.TimerFocusSettings.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.CategoriesSettings.route)
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.AchievementsSettings.route)
                },
                onNavigateToAdvanced = {
                    navController.navigate(Screen.AdvancedSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.AboutScreen.route)
                }
            )
        }

        // Settings Sub-Screens (all with slide-in animation)
        val settingsEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        }

        val settingsExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }

        composable(
            route = Screen.NotificationSettings.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AppearanceSettings.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            AppearanceSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TimerFocusSettings.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            TimerFocusSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CategoriesSettings.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            CategoriesSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AchievementsSettings.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            AchievementsSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AdvancedSettings.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            AdvancedSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AboutScreen.route,
            enterTransition = { settingsEnterTransition() },
            exitTransition = { settingsExitTransition() }
        ) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Detail Screens
        composable(
            route = "add_edit_task?taskId={taskId}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mustdo://task/{taskId}" },
                navDeepLink { uriPattern = "mustdo://task?taskId={taskId}" }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            AddEditTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFocus = { taskId ->
                    navController.navigate(Screen.Focus.createRoute(taskId))
                }
            )
        }

        composable(Screen.MonthlyReport.route) {
            com.example.todo.features.reports.presentation.MonthlyReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
