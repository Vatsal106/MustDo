package com.example.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.navigation.MustDoNavHost
import com.example.todo.navigation.Screen
import com.example.todo.navigation.bottomNavItems
import com.example.todo.core.notification.InactivityNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.foundation.background

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: UserPreferencesManager

    private var lastSetColorTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Reset and schedule the 2-day inactivity notification
        InactivityNotificationWorker.schedule(this)

        enableEdgeToEdge()

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "SYSTEM")
            val amoledMode by preferencesManager.amoledMode.collectAsState(initial = false)
            val useDynamicColor by preferencesManager.useDynamicColor.collectAsState(initial = false)
            val accentColorHex by preferencesManager.accentColor.collectAsState(initial = "")
            val colorTheme by preferencesManager.colorTheme.collectAsState(initial = "ORANGE")

            // Update dynamic theme color palette
            MustDoColors.currentTheme = colorTheme

            SideEffect {
                lastSetColorTheme = colorTheme
            }

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val accentColor = if (accentColorHex.isNotEmpty()) {
                try {
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(accentColorHex))
                } catch (e: Exception) {
                    null
                }
            } else null

            MustDoTheme(
                darkTheme = isDark,
                isAmoled = amoledMode,
                useDynamicColor = useDynamicColor,
                accentColor = accentColor
            ) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    com.example.todo.core.presentation.SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    MustDoApp()
                }
            }
        }
    }

    private fun updateAppIcon(theme: String, showToast: Boolean) {
        // Manifest namespace is com.example.todo, NOT the applicationId (com.vk.mustdo)
        val aliases = mapOf(
            "ORANGE"     to android.content.ComponentName(packageName, "com.example.todo.MainActivityOrange"),
            "PURPLE"     to android.content.ComponentName(packageName, "com.example.todo.MainActivityPurple"),
            "RED"        to android.content.ComponentName(packageName, "com.example.todo.MainActivityRed"),
            "BLUE"       to android.content.ComponentName(packageName, "com.example.todo.MainActivityBlue"),
            "MONOCHROME" to android.content.ComponentName(packageName, "com.example.todo.MainActivityMonochrome"),
            "EMERALD"    to android.content.ComponentName(packageName, "com.example.todo.MainActivityEmerald"),
            "CREAM"      to android.content.ComponentName(packageName, "com.example.todo.MainActivityCream"),
            "MIDNIGHT"   to android.content.ComponentName(packageName, "com.example.todo.MainActivityMidnight")
        )
        
        val pm = packageManager
        val flag = android.content.pm.PackageManager.DONT_KILL_APP
        val targetAlias = aliases[theme] ?: aliases["ORANGE"]!!
        
        try {
            val targetState = pm.getComponentEnabledSetting(targetAlias)
            val isAlreadyActive = targetState == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (theme == "ORANGE" && targetState == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

            if (!isAlreadyActive) {
                // Enable the target alias
                pm.setComponentEnabledSetting(
                    targetAlias,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    flag
                )
                // Disable all others
                aliases.forEach { (key, alias) ->
                    if (key != theme) {
                        pm.setComponentEnabledSetting(
                            alias,
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            flag
                        )
                    }
                }
                // if (showToast) {
                //     android.widget.Toast.makeText(this, "App icon updated! It may take a moment to appear.", android.widget.Toast.LENGTH_SHORT).show()
                // }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        lastSetColorTheme?.let { theme ->
            updateAppIcon(theme, showToast = false)
        }
    }
}

@Composable
fun MustDoApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes where bottom nav should be visible
    val bottomNavRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Tasks.route,
        Screen.Calendar.route,
        Screen.Focus.route,
        Screen.Settings.route
    )
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Box(
                                    modifier = if (selected) Modifier
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(MustDoColors.Primary, MustDoColors.PrimaryLight)
                                            ),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 20.dp, vertical = 6.dp)
                                    else Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                selectedTextColor = MustDoColors.Primary,
                                indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            MustDoNavHost(
                navController = navController
            )
        }
    }
}