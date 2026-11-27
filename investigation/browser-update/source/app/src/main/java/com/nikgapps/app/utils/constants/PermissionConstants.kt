package com.nikgapps.app.utils.constants

import android.Manifest
import android.os.Build
import android.provider.Settings
import com.nikgapps.app.data.PermissionInfo

object PermissionConstants {
    const val STORAGE = "Storage"
    const val NOTIFICATIONS = "Notifications"
//    const val LOCATION = "Location"

    val permissionMap: Map<String, PermissionInfo>
        get() = buildMap {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                put(NOTIFICATIONS, PermissionInfo(
                    permission = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    rationale = "Get build notifications and release alerts.",
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                ))
            }
            put(STORAGE, PermissionInfo(
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                },
                rationale = "Download packages and save ZIP builds.",
                action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                } else {
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                }
            ))
//        LOCATION to PermissionInfo(
//            permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//            rationale = "Location permission is required to access your location."
//        ),
        }
}
