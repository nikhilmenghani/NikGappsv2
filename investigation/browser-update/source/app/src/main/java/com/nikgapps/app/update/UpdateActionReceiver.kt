package com.nikgapps.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DOWNLOAD) return
        val version = intent.getStringExtra(EXTRA_VERSION) ?: return
        if (AppUpdateManager.isValidVersion(version))
            com.nikgapps.app.utils.NotificationUtility.showUpdateAvailable(context, version)

    }

    companion object {
        const val ACTION_DOWNLOAD = "com.nikgapps.action.DOWNLOAD_UPDATE"
        const val ACTION_INSTALL = "com.nikgapps.action.INSTALL_UPDATE"
        const val EXTRA_VERSION = "update_version"
        const val EXTRA_APK_PATH = "update_apk_path"
    }
}
