package com.gamefocus.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

object UsagePermissionHelper {
    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
        val mode = appOps?.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            context.packageName
        ) ?: android.app.AppOpsManager.MODE_ERRORED
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun openUsageSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
