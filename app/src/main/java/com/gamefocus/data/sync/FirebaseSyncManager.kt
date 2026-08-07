package com.gamefocus.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.*
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.data.models.GameSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FirebaseSyncManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L

        @Volatile
        private var INSTANCE: FirebaseSyncManager? = null

        fun getInstance(context: Context): FirebaseSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val settingsManager = SettingsManager(context)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: StateFlow<SyncProgress?> = _syncProgress.asStateFlow()

    private var syncJob: Job? = null

    init {
        // Observe auth state
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val wasSignedIn = _isSignedIn.value
            _isSignedIn.value = user != null
            _userId.value = user?.uid

            // Auto sign-in anonymously when sync is enabled and not signed in
            if (user == null && _syncEnabled.value && !wasSignedIn) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        signInAnonymously()
                    } catch (e: Exception) {
                        Log.w(TAG, "Auto sign-in failed: ${e.message}")
                    }
                }
            }
        }

        // Observe sync enabled setting
        CoroutineScope(Dispatchers.IO).launch {
            settingsManager.syncEnabled.collectLatest { enabled ->
                _syncEnabled.value = enabled
                if (enabled && auth.currentUser == null) {
                    signInAnonymously()
                }
            }
        }

        // Observe session endings to trigger auto-sync
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = com.gamefocus.data.local.SessionManager.getInstance(context, com.gamefocus.data.local.AppDatabase.getInstance(context).gameSessionDao())
                var previousActiveSessionId: Long? = null

                sessionManager.activeSession.collectLatest { active ->
                    val currentId = active?.id
                    if (currentId != null && currentId != previousActiveSessionId) {
                        previousActiveSessionId = currentId
                    } else if (currentId == null && previousActiveSessionId != null) {
                        previousActiveSessionId = null
                        Log.d(TAG, "Session ended, triggering auto-sync...")
                        val sessions = try {
                            com.gamefocus.data.local.AppDatabase.getInstance(context).gameSessionDao().getAllSessionsList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        triggerSync(sessions)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to observe session manager: ${e.message}")
            }
        }

        scheduleSyncRetryWork()
    }

    private fun scheduleSyncRetryWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRetryRequest = PeriodicWorkRequest.Builder(
            com.gamefocus.data.sync.SyncRetryWorker::class.java,
            15,
            java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            com.gamefocus.data.sync.SyncRetryWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRetryRequest
        )
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    fun signInAnonymously() {
        if (_isSignedIn.value) return

        _syncStatus.value = SyncStatus.SigningIn
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                _isSignedIn.value = true
                _userId.value = result.user?.uid
                _syncStatus.value = SyncStatus.SignedIn
                Log.d(TAG, "Anonymous sign-in successful: ${result.user?.uid}")
            }
            .addOnFailureListener { e ->
                _syncStatus.value = SyncStatus.Error("Sign-in failed: ${e.message}")
                Log.e(TAG, "Anonymous sign-in failed", e)
            }
    }

    fun signInWithGoogle(idToken: String) {
        _syncStatus.value = SyncStatus.SigningIn
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                _isSignedIn.value = true
                _userId.value = result.user?.uid
                _syncStatus.value = SyncStatus.SignedIn
            }
            .addOnFailureListener { e ->
                _syncStatus.value = SyncStatus.Error("Google sign-in failed: ${e.message}")
            }
    }

    fun signUpWithEmail(email: String, password: String) {
        if (!isValidEmail(email) || !isValidPassword(password)) {
            _syncStatus.value = SyncStatus.Error("Invalid email or password")
            return
        }
        _syncStatus.value = SyncStatus.SigningIn
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _isSignedIn.value = true
                _userId.value = result.user?.uid
                _syncStatus.value = SyncStatus.SignedIn
            }
            .addOnFailureListener { e ->
                _syncStatus.value = SyncStatus.Error("Sign-up failed: ${e.message}")
            }
    }

    fun signInWithEmail(email: String, password: String) {
        if (!isValidEmail(email) || !isValidPassword(password)) {
            _syncStatus.value = SyncStatus.Error("Invalid email or password")
            return
        }
        _syncStatus.value = SyncStatus.SigningIn
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _isSignedIn.value = true
                _userId.value = result.user?.uid
                _syncStatus.value = SyncStatus.SignedIn
            }
            .addOnFailureListener { e ->
                _syncStatus.value = SyncStatus.Error("Email sign-in failed: ${e.message}")
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun enableSync(enabled: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            settingsManager.setSyncEnabled(enabled)
        }
    }

    fun triggerSync(sessions: List<GameSession>) {
        if (syncJob?.isActive == true) {
            return
        }

        if (!_syncEnabled.value) {
            return
        }

        if (!_isSignedIn.value) {
            signInAnonymously()
            return
        }

        if (!isNetworkAvailable()) {
            _syncStatus.value = SyncStatus.Offline
            return
        }

        syncJob = CoroutineScope(Dispatchers.IO).launch {
            performFullSync(sessions)
        }
    }

    private suspend fun performFullSync(localSessions: List<GameSession> = emptyList()) {
        val userId = _userId.value ?: return

        _syncStatus.value = SyncStatus.Syncing
        _syncProgress.value = SyncProgress(0, "Starting sync...")

        try {
            // Step 1: Upload local sessions
            _syncProgress.value = SyncProgress(10, "Uploading sessions...")
            uploadSessions(userId, localSessions)

            // Step 2: Download remote sessions
            _syncProgress.value = SyncProgress(50, "Downloading sessions...")
            val remoteSessions = downloadRemoteSessions(userId)

            // Step 3: Merge local and remote
            _syncProgress.value = SyncProgress(75, "Merging data...")
            val mergedSessions = mergeSessions(localSessions, remoteSessions)

            // Step 4: Update local database with merged data
            _syncProgress.value = SyncProgress(90, "Updating local data...")
            updateLocalDatabase(mergedSessions)

            // Step 5: Upload any conflicts that were resolved
            uploadResolvedConflicts(userId, mergedSessions)

            _lastSyncTime.value = System.currentTimeMillis()
            _syncProgress.value = SyncProgress(100, "Sync complete")
            _syncStatus.value = SyncStatus.Idle

            delay(2000)
            _syncProgress.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncStatus.value = SyncStatus.Error("Sync failed: ${e.message}")
            _syncProgress.value = null
        }
    }

    private suspend fun uploadSessions(userId: String, sessions: List<GameSession>) {
        var uploaded = 0
        val total = sessions.size

        for (session in sessions) {
            try {
                val sessionMap = hashMapOf(
                    "packageName" to session.packageName,
                    "gameName" to session.gameName,
                    "startTime" to session.startTime,
                    "endTime" to session.endTime,
                    "durationMillis" to session.durationMillis,
                    "sessionDate" to session.sessionDate,
                    "focusModeStart" to session.focusModeStart,
                    "focusModeEnd" to session.focusModeEnd,
                    "updatedAt" to System.currentTimeMillis(),
                    "source" to "local"
                )

                db.collection("users").document(userId)
                    .collection("sessions")
                    .document(session.id.toString())
                    .set(sessionMap, SetOptions.merge())
                    .await()

                uploaded++
                _syncProgress.value = SyncProgress(
                    progress = 10 + (uploaded * 40 / total.coerceAtLeast(1)),
                    message = "Uploading... ($uploaded/$total)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload session ${session.id}: ${e.message}")
            }
        }
    }

    private suspend fun downloadRemoteSessions(userId: String): List<GameSession> {
        return try {
            val documents = db.collection("users").document(userId)
                .collection("sessions")
                .get()
                .await()

            documents.map { doc ->
                GameSession(
                    id = doc.id.toLongOrNull() ?: 0,
                    packageName = doc.getString("packageName") ?: "",
                    gameName = doc.getString("gameName") ?: "Unknown",
                    startTime = doc.getLong("startTime") ?: 0L,
                    endTime = doc.getLong("endTime") ?: 0L,
                    durationMillis = doc.getLong("durationMillis") ?: 0L,
                    sessionDate = doc.getLong("sessionDate") ?: 0L,
                    focusModeStart = doc.getLong("focusModeStart") ?: 0L,
                    focusModeEnd = doc.getLong("focusModeEnd") ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download remote sessions: ${e.message}")
            emptyList()
        }
    }

    private fun mergeSessions(local: List<GameSession>, remote: List<GameSession>): List<GameSession> {
        val merged = mutableMapOf<Long, GameSession>()

        // Add local sessions
        local.forEach { session ->
            merged[session.id] = session
        }

        // Merge remote sessions
        remote.forEach { remoteSession ->
            val localSession = merged[remoteSession.id]
            if (localSession == null) {
                // New remote session, add it
                merged[remoteSession.id] = remoteSession
            } else {
                // Conflict resolution: prefer the one with longer duration or later endTime
                // For active sessions (endTime == 0), prefer local
                if (localSession.endTime == 0L) {
                    merged[remoteSession.id] = localSession
                } else if (remoteSession.endTime == 0L) {
                    merged[remoteSession.id] = remoteSession
                } else {
                    // Both completed, prefer the one with longer duration
                    // If durations are equal, prefer the one with later startTime
                    if (remoteSession.durationMillis > localSession.durationMillis) {
                        merged[remoteSession.id] = remoteSession
                    } else if (remoteSession.durationMillis == localSession.durationMillis &&
                               remoteSession.startTime > localSession.startTime) {
                        merged[remoteSession.id] = remoteSession
                    } else {
                        merged[remoteSession.id] = localSession
                    }
                }
            }
        }

        return merged.values.toList()
    }

    private suspend fun updateLocalDatabase(sessions: List<GameSession>) {
        withContext(Dispatchers.IO) {
            try {
                val db = com.gamefocus.data.local.AppDatabase.getInstance(context)
                val dao = db.gameSessionDao()

                // Insert or update each session
                for (session in sessions) {
                    val existing = dao.getSessionById(session.id)
                    if (existing == null) {
                        dao.insertSession(session)
                    } else {
                        // Only update if remote is newer (based on duration as proxy)
                        if (session.durationMillis >= existing.durationMillis) {
                            dao.updateSession(session)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update local database: ${e.message}")
            }
        }
    }

    private suspend fun uploadResolvedConflicts(userId: String, sessions: List<GameSession>) {
        // Upload the final merged state back to Firestore to ensure consistency
        var uploaded = 0
        val total = sessions.size

        for (session in sessions) {
            try {
                val sessionMap = hashMapOf(
                    "packageName" to session.packageName,
                    "gameName" to session.gameName,
                    "startTime" to session.startTime,
                    "endTime" to session.endTime,
                    "durationMillis" to session.durationMillis,
                    "sessionDate" to session.sessionDate,
                    "focusModeStart" to session.focusModeStart,
                    "focusModeEnd" to session.focusModeEnd,
                    "updatedAt" to System.currentTimeMillis(),
                    "source" to "merged"
                )

                db.collection("users").document(userId)
                    .collection("sessions")
                    .document(session.id.toString())
                    .set(sessionMap, SetOptions.merge())
                    .await()

                uploaded++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload conflict resolution for session ${session.id}: ${e.message}")
            }
        }
    }

    suspend fun syncSessions(sessions: List<GameSession>) {
        triggerSync(sessions)
    }

    suspend fun fetchRemoteSessions(): List<GameSession> {
        val userId = _userId.value ?: return emptyList()
        return downloadRemoteSessions(userId)
    }

    fun signOut() {
        syncJob?.cancel()
        auth.signOut()
        CoroutineScope(Dispatchers.IO).launch {
            settingsManager.setSyncEnabled(false)
        }
        _isSignedIn.value = false
        _userId.value = null
        _lastSyncTime.value = null
        _syncStatus.value = SyncStatus.Idle
    }

    fun getLastSyncTimeFormatted(): String {
        val time = _lastSyncTime.value ?: return "Never"
        val now = System.currentTimeMillis()
        val diff = now - time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MINUTES.toMinutes(diff)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.HOURS.toHours(diff)} hours ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.DAYS.toDays(diff)} days ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(time))
            }
        }
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Offline : SyncStatus()
    object SigningIn : SyncStatus()
    object SignedIn : SyncStatus()
    object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class SyncProgress(
    val progress: Int,
    val message: String
)
