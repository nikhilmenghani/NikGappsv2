package com.nikgapps.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nikgapps.app.utils.NotificationUtility
import java.util.concurrent.TimeUnit

object AppUpdateManager {
    private const val PERIODIC_WORK = "nikgapps_periodic_update_check"
    private const val STARTUP_WORK = "nikgapps_startup_update_check"
    private const val DOWNLOAD_WORK = "nikgapps_app_update_download"

    fun scheduleChecks(context: Context, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        if (intervalHours <= 0) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).setConstraints(networkConstraints()).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun checkOnAppStart(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DOWNLOAD_WORK)
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(networkConstraints()).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            STARTUP_WORK, ExistingWorkPolicy.KEEP, request
        )
    }

    suspend fun announceUpdate(context: Context, version: String) {
        if (isValidVersion(version)) NotificationUtility.showUpdateAvailable(context, version)
    }

    internal fun isValidVersion(version: String) = AppUpdateBrowser.validVersion(version)

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED).build()
}
