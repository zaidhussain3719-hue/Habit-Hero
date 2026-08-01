package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringSetPreferencesKey

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val isLoggedIn: Boolean = true,
    val userName: String = "Hero User",
    val userEmail: String = "hero@habithero.app",
    val themeMode: String = "Light", // "Light", "Dark", "Emerald", "Forest"
    val isReminderEnabled: Boolean = true,
    val defaultReminderTime: String = "08:00",
    val isPremium: Boolean = false,
    val language: String = "English"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_REMINDER_ENABLED = booleanPreferencesKey("is_reminder_enabled")
        val DEFAULT_REMINDER_TIME = stringPreferencesKey("default_reminder_time")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val LANGUAGE = stringPreferencesKey("language")
        val UNLOCKED_BADGES = stringSetPreferencesKey("unlocked_badges")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: true,
            userName = prefs[Keys.USER_NAME] ?: "Hero User",
            userEmail = prefs[Keys.USER_EMAIL] ?: "hero@habithero.app",
            themeMode = prefs[Keys.THEME_MODE] ?: "Light",
            isReminderEnabled = prefs[Keys.IS_REMINDER_ENABLED] ?: true,
            defaultReminderTime = prefs[Keys.DEFAULT_REMINDER_TIME] ?: "08:00",
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "English"
        )
    }

    val unlockedBadges: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNLOCKED_BADGES] ?: emptySet()
    }

    suspend fun saveUnlockedBadge(badgeId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_BADGES] ?: emptySet()
            if (!current.contains(badgeId)) {
                prefs[Keys.UNLOCKED_BADGES] = current + badgeId
            }
        }
    }

    suspend fun setUserLogin(isLoggedIn: Boolean, name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = isLoggedIn
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_REMINDER_TIME] = time
        }
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
        }
    }
}
