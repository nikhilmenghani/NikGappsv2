package com.nikgapps.app.utils

import com.nikgapps.app.update.UpdateActionReceiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nikgapps.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nikgapps.R
import com.nikgapps.MainActivity

object NotificationUtility {

    const val CHANNEL_ID = "progress_channel_id"
    const val NOTIFICATION_ID = 1
    private const val UPDATE_NOTIFICATION_ID = 2202
    private const val UPDATE_CHANNEL_ID = "app_updates"

    fun startFileDownload(context: Context) {
        createNotificationChannel(context)

        val totalProgress = 100

        // Simulate a download with a coroutine (replace with actual logic)
        CoroutineScope(Dispatchers.IO).launch {
            for (progress in 0..totalProgress step 10) {
                delay(500) // Simulate download delay
                showProgressNotification(context, progress)
            }
        }
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    fun showProgressNotification(
        context: Context = App.globalClass,
        progress: Int,
        progressText: String = "Download in progress",
        channelId: String = CHANNEL_ID,
        contentTitle: String = "File Download",
        priority: Int = NotificationCompat.PRIORITY_HIGH,
        completeText: String = "Download complete",
        notificationId: Int = NOTIFICATION_ID
    ) {
        val notificationManager = NotificationManagerCompat.from(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(contentTitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(priority)
            .setOnlyAlertOnce(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Ensures sound, vibration, etc.
            .setCategory(NotificationCompat.CATEGORY_PROGRESS) // Explicitly set category

        if (progress < 100) {
            builder.setContentText("$progressText: $progress%")
                .setProgress(100, progress, false)
        } else {
            notificationManager.cancel(notificationId)
            builder.setContentText(completeText)
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
        }
        notificationManager.notify(notificationId, builder.build())
    }


    fun createNotificationChannel(
        context: Context = App.globalClass,
        name: String = "Progress Channel",
        descriptionText: String = "Notification channel for progress updates",
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        channelId: String = CHANNEL_ID
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_IMMUTABLE
    )

    fun showUpdateAvailable(context: Context, version: String) = postUpdate(
        context, updateNotification(context)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("NikGapps v$version is available")
            .setContentText("Open the APK download in your browser.")
            .addAction(android.R.drawable.stat_sys_download, "Update", downloadIntent(context, version))
    )

    fun showUpdateProgress(context: Context, version: String, status: String, progress: Int? = null) = postUpdate(
        context, updateNotification(context)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading NikGapps v$version")
            .setContentText(status)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setProgress(100, progress ?: 0, progress == null)
    )

    fun showUpdateFailed(context: Context, version: String) = postUpdate(
        context, updateNotification(context)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Couldn't download NikGapps v$version")
            .setContentText("Tap Update to open the APK download in your browser.")
            .addAction(android.R.drawable.stat_sys_download, "Update", downloadIntent(context, version))
    )

    // Compatibility for an update worker persisted by an older app version.
    fun showUpdateReady(context: Context, version: String) = showUpdateAvailable(context, version)

    private fun downloadIntent(context: Context, version: String): PendingIntent = PendingIntent.getActivity(
        context, version.hashCode(), runCatching { com.nikgapps.app.update.AppUpdateBrowser.intent(context, version) }.getOrElse {
            Intent(context, MainActivity::class.java).apply {
                action = UpdateActionReceiver.ACTION_DOWNLOAD
                putExtra(UpdateActionReceiver.EXTRA_VERSION, version)
            }
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun clearUpdateNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(UPDATE_NOTIFICATION_ID)
    }

    private fun updateNotification(context: Context) = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
        .setContentIntent(openAppIntent(context)).setOnlyAlertOnce(true)

    @SuppressLint("MissingPermission")
    private fun postUpdate(context: Context, notification: NotificationCompat.Builder) {
        createNotificationChannel(context, "App updates", "New versions and installation status",
            NotificationManager.IMPORTANCE_DEFAULT, UPDATE_CHANNEL_ID)
        runCatching { NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification.build()) }
    }
}

