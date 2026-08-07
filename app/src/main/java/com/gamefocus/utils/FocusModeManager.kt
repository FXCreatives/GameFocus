package com.gamefocus.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusModeManager(private val context: Context) {
    private val _isFocusModeOn = MutableStateFlow(false)
    val isFocusModeOn: StateFlow<Boolean> = _isFocusModeOn.asStateFlow()

    private val _previousInterruptionFilter = MutableStateFlow<Int?>(null)
    private var previousNotificationPolicy: android.app.NotificationManager.Policy? = null
    private var focusStartTime: Long = 0L

    fun hasDndAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.isNotificationPolicyAccessGranted ?: false
        } else {
            true
        }
    }

    fun openDndSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun enableFocusMode() {
        if (!hasDndAccess()) {
            _isFocusModeOn.value = false
            return
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                _previousInterruptionFilter.value = notificationManager.currentInterruptionFilter

                // Save previous notification policy (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    previousNotificationPolicy = notificationManager.notificationPolicy
                }

                // Use PRIORITY filter instead of NONE to preserve media audio
                notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)

                // Configure notification policy to suppress notifications while allowing media
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val currentPolicy = notificationManager.notificationPolicy
                        val suppressedEffects = android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF or android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON
                        val newPolicy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            android.app.NotificationManager.Policy(
                                currentPolicy.priorityCategories,
                                0, // PRIORITY_SENDERS_NONE
                                0, // PRIORITY_SENDERS_NONE
                                currentPolicy.priorityConversationSenders,
                                suppressedEffects
                            )
                        } else {
                            android.app.NotificationManager.Policy(
                                currentPolicy.priorityCategories,
                                0, // PRIORITY_SENDERS_NONE
                                0, // PRIORITY_SENDERS_NONE
                                suppressedEffects
                            )
                        }
                        notificationManager.notificationPolicy = newPolicy
                    } catch (e: Exception) {
                        // Policy modification not supported on this device; interruption filter still active
                    }
                }

                _isFocusModeOn.value = true
                focusStartTime = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            _isFocusModeOn.value = false
        }
    }

    fun disableFocusMode() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                val previousMode = _previousInterruptionFilter.value
                if (previousMode != null && previousMode != android.app.NotificationManager.INTERRUPTION_FILTER_NONE) {
                    notificationManager.setInterruptionFilter(previousMode)
                }

                // Restore previous notification policy
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    previousNotificationPolicy?.let { policy ->
                        try {
                            notificationManager.notificationPolicy = policy
                        } catch (e: Exception) {
                            // Restore failed; ignore to avoid crashing
                        }
                    }
                }

                _isFocusModeOn.value = false
            }
        } catch (e: Exception) {
            _isFocusModeOn.value = false
        }
    }

    fun getFocusDuration(): Long {
        return if (_isFocusModeOn.value && focusStartTime > 0) {
            System.currentTimeMillis() - focusStartTime
        } else {
            0L
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }
        return true
    }

    fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback
                val intentSettings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intentSettings)
            }
        }
    }
}