package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.example.HABIT_REMINDER_ACTION" -> {
                NotificationHelper.showDailyReminderNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val repository = UserPreferencesRepository(context.applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.userSettings.first()
                    if (settings.isReminderEnabled) {
                        NotificationHelper.scheduleDailyReminder(
                            context.applicationContext,
                            settings.defaultReminderTime
                        )
                    }
                }
            }
        }
    }
}
