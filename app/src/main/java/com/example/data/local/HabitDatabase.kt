package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(entities = [HabitEntity::class, HabitLogEntity::class], version = 1, exportSchema = false)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_hero_database"
                )
                    .addCallback(HabitDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class HabitDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.habitDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: HabitDao) {
                val sampleHabits = listOf(
                    HabitEntity(
                        title = "Morning Water (8 Glasses)",
                        description = "Stay hydrated throughout the day",
                        category = "Health",
                        frequency = "Daily",
                        targetCount = 8,
                        unit = "glasses",
                        reminderTime = "08:00",
                        iconName = "WaterDrop",
                        colorHex = "#10B981"
                    ),
                    HabitEntity(
                        title = "Read 20 Pages",
                        description = "Expand knowledge and focus",
                        category = "Reading",
                        frequency = "Daily",
                        targetCount = 20,
                        unit = "pages",
                        reminderTime = "21:00",
                        iconName = "Book",
                        colorHex = "#8B5CF6"
                    ),
                    HabitEntity(
                        title = "30 Min Exercise",
                        description = "Cardio or strength workout",
                        category = "Fitness",
                        frequency = "Daily",
                        targetCount = 30,
                        unit = "mins",
                        reminderTime = "17:30",
                        iconName = "FitnessCenter",
                        colorHex = "#F59E0B"
                    ),
                    HabitEntity(
                        title = "Study Mobile Dev",
                        description = "Kotlin, Jetpack Compose, & UI Architecture",
                        category = "Study",
                        frequency = "Daily",
                        targetCount = 1,
                        unit = "session",
                        reminderTime = "10:00",
                        iconName = "Code",
                        colorHex = "#3B82F6"
                    ),
                    HabitEntity(
                        title = "Deep Focus Work",
                        description = "Pomodoro focus block without social media",
                        category = "Work",
                        frequency = "Daily",
                        targetCount = 2,
                        unit = "blocks",
                        reminderTime = "09:30",
                        iconName = "Work",
                        colorHex = "#EC4899"
                    ),
                    HabitEntity(
                        title = "10 Min Meditation",
                        description = "Mindfulness & breathing exercise",
                        category = "Personal",
                        frequency = "Daily",
                        targetCount = 10,
                        unit = "mins",
                        reminderTime = "07:00",
                        iconName = "SelfImprovement",
                        colorHex = "#14B8A6"
                    )
                )

                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                sampleHabits.forEachIndexed { index, habit ->
                    val habitId = dao.insertHabit(habit)
                    // Insert initial streak logs for sample habits for the last 3 days
                    for (daysAgo in 0..2) {
                        val dateStr = today.minusDays(daysAgo.toLong()).format(formatter)
                        if (index % 2 == 0 || daysAgo < 2) {
                            dao.insertLog(
                                HabitLogEntity(
                                    habitId = habitId,
                                    date = dateStr,
                                    completedCount = habit.targetCount
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
