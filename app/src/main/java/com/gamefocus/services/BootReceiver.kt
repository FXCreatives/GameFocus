package com.gamefocus.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, starting GameDetectionService")
            
            val settingsManager = SettingsManager(context)
            val db = AppDatabase.getInstance(context)
            val gameDao = db.gameDao()
            
            // Check if detection was enabled before boot
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val detectionEnabled = settingsManager.detectionEnabled.first()
                    if (detectionEnabled) {
                        GameDetectionService.startService(context)
                        Log.d(TAG, "Service started after boot")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service after boot", e)
                }
            }
        }
    }
}
