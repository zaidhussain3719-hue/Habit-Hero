package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HabitDatabase
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.remote.AuthState
import com.example.data.remote.FirebaseSyncRepository
import com.example.data.remote.SyncStatus
import com.example.data.repository.BadgeItem
import com.example.data.repository.HabitRepository
import com.example.data.repository.MotivationalQuote
import com.example.data.repository.StreakInfo
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val habitDao = HabitDatabase.getDatabase(application, viewModelScope).habitDao()
    val firebaseSyncRepository = FirebaseSyncRepository(habitDao, viewModelScope)
    private val repository = HabitRepository(habitDao, firebaseSyncRepository)
    private val preferencesRepository = UserPreferencesRepository(application)

    val authState: StateFlow<AuthState> = firebaseSyncRepository.authState
    val syncStatus: StateFlow<SyncStatus> = firebaseSyncRepository.syncStatus

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val todayStr: String = LocalDate.now().format(formatter)

    // State flows
    private val _selectedDate = MutableStateFlow(todayStr)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _quote = MutableStateFlow(repository.getRandomDailyQuote())
    val quote: StateFlow<MotivationalQuote> = _quote.asStateFlow()

    private val _globalStats = MutableStateFlow(StreakInfo(0, 0, 0, 0))
    val globalStats: StateFlow<StreakInfo> = _globalStats.asStateFlow()

    private val _badges = MutableStateFlow<List<BadgeItem>>(emptyList())
    val badges: StateFlow<List<BadgeItem>> = _badges.asStateFlow()

    private val _unlockedBadgeEvent = MutableStateFlow<BadgeItem?>(null)
    val unlockedBadgeEvent: StateFlow<BadgeItem?> = _unlockedBadgeEvent.asStateFlow()

    // Preferences & Settings
    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    val savedUnlockedBadgeIds: StateFlow<Set<String>> = preferencesRepository.unlockedBadges.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )
    private val _isAddHabitDialogOpen = MutableStateFlow(false)
    val isAddHabitDialogOpen: StateFlow<Boolean> = _isAddHabitDialogOpen.asStateFlow()

    private val _isAuthModalOpen = MutableStateFlow(false)
    val isAuthModalOpen: StateFlow<Boolean> = _isAuthModalOpen.asStateFlow()

    private val _isExportModalOpen = MutableStateFlow(false)
    val isExportModalOpen: StateFlow<Boolean> = _isExportModalOpen.asStateFlow()

    private val _isBackupModalOpen = MutableStateFlow(false)
    val isBackupModalOpen: StateFlow<Boolean> = _isBackupModalOpen.asStateFlow()

    private val _isAdBannerVisible = MutableStateFlow(true)
    val isAdBannerVisible: StateFlow<Boolean> = _isAdBannerVisible.asStateFlow()

    // Habits stream
    val habits: StateFlow<List<HabitEntity>> = repository.allActiveHabits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Logs for current selected date
    private val _logsForSelectedDate = MutableStateFlow<List<HabitLogEntity>>(emptyList())
    val logsForSelectedDate: StateFlow<List<HabitLogEntity>> = _logsForSelectedDate.asStateFlow()

    // Filtered habits list combining habits, category, and search query
    val filteredHabits: StateFlow<List<HabitEntity>> = combine(
        habits,
        searchQuery,
        selectedCategory
    ) { habitList, query, cat ->
        habitList.filter { h ->
            val matchesQuery = query.isEmpty() || h.title.contains(query, ignoreCase = true) || h.description.contains(query, ignoreCase = true)
            val matchesCat = cat == "All" || h.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshStatsAndLogs()
        viewModelScope.launch {
            authState.collect { auth ->
                when (auth) {
                    is AuthState.Authenticated -> {
                        preferencesRepository.setUserLogin(isLoggedIn = true, name = auth.displayName, email = auth.email)
                    }
                    is AuthState.Unauthenticated -> {
                        preferencesRepository.setUserLogin(isLoggedIn = false, name = "Guest User", email = "guest@habithero.app")
                    }
                    else -> {}
                }
            }
        }
    }

    fun refreshStatsAndLogs() {
        viewModelScope.launch {
            val savedSet = savedUnlockedBadgeIds.value
            val stats = repository.calculateGlobalStats()
            _globalStats.value = stats

            val updatedBadges = repository.getAchievements(stats, savedSet)
            _badges.value = updatedBadges

            // Identify newly unlocked badges that were not previously saved
            val newlyUnlocked = updatedBadges.filter { it.isUnlocked && !savedSet.contains(it.id) }
            for (badge in newlyUnlocked) {
                preferencesRepository.saveUnlockedBadge(badge.id)
            }

            if (newlyUnlocked.isNotEmpty() && _unlockedBadgeEvent.value == null) {
                _unlockedBadgeEvent.value = newlyUnlocked.first()
            }

            loadLogsForDate(_selectedDate.value)
        }
    }

    fun dismissBadgeEventDialog() {
        _unlockedBadgeEvent.value = null
    }

    private fun loadLogsForDate(dateStr: String) {
        viewModelScope.launch {
            repository.getLogsForDate(dateStr).collect { logList ->
                _logsForSelectedDate.value = logList
            }
        }
    }

    fun toggleCheckIn(habitId: Long) {
        viewModelScope.launch {
            repository.toggleHabitCheckIn(habitId, _selectedDate.value)
            refreshStatsAndLogs()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setSelectedDate(dateStr: String) {
        _selectedDate.value = dateStr
        loadLogsForDate(dateStr)
    }

    fun addHabit(
        title: String,
        description: String,
        category: String,
        frequency: String,
        targetCount: Int,
        unit: String,
        reminderTime: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val habit = HabitEntity(
                title = title,
                description = description,
                category = category,
                frequency = frequency,
                targetCount = targetCount,
                unit = unit,
                reminderTime = reminderTime,
                colorHex = colorHex
            )
            repository.insertHabit(habit)
            refreshStatsAndLogs()
            _isAddHabitDialogOpen.value = false
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            refreshStatsAndLogs()
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            refreshStatsAndLogs()
        }
    }

    fun openAddHabitDialog(open: Boolean) {
        _isAddHabitDialogOpen.value = open
    }

    fun openAuthModal(open: Boolean) {
        _isAuthModalOpen.value = open
    }

    fun openExportModal(open: Boolean) {
        _isExportModalOpen.value = open
    }

    fun openBackupModal(open: Boolean) {
        _isBackupModalOpen.value = open
    }

    fun dismissAdBanner() {
        _isAdBannerVisible.value = false
    }

    fun loginWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseSyncRepository.loginWithEmail(email, pass)
            result.fold(
                onSuccess = { user ->
                    val name = user.displayName ?: email.substringBefore("@")
                    preferencesRepository.setUserLogin(isLoggedIn = true, name = name, email = user.email ?: email)
                    _isAuthModalOpen.value = false
                    refreshStatsAndLogs()
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage ?: "Login failed")
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseSyncRepository.signUpWithEmail(email, pass, name)
            result.fold(
                onSuccess = { user ->
                    val userName = if (name.isNotBlank()) name else email.substringBefore("@")
                    preferencesRepository.setUserLogin(isLoggedIn = true, name = userName, email = user.email ?: email)
                    _isAuthModalOpen.value = false
                    refreshStatsAndLogs()
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage ?: "Sign up failed")
                }
            )
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            firebaseSyncRepository.logout()
            preferencesRepository.setUserLogin(isLoggedIn = false, name = "Guest User", email = "guest@habithero.app")
            refreshStatsAndLogs()
        }
    }

    fun manualSync() {
        val uid = firebaseSyncRepository.getCurrentUserId()
        if (!uid.isNullOrBlank()) {
            firebaseSyncRepository.startFirestoreSync(uid)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReminderEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                NotificationHelper.scheduleDailyReminder(context, userSettings.value.defaultReminderTime)
            } else {
                NotificationHelper.cancelDailyReminder(context)
            }
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            preferencesRepository.setReminderTime(time)
            val context = getApplication<Application>()
            if (userSettings.value.isReminderEnabled) {
                NotificationHelper.scheduleDailyReminder(context, time)
            }
        }
    }

    fun sendTestNotification() {
        val context = getApplication<Application>()
        NotificationHelper.showTestNotification(context)
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            preferencesRepository.setPremiumStatus(true)
            _isAdBannerVisible.value = false
        }
    }

    fun generateExportSummaryReport(): String {
        val stats = _globalStats.value
        val user = userSettings.value
        val habitList = habits.value
        val dateNow = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

        return buildString {
            appendLine("==========================================")
            appendLine("           HABIT HERO PROGRESS REPORT     ")
            appendLine("==========================================")
            appendLine("User: ${user.userName} (${user.userEmail})")
            appendLine("Date Generated: $dateNow")
            appendLine()
            appendLine("--- SUMMARY STATISTICS ---")
            appendLine("Current Active Streak: ${stats.currentStreak} Days 🔥")
            appendLine("Longest Streak Record: ${stats.longestStreak} Days ⚡")
            appendLine("Total Habit Check-Ins: ${stats.totalCheckIns} 💯")
            appendLine("Today's Completion Rate: ${stats.completionRatePercentage}%")
            appendLine()
            appendLine("--- ACTIVE HABITS (${habitList.size}) ---")
            habitList.forEachIndexed { i, h ->
                appendLine("${i + 1}. ${h.title} [${h.category}]")
                appendLine("   Goal: ${h.targetCount} ${h.unit} (${h.frequency})")
            }
            appendLine()
            appendLine("==========================================")
            appendLine("    Keep building momentum with Habit Hero! ")
            appendLine("==========================================")
        }
    }
}
