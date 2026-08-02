package com.example.data.remote

import android.util.Log
import com.example.data.local.HabitDao
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
    data class Error(val errorMessage: String) : SyncStatus()
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseSyncRepository(
    private val habitDao: HabitDao,
    private val externalScope: CoroutineScope
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var habitsListener: ListenerRegistration? = null
    private var logsListener: ListenerRegistration? = null

    init {
        // Monitor Auth state changes automatically
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                _authState.value = AuthState.Authenticated(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Hero"
                )
                startFirestoreSync(currentUser.uid)
            } else {
                _authState.value = AuthState.Unauthenticated
            }

            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    val name = user.displayName ?: user.email?.substringBefore("@") ?: "Hero"
                    _authState.value = AuthState.Authenticated(user.uid, user.email ?: "", name)
                    startFirestoreSync(user.uid)
                } else {
                    stopFirestoreSync()
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to initialize Firebase Auth listener: ${e.message}", e)
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun getCurrentUserId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, displayName: String = ""): Result<FirebaseUser> {
        _authState.value = AuthState.Loading
        _syncStatus.value = SyncStatus.Syncing
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                val name = if (displayName.isNotBlank()) displayName else email.substringBefore("@")
                _authState.value = AuthState.Authenticated(user.uid, email, name)
                _syncStatus.value = SyncStatus.Success("Signed up successfully & connected to Firestore!")
                startFirestoreSync(user.uid)
                uploadAllLocalDataToFirestore(user.uid)
                Result.success(user)
            } else {
                val err = "Sign up failed: User creation returned null."
                _authState.value = AuthState.Error(err)
                _syncStatus.value = SyncStatus.Error(err)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            val errMessage = e.localizedMessage ?: "Sign up failed."
            _authState.value = AuthState.Error(errMessage)
            _syncStatus.value = SyncStatus.Error(errMessage)
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, pass: String): Result<FirebaseUser> {
        _authState.value = AuthState.Loading
        _syncStatus.value = SyncStatus.Syncing
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                val name = user.displayName ?: email.substringBefore("@")
                _authState.value = AuthState.Authenticated(user.uid, email, name)
                _syncStatus.value = SyncStatus.Success("Logged in successfully! Syncing Firestore habits...")
                startFirestoreSync(user.uid)
                Result.success(user)
            } else {
                val err = "Login failed: Null user returned."
                _authState.value = AuthState.Error(err)
                _syncStatus.value = SyncStatus.Error(err)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            val errMessage = e.localizedMessage ?: "Invalid email or password."
            _authState.value = AuthState.Error(errMessage)
            _syncStatus.value = SyncStatus.Error(errMessage)
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            stopFirestoreSync()
            auth.signOut()
            _authState.value = AuthState.Unauthenticated
            _syncStatus.value = SyncStatus.Idle
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error logging out: ${e.message}")
        }
    }

    fun startFirestoreSync(uid: String) {
        if (uid.isBlank()) return
        stopFirestoreSync()

        _syncStatus.value = SyncStatus.Syncing

        try {
            // Real-time listener for user's habits
            habitsListener = firestore.collection("users")
                .document(uid)
                .collection("habits")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("FirebaseSync", "Firestore habit sync error: ${error.message}")
                        _syncStatus.value = SyncStatus.Error("Firestore sync error: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        externalScope.launch(Dispatchers.IO) {
                            try {
                                for (doc in snapshots.documents) {
                                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                                    val title = doc.getString("title") ?: continue
                                    val description = doc.getString("description") ?: ""
                                    val category = doc.getString("category") ?: "Health"
                                    val frequency = doc.getString("frequency") ?: "Daily"
                                    val targetCount = doc.getLong("targetCount")?.toInt() ?: 1
                                    val unit = doc.getString("unit") ?: "times"
                                    val reminderTime = doc.getString("reminderTime") ?: "08:00"
                                    val colorHex = doc.getString("colorHex") ?: "#10B981"
                                    val isArchived = doc.getBoolean("isArchived") ?: false
                                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                                    val habitEntity = HabitEntity(
                                        id = id,
                                        title = title,
                                        description = description,
                                        category = category,
                                        frequency = frequency,
                                        targetCount = targetCount,
                                        unit = unit,
                                        reminderTime = reminderTime,
                                        colorHex = colorHex,
                                        createdAt = createdAt,
                                        isArchived = isArchived
                                    )
                                    habitDao.insertHabit(habitEntity)
                                }
                                _syncStatus.value = SyncStatus.Success("Cloud Firestore synced")
                            } catch (e: Exception) {
                                Log.e("FirebaseSync", "Error storing remote habits locally: ${e.message}")
                            }
                        }
                    }
                }

            // Real-time listener for habit completion logs
            logsListener = firestore.collection("users")
                .document(uid)
                .collection("habit_logs")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("FirebaseSync", "Firestore logs sync error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        externalScope.launch(Dispatchers.IO) {
                            try {
                                for (doc in snapshots.documents) {
                                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                                    val habitId = doc.getLong("habitId") ?: continue
                                    val date = doc.getString("date") ?: continue
                                    val completedCount = doc.getLong("completedCount")?.toInt() ?: 1
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                    val logEntity = HabitLogEntity(
                                        id = id,
                                        habitId = habitId,
                                        date = date,
                                        completedCount = completedCount,
                                        timestamp = timestamp
                                    )
                                    habitDao.insertLog(logEntity)
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseSync", "Error storing remote logs locally: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error attaching Firestore listeners: ${e.message}")
            _syncStatus.value = SyncStatus.Error("Network/Firestore error: ${e.localizedMessage}")
        }
    }

    private fun stopFirestoreSync() {
        habitsListener?.remove()
        habitsListener = null
        logsListener?.remove()
        logsListener = null
    }

    fun syncHabitToFirestore(habit: HabitEntity) {
        val uid = getCurrentUserId() ?: return
        externalScope.launch(Dispatchers.IO) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val habitMap = mapOf(
                    "id" to habit.id,
                    "title" to habit.title,
                    "description" to habit.description,
                    "category" to habit.category,
                    "frequency" to habit.frequency,
                    "targetCount" to habit.targetCount,
                    "unit" to habit.unit,
                    "reminderTime" to habit.reminderTime,
                    "colorHex" to habit.colorHex,
                    "createdAt" to habit.createdAt,
                    "isArchived" to habit.isArchived,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(uid)
                    .collection("habits")
                    .document(habit.id.toString())
                    .set(habitMap)
                    .await()

                _syncStatus.value = SyncStatus.Success("Habit synced to Cloud")
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed syncing habit to Firestore: ${e.message}")
                _syncStatus.value = SyncStatus.Error("Failed syncing habit: ${e.localizedMessage}")
            }
        }
    }

    fun deleteHabitFromFirestore(habitId: Long) {
        val uid = getCurrentUserId() ?: return
        externalScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("habits")
                    .document(habitId.toString())
                    .delete()
                    .await()

                _syncStatus.value = SyncStatus.Success("Habit deleted from Cloud")
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed deleting habit from Firestore: ${e.message}")
            }
        }
    }

    fun syncLogToFirestore(log: HabitLogEntity) {
        val uid = getCurrentUserId() ?: return
        externalScope.launch(Dispatchers.IO) {
            try {
                val docId = "${log.habitId}_${log.date}"
                val logMap = mapOf(
                    "id" to log.id,
                    "habitId" to log.habitId,
                    "date" to log.date,
                    "completedCount" to log.completedCount,
                    "timestamp" to log.timestamp
                )

                firestore.collection("users")
                    .document(uid)
                    .collection("habit_logs")
                    .document(docId)
                    .set(logMap)
                    .await()

                _syncStatus.value = SyncStatus.Success("Check-in synced to Cloud")
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed syncing log to Firestore: ${e.message}")
            }
        }
    }

    fun deleteLogFromFirestore(habitId: Long, dateStr: String) {
        val uid = getCurrentUserId() ?: return
        externalScope.launch(Dispatchers.IO) {
            try {
                val docId = "${habitId}_${dateStr}"
                firestore.collection("users")
                    .document(uid)
                    .collection("habit_logs")
                    .document(docId)
                    .delete()
                    .await()

                _syncStatus.value = SyncStatus.Success("Check-in removed from Cloud")
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed deleting log from Firestore: ${e.message}")
            }
        }
    }

    private suspend fun uploadAllLocalDataToFirestore(uid: String) {
        try {
            val habits = habitDao.getAllHabitsSync()
            val logs = habitDao.getAllLogsSync()

            for (habit in habits) {
                syncHabitToFirestore(habit)
            }
            for (log in logs) {
                syncLogToFirestore(log)
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading local data: ${e.message}")
        }
    }
}
