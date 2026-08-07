package com.gamefocus.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gamefocus.MainActivity
import com.gamefocus.R
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.GameEntity
import com.gamefocus.data.local.SessionManager
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.data.repository.AchievementChecker
import com.gamefocus.data.repository.AnalyticsRepository
import com.gamefocus.data.sync.FirebaseSyncManager
import com.gamefocus.utils.FocusModeManager
import com.gamefocus.utils.UsagePermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameDetectionService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var sessionManager: SessionManager
    private lateinit var focusModeManager: FocusModeManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var firebaseSyncManager: FirebaseSyncManager
    private var currentForegroundPackage: String? = null
    private var pendingForegroundPackage: String? = null
    private var focusModeConfirmCount = 0
    private val FOCUS_MODE_CONFIRM_THRESHOLD = 2
    private var isDetectionRunning = false
    private var cachedGames: List<GameEntity> = emptyList()
    private var sessionStartTime: Long = 0L
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "game_detection_channel"
        const val ACTION_START = "com.gamefocus.START_DETECTION"
        const val ACTION_STOP = "com.gamefocus.STOP_DETECTION"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, GameDetectionService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GameDetectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val db = AppDatabase.getInstance(this)
        val achievementDao = db.achievementDao()
        val achievementChecker = AchievementChecker(achievementDao, AnalyticsRepository(db.gameSessionDao()))
        firebaseSyncManager = FirebaseSyncManager.getInstance(this)
        sessionManager = SessionManager.getInstance(this, db.gameSessionDao(), achievementChecker)
        focusModeManager = FocusModeManager(this)
        settingsManager = SettingsManager(this)
        DetectionManager.updateServiceStatus(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                if (!isDetectionRunning) {
                    observeSettingsAndRun()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
            null -> {
                startForegroundService()
                if (!isDetectionRunning) {
                    observeSettingsAndRun()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification(null, null, 0L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeSettingsAndRun() {
        isDetectionRunning = true
        serviceScope.launch {
            launch {
                val db = AppDatabase.getInstance(this@GameDetectionService)
                db.gameDao().getAllGames().collect { games ->
                    cachedGames = games
                }
            }

            settingsManager.detectionEnabled.collectLatest { enabled ->
                if (enabled) {
                    runDetectionLoop()
                } else {
                    DetectionManager.updateForegroundApp("Disabled", "None")
                }
            }
        }
    }

    private suspend fun runDetectionLoop() {
        while (isDetectionRunning) {
            val autoFocus = settingsManager.autoFocusEnabled.first()
            checkForegroundApp(autoFocus)
            val jitter = (0L..500L).random()
            delay(3000L + jitter)
        }
    }

    private suspend fun checkForegroundApp(autoFocus: Boolean) {
        if (!UsagePermissionHelper.hasUsagePermission(this)) {
            Log.w("GameDetectionService", "Missing Usage Access Permission")
            DetectionManager.updateForegroundApp("No Permission", "None")
            return
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
        if (usageStatsManager == null) {
            DetectionManager.updateForegroundApp("No Permission", "None")
            return
        }
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // Look back 10 seconds
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        val event = android.app.usage.UsageEvents.Event()
        var lastMovedPackage: String? = null

        if (usageEvents != null) {
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastMovedPackage = event.packageName
                }
            }
        }

        var newForeground = lastMovedPackage

        if (newForeground == null) {
            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            val mostRecent = stats?.filter { it.lastTimeUsed in startTime..endTime }
                ?.maxByOrNull { it.lastTimeUsed }
            newForeground = mostRecent?.packageName
        }

        if (newForeground != null && newForeground != currentForegroundPackage) {
            val newIsGame = cachedGames.any { it.packageName == newForeground }
            
            if (newIsGame) {
                // Game detected - immediately switch to it
                focusModeConfirmCount = 0
                pendingForegroundPackage = null
                handleAppChange(currentForegroundPackage, newForeground, autoFocus)
                currentForegroundPackage = newForeground
            } else {
                // Non-game detected - require confirmation
                if (pendingForegroundPackage == newForeground) {
                    focusModeConfirmCount++
                    if (focusModeConfirmCount >= FOCUS_MODE_CONFIRM_THRESHOLD) {
                        // Confirmed non-game foreground
                        handleAppChange(currentForegroundPackage, newForeground, autoFocus)
                        currentForegroundPackage = newForeground
                        pendingForegroundPackage = null
                        focusModeConfirmCount = 0
                    }
                } else {
                    // New potential non-game foreground
                    pendingForegroundPackage = newForeground
                    focusModeConfirmCount = 1
                }
            }
        } else if (newForeground != null && newForeground == currentForegroundPackage) {
            // Same foreground as before - if it's a game, keep session alive
            // If it's a non-game and we're tracking it, continue confirming
            val currentIsGame = cachedGames.any { it.packageName == newForeground }
            if (!currentIsGame && pendingForegroundPackage == newForeground) {
                focusModeConfirmCount++
                if (focusModeConfirmCount >= FOCUS_MODE_CONFIRM_THRESHOLD) {
                    // This shouldn't normally happen since we already switched,
                    // but handle it for edge cases
                    handleAppChange(currentForegroundPackage, newForeground, autoFocus)
                    currentForegroundPackage = newForeground
                    pendingForegroundPackage = null
                    focusModeConfirmCount = 0
                }
            } else if (currentIsGame) {
                // Still on a game - reset any pending non-game tracking
                focusModeConfirmCount = 0
                pendingForegroundPackage = null
            }
        }
    }

    private suspend fun handleAppChange(oldPackage: String?, newPackage: String, autoFocus: Boolean) {
        val games = cachedGames
        
        val wasGame = oldPackage != null && games.any { it.packageName == oldPackage }

        if (oldPackage != null) {
            sessionManager.endCurrentSession()
            if (wasGame) {
                focusModeManager.disableFocusMode()
                DetectionManager.updateFocusMode(false)
            }
            stopTimerUpdates()
            updateForegroundNotification(null, null, 0L)
        }

        val game = games.find { it.packageName == newPackage }
        if (game != null) {
            sessionManager.startSession(newPackage, game.appName)
            sessionStartTime = System.currentTimeMillis()
            if (autoFocus) {
                focusModeManager.enableFocusMode()
                DetectionManager.updateFocusMode(true)
            }
            DetectionManager.updateForegroundApp(newPackage, game.appName)
            val icon = getGameIconBitmap(newPackage)
            updateForegroundNotification(game.appName, icon, 0L)
            startTimerUpdates(newPackage, icon)
        } else {
            DetectionManager.updateForegroundApp(newPackage, "None")
            stopTimerUpdates()
            updateForegroundNotification(null, null, 0L)
        }
    }

    private fun startTimerUpdates(packageName: String, icon: Bitmap?) {
        stopTimerUpdates()
        timerJob = serviceScope.launch {
            while (isDetectionRunning && currentForegroundPackage == packageName) {
                val elapsed = System.currentTimeMillis() - sessionStartTime
                updateForegroundNotification(
                    cachedGames.find { it.packageName == packageName }?.appName,
                    icon,
                    elapsed
                )
                delay(1000)
            }
        }
    }

    private fun stopTimerUpdates() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateForegroundNotification(gameName: String?, largeIcon: Bitmap?, elapsedMillis: Long) {
        val notification = createNotification(gameName, largeIcon, elapsedMillis)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDetectionRunning = false
        stopTimerUpdates()
        serviceJob.cancel()
        focusModeManager.disableFocusMode()
        DetectionManager.updateServiceStatus(false)
        DetectionManager.updateFocusMode(false)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Game Detection Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Running in background to detect games"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(gameName: String?, largeIcon: Bitmap?, elapsedMillis: Long): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (!gameName.isNullOrBlank()) {
            "🎮 $gameName detected"
        } else {
            "GameFocus Active"
        }

        val content = if (!gameName.isNullOrBlank()) {
            "Focus Mode Active • ${formatElapsedTime(elapsedMillis)}"
        } else {
            "Monitoring gameplay..."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun formatElapsedTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    private fun getGameIconBitmap(packageName: String): Bitmap? {
        return try {
            val pm = packageManager
            val icon = pm.getApplicationIcon(packageName)
            val drawable = icon as? android.graphics.drawable.BitmapDrawable
            drawable?.bitmap ?: drawableToBitmap(icon)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
