package com.gamefocus.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val syncManager = FirebaseSyncManager.getInstance(applicationContext)
            val currentStatus = syncManager.syncStatus.value
            if (currentStatus is SyncStatus.Error || currentStatus is SyncStatus.Offline) {
                val sessions = try {
                    com.gamefocus.data.local.AppDatabase.getInstance(applicationContext)
                        .gameSessionDao()
                        .getAllSessionsList()
                } catch (e: Exception) {
                    emptyList()
                }
                syncManager.triggerSync(sessions)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "gamefocus_sync_retry"
    }
}
