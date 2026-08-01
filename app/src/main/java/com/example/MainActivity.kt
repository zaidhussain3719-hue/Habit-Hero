package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AddEditHabitDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.BadgeUnlockedDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HabitHeroTheme
import com.example.ui.viewmodel.HabitViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.notifications.NotificationHelper.createNotificationChannel(this)

        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            val isDarkTheme = when (userSettings.themeMode) {
                "Dark", "Emerald" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            HabitHeroTheme(darkTheme = isDarkTheme) {
                HabitHeroApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HabitHeroApp(viewModel: HabitViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val isAddHabitDialogOpen by viewModel.isAddHabitDialogOpen.collectAsState()
    val isExportModalOpen by viewModel.isExportModalOpen.collectAsState()
    val isAuthModalOpen by viewModel.isAuthModalOpen.collectAsState()
    val unlockedBadgeEvent by viewModel.unlockedBadgeEvent.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel)
            }
            composable(Screen.Habits.route) {
                HabitsScreen(viewModel = viewModel)
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen(viewModel = viewModel)
            }
            composable(Screen.Achievements.route) {
                AchievementsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // Add Habit Dialog
        if (isAddHabitDialogOpen) {
            AddEditHabitDialog(
                onDismiss = { viewModel.openAddHabitDialog(false) },
                onConfirm = { title, desc, cat, freq, target, unit, reminder, color ->
                    viewModel.addHabit(title, desc, cat, freq, target, unit, reminder, color)
                }
            )
        }

        // Export Report Modal
        if (isExportModalOpen) {
            ExportReportDialog(
                reportText = viewModel.generateExportSummaryReport(),
                onDismiss = { viewModel.openExportModal(false) }
            )
        }

        // Auth Modal
        if (isAuthModalOpen) {
            AuthDialog(
                onDismiss = { viewModel.openAuthModal(false) },
                onLogin = { name, email ->
                    viewModel.loginUser(name, email)
                }
            )
        }

        // Unlocked Badge Celebration Dialog
        unlockedBadgeEvent?.let { badge ->
            BadgeUnlockedDialog(
                badge = badge,
                onDismiss = { viewModel.dismissBadgeEventDialog() }
            )
        }
    }
}
