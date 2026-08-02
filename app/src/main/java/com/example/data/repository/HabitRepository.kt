package com.example.data.repository

import com.example.data.local.HabitDao
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.remote.FirebaseSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCheckIns: Int,
    val completionRatePercentage: Int
)

data class MotivationalQuote(
    val quote: String,
    val author: String
)

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String = "🏆",
    val iconName: String = "Star",
    val isUnlocked: Boolean = false,
    val currentProgress: Int = 0,
    val maxProgress: Int = 1,
    val progressPercentage: Float = 0f,
    val progressText: String = "",
    val unlockedAt: String? = null
)

class HabitRepository(
    private val habitDao: HabitDao,
    private val firebaseSyncRepository: FirebaseSyncRepository? = null
) {

    val allActiveHabits: Flow<List<HabitEntity>> = habitDao.getAllActiveHabits()
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLogEntity>> = habitDao.getAllLogs()

    fun getLogsForDate(date: String): Flow<List<HabitLogEntity>> = habitDao.getLogsForDate(date)

    suspend fun getHabitById(id: Long): HabitEntity? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: HabitEntity): Long {
        val newId = habitDao.insertHabit(habit)
        val habitToSync = if (habit.id == 0L) habit.copy(id = newId) else habit
        firebaseSyncRepository?.syncHabitToFirestore(habitToSync)
        return newId
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
        firebaseSyncRepository?.syncHabitToFirestore(habit)
    }

    suspend fun deleteHabit(id: Long) {
        habitDao.deleteHabitById(id)
        habitDao.deleteAllLogsForHabit(id)
        firebaseSyncRepository?.deleteHabitFromFirestore(id)
    }

    suspend fun toggleHabitCheckIn(habitId: Long, dateStr: String) {
        val habit = habitDao.getHabitById(habitId) ?: return
        val existingLog = habitDao.getLogForHabitAndDate(habitId, dateStr)
        if (existingLog != null) {
            // Already completed today -> remove log (uncheck)
            habitDao.deleteLog(existingLog)
            firebaseSyncRepository?.deleteLogFromFirestore(habitId, dateStr)
        } else {
            // Check in -> create log
            val log = HabitLogEntity(
                habitId = habitId,
                date = dateStr,
                completedCount = habit.targetCount
            )
            habitDao.insertLog(log)
            firebaseSyncRepository?.syncLogToFirestore(log)
        }
    }

    suspend fun calculateHabitStreak(habitId: Long): StreakInfo {
        val logs = habitDao.getLogsForHabit(habitId).first().sortedByDescending { it.date }
        if (logs.isEmpty()) return StreakInfo(0, 0, 0, 0)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val todayStr = today.format(formatter)
        val yesterdayStr = today.minusDays(1).format(formatter)

        val dateSet = logs.map { it.date }.toSet()

        var currentStreak = 0
        var checkDate = if (dateSet.contains(todayStr)) today else if (dateSet.contains(yesterdayStr)) today.minusDays(1) else null

        if (checkDate != null) {
            while (dateSet.contains(checkDate?.format(formatter))) {
                currentStreak++
                checkDate = checkDate?.minusDays(1)
            }
        }

        // Longest streak calculation
        val sortedDates = dateSet.mapNotNull {
            try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
        }.sorted()

        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if (prevDate == null || d == prevDate.plusDays(1)) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            longestStreak = max(longestStreak, tempStreak)
            prevDate = d
        }

        val totalCheckIns = logs.size
        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCheckIns = totalCheckIns,
            completionRatePercentage = if (totalCheckIns > 0) 100 else 0
        )
    }

    suspend fun calculateGlobalStats(): StreakInfo {
        val habits = habitDao.getAllActiveHabits().first()
        val logs = habitDao.getAllLogs().first()

        if (habits.isEmpty()) return StreakInfo(0, 0, 0, 0)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val todayStr = today.format(formatter)
        val yesterdayStr = today.minusDays(1).format(formatter)

        // Group logs by date
        val datesWithLogs = logs.map { it.date }.toSet()

        var currentStreak = 0
        var checkDate = if (datesWithLogs.contains(todayStr)) today else if (datesWithLogs.contains(yesterdayStr)) today.minusDays(1) else null

        if (checkDate != null) {
            while (datesWithLogs.contains(checkDate?.format(formatter))) {
                currentStreak++
                checkDate = checkDate?.minusDays(1)
            }
        }

        val sortedDates = datesWithLogs.mapNotNull {
            try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
        }.sorted()

        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if (prevDate == null || d == prevDate.plusDays(1)) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            longestStreak = max(longestStreak, tempStreak)
            prevDate = d
        }

        val totalCheckIns = logs.size
        val todayLogsCount = logs.count { it.date == todayStr }
        val completionRate = if (habits.isNotEmpty()) ((todayLogsCount.toFloat() / habits.size.toFloat()) * 100).toInt().coerceAtMost(100) else 0

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCheckIns = totalCheckIns,
            completionRatePercentage = completionRate
        )
    }

    fun getRandomDailyQuote(): MotivationalQuote {
        val quotes = listOf(
            MotivationalQuote("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Aristotle"),
            MotivationalQuote("Small daily improvements over time lead to stunning results.", "Robin Sharma"),
            MotivationalQuote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
            MotivationalQuote("Motivation is what gets you started. Habit is what keeps you going.", "Jim Ryun"),
            MotivationalQuote("Your habits will determine your future. Choose them wisely.", "Jack Canfield"),
            MotivationalQuote("Chains of habit are too light to be felt until they are too heavy to be broken.", "Warren Buffett"),
            MotivationalQuote("You’ll never change your life until you change something you do daily.", "John C. Maxwell")
        )
        return quotes.random()
    }

    suspend fun getAchievements(
        globalStats: StreakInfo,
        savedUnlockedBadgeIds: Set<String> = emptySet()
    ): List<BadgeItem> {
        val habits = habitDao.getAllHabits().first()
        val logs = habitDao.getAllLogs().first()

        val totalCheckIns = globalStats.totalCheckIns
        val maxStreak = max(globalStats.currentStreak, globalStats.longestStreak)
        val uniqueDays = logs.map { it.date }.distinct().size
        val totalHabitsCreated = habits.size

        return listOf(
            // 1. 🌱 First Step
            createBadgeItem(
                id = "first_step",
                title = "First Step",
                description = "Complete your first habit.",
                emoji = "🌱",
                iconName = "CheckCircle",
                current = totalCheckIns,
                target = 1,
                unit = "habit",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 2. 🔥 3-Day Streak
            createBadgeItem(
                id = "streak_3",
                title = "3-Day Streak",
                description = "Maintain a 3-day streak.",
                emoji = "🔥",
                iconName = "Whatshot",
                current = maxStreak,
                target = 3,
                unit = "days",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 3. 💪 7-Day Streak
            createBadgeItem(
                id = "streak_7",
                title = "7-Day Streak",
                description = "Maintain a 7-day streak.",
                emoji = "💪",
                iconName = "Bolt",
                current = maxStreak,
                target = 7,
                unit = "days",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 4. 🏆 30-Day Champion
            createBadgeItem(
                id = "streak_30",
                title = "30-Day Champion",
                description = "Maintain a 30-day streak.",
                emoji = "🏆",
                iconName = "EmojiEvents",
                current = maxStreak,
                target = 30,
                unit = "days",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 5. 📅 Consistent User
            createBadgeItem(
                id = "consistent_user",
                title = "Consistent User",
                description = "Complete habits for 15 different days.",
                emoji = "📅",
                iconName = "CalendarToday",
                current = uniqueDays,
                target = 15,
                unit = "days",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 6. ⭐ Habit Master
            createBadgeItem(
                id = "habit_master",
                title = "Habit Master",
                description = "Create 10 habits.",
                emoji = "⭐",
                iconName = "Star",
                current = totalHabitsCreated,
                target = 10,
                unit = "habits",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 7. 🎯 Goal Crusher
            createBadgeItem(
                id = "goal_crusher",
                title = "Goal Crusher",
                description = "Complete 100 total habits.",
                emoji = "🎯",
                iconName = "MilitaryTech",
                current = totalCheckIns,
                target = 100,
                unit = "check-ins",
                savedUnlocked = savedUnlockedBadgeIds
            ),
            // 8. 👑 Legend
            createBadgeItem(
                id = "legend",
                title = "Legend",
                description = "Complete 500 total habits.",
                emoji = "👑",
                iconName = "WorkspacePremium",
                current = totalCheckIns,
                target = 500,
                unit = "check-ins",
                savedUnlocked = savedUnlockedBadgeIds
            )
        )
    }

    private fun createBadgeItem(
        id: String,
        title: String,
        description: String,
        emoji: String,
        iconName: String,
        current: Int,
        target: Int,
        unit: String,
        savedUnlocked: Set<String>
    ): BadgeItem {
        val isUnlocked = current >= target || id in savedUnlocked
        val currentProgress = current.coerceAtMost(target)
        val percentage = (currentProgress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        val progressText = if (isUnlocked) {
            "Unlocked! ($target / $target $unit)"
        } else {
            "$currentProgress / $target $unit (${(percentage * 100).toInt()}%)"
        }

        return BadgeItem(
            id = id,
            title = title,
            description = description,
            emoji = emoji,
            iconName = iconName,
            isUnlocked = isUnlocked,
            currentProgress = currentProgress,
            maxProgress = target,
            progressPercentage = percentage,
            progressText = progressText
        )
    }
}
