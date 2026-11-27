package com.nikgapps

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.ui.component.LocalSystemStartupLogo
import com.nikgapps.app.presentation.navigation.ScreenNavigator
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.security.AppLock
import com.nikgapps.app.security.canUseAppLock
import com.nikgapps.app.update.AppUpdateManager
import com.nikgapps.app.update.UpdateActionReceiver
import com.nikgapps.app.analytics.AppAnalytics
import com.nikgapps.app.utils.AppDiagnostics
import android.widget.Toast
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.info("app", "launched", mapOf(
            "version" to BuildConfig.VERSION_NAME,
            "androidApi" to Build.VERSION.SDK_INT
        ))
        AppAnalytics.track("app_opened", mapOf(
            "app_version" to BuildConfig.VERSION_NAME,
            "android_api" to Build.VERSION.SDK_INT
        ))
        if (handleUpdateIntent(intent)) { finish(); return }
        if (Permissions.hasAllRequiredPermissions(this)) {
            val startupIcon = mutableStateOf<ImageBitmap?>(null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                splashScreen.setOnExitAnimationListener { splash ->
                    val icon = splash.iconView
                    val width = icon?.width ?: 0
                    val height = icon?.height ?: 0
                    if (icon != null && width > 0 && height > 0) {
                        startupIcon.value = runCatching {
                            val picture = Picture()
                            try {
                                icon.draw(picture.beginRecording(width, height))
                            } finally {
                                picture.endRecording()
                            }
                            Bitmap.createBitmap(picture).asImageBitmap()
                        }.getOrNull()
                    }
                    // Keep the platform splash over the first frame using the captured icon.
                    val decor = window.decorView
                    decor.postOnAnimation { decor.postOnAnimation { splash.remove() } }
                }
            }
            setContent {
                CompositionLocalProvider(LocalSystemStartupLogo provides startupIcon.value) {
                    NikGappsTheme {
                        AppLock(
                            enabled = globalClass.preferencesManager.displayPrefs.biometricLockEnabled && canUseAppLock(this@MainActivity),
                            activity = this@MainActivity
                        ) { ScreenNavigator() }
                    }
                }
            }
        } else {
            // Launch PermissionsActivity if any permissions are missing
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        AppUpdateManager.checkOnAppStart(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleUpdateIntent(updateIntent: Intent): Boolean {
        if (updateIntent.action !in setOf(UpdateActionReceiver.ACTION_DOWNLOAD, UpdateActionReceiver.ACTION_INSTALL)) return false
        val version = updateIntent.getStringExtra(UpdateActionReceiver.EXTRA_VERSION)
            ?: updateIntent.getStringExtra(UpdateActionReceiver.EXTRA_APK_PATH)?.let {
                File(it).name.removePrefix("NikGapps-v").removeSuffix(".apk")
            } ?: com.nikgapps.app.utils.network.VersionFetcher.lastKnownVersion()
        updateIntent.action = null
        if (version != null) com.nikgapps.app.update.AppUpdateBrowser.open(this, version)
        else Toast.makeText(this, "Check for updates again to get the latest download.", Toast.LENGTH_SHORT).show()
        return true
    }

    fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
