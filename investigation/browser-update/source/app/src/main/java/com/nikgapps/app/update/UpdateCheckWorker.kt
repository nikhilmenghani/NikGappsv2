package com.nikgapps.app.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikgapps.app.utils.network.VersionFetcher
import com.nikgapps.dumps.getCurrentVersion

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val latest = VersionFetcher.fetchLatestVersion()
        if (latest == "Unknown") return Result.retry()
        if (!VersionFetcher.isNewer(latest, getCurrentVersion(applicationContext))) {
            com.nikgapps.app.utils.NotificationUtility.clearUpdateNotification(applicationContext)
            return Result.success()
        }
        AppUpdateManager.announceUpdate(applicationContext, latest)
        return Result.success()
    }

}
