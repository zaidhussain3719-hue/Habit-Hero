package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val context = LocalContext.current

    var isTimePickerDialogVisible by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage account, reminders, backup, and themes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userSettings.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userSettings.userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (userSettings.isPremium) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "HERO PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (userSettings.isLoggedIn) {
                                viewModel.logoutUser()
                            } else {
                                viewModel.openAuthModal(true)
                            }
                        },
                        modifier = Modifier.testTag("auth_action_button")
                    ) {
                        Text(if (userSettings.isLoggedIn) "Logout" else "Sign In")
                    }
                }
            }
        }

        // Premium Monetization Card
        if (!userSettings.isPremium) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unlock Hero Premium",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ad-free, unlimited habits, & exclusive dark themes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { viewModel.upgradeToPremium() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.testTag("upgrade_pro_button")
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }

        // Daily Local Notifications Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Habit Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Enable/Disable Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifications",
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reminders",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (userSettings.isReminderEnabled)
                                    "Scheduled for ${format12HourTime(userSettings.defaultReminderTime)}"
                                else
                                    "Reminders disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userSettings.isReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.toggleReminderEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("daily_reminder_switch")
                        )
                    }

                    if (userSettings.isReminderEnabled) {
                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        // Time Picker Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTimePickerDialogVisible = true }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Time",
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = format12HourTime(userSettings.defaultReminderTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { isTimePickerDialogVisible = true },
                                modifier = Modifier.testTag("change_reminder_time_button")
                            ) {
                                Text("Change Time")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        // Permission Status Row (Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notification Permission",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (hasNotificationPermission) "Granted ✓" else "Permission required for system alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasNotificationPermission) EmeraldPrimary else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (!hasNotificationPermission) {
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.testTag("request_permission_button")
                                    ) {
                                        Text("Grant")
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp))
                        }

                        // Send Test Notification Button
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.sendTestNotification()
                                    Toast.makeText(context, "Test notification sent! 🔔", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_notification_button")
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Test Notification")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Test Notification")
                        }
                    }
                }
            }
        }

        // App Preferences Group
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Display & Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Theme Selection Row
                    SettingsActionRow(
                        title = "Theme Mode",
                        subtitle = "Current: ${userSettings.themeMode}",
                        icon = Icons.Default.Palette,
                        onClick = {
                            val nextMode = when (userSettings.themeMode) {
                                "Light" -> "Dark"
                                "Dark" -> "Emerald"
                                else -> "Light"
                            }
                            viewModel.setThemeMode(nextMode)
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Language
                    SettingsActionRow(
                        title = "Language",
                        subtitle = userSettings.language,
                        icon = Icons.Default.Language,
                        onClick = {
                            Toast.makeText(context, "English is set as default language", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Backup & Export Data Group
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data & Backup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SettingsActionRow(
                        title = "Export Progress Summary",
                        subtitle = "Generate shareable PDF/Text report",
                        icon = Icons.Default.FileDownload,
                        onClick = { viewModel.openExportModal(true) }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsActionRow(
                        title = "Backup & Restore Data",
                        subtitle = "Automatic local database sync",
                        icon = Icons.Default.Backup,
                        onClick = {
                            Toast.makeText(context, "Automatic cloud/local sync active!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // About App
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Habit Hero v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Built with Jetpack Compose & Room Database",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // Time Picker Modal Dialog
    if (isTimePickerDialogVisible) {
        TimePickerModalDialog(
            initialTime = userSettings.defaultReminderTime,
            onDismiss = { isTimePickerDialogVisible = false },
            onTimeSelected = { newTime ->
                viewModel.setReminderTime(newTime)
                Toast.makeText(context, "Reminder scheduled for ${format12HourTime(newTime)}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModalDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Reminder Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onTimeSelected(formatted)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun format12HourTime(timeStr: String): String {
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%02d:%02d %s", displayHour, minute, amPm)
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = EmeraldPrimary)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
