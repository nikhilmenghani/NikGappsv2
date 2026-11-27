package com.nikgapps.browsercheck

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikgapps.PermissionsActivity
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.ui.component.*
import com.nikgapps.app.update.*
import com.nikgapps.app.utils.NotificationUtility
import com.nikgapps.app.utils.network.VersionFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

/** Separate signed test APK only; real components and browser handoff, no production test hooks. */
class BrowserUpdateInstrumentation : Instrumentation() {
    private lateinit var parameters: Bundle
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); parameters = arguments ?: Bundle(); start() }
    override fun onStart() {
        Thread {
            var activity: ComponentActivity? = null
            try {
                val mode = parameters.getString("mode") ?: "gate"
                check(mode in setOf("gate", "home", "notification"))
                val latest = runBlocking(Dispatchers.IO) { VersionFetcher.fetchLatestVersion() }
                check(latest == parameters.getString("expectedVersion")) { "Latest published version differs; inspect release lookup" }
                val browser = AppUpdateBrowser.intent(targetContext, latest)
                check(browser.action == Intent.ACTION_VIEW && browser.data?.scheme == "https")
                check(browser.`package` != null && browser.`package` != targetContext.packageName)
                check(browser.extras == null && browser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0)
                val clicked = AtomicBoolean(false)
                val host = startActivitySync(Intent(targetContext, PermissionsActivity::class.java)
                    .putExtra(PermissionsActivity.EXTRA_REVIEW_MODE, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
                activity = host
                runOnMainSync {
                    host.setContent {
                        NikGappsTheme { ProjectsBackdrop {
                            if (mode == "gate") MandatoryUpdateScreen(false, "0.81", latest, emptyList(),
                                onUpdate = { clicked.set(AppUpdateBrowser.open(host, latest)) }, onOpenSettings = {})
                            else if (mode == "home") {
                                var show by remember { mutableStateOf(false) }
                                Box(Modifier.fillMaxSize().padding(24.dp)) {
                                    HomeUpdateIndicator(latest, false, onClick = { show = true })
                                    if (show) ChangelogDialog("What’s new in $latest", listOf(ChangelogEntry(latest, listOf("App update available."))),
                                        onDismiss = { show = false }, onUpdate = { show = false; clicked.set(AppUpdateBrowser.open(host, latest)) })
                                }
                            } else {
                                androidx.compose.material3.Text("Update notification test", Modifier.padding(24.dp))
                            }
                        } }
                    }
                    if (mode == "notification") NotificationUtility.showUpdateAvailable(host, latest)
                }
                sendStatus(1, Bundle().apply { putString("browser_check", "ready:" + mode); putString("version", latest) })
                if (mode == "notification") SystemClock.sleep(30_000)
                else {
                    val deadline = SystemClock.elapsedRealtime() + 180_000
                    while (!clicked.get() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
                    check(clicked.get()) { "Actual update button did not open a browser" }
                }
                finish(Activity.RESULT_OK, Bundle().apply {
                    putString("browser_check", if (mode == "notification") "notification_posted" else "button_opened_browser")
                    putString("browser_package", browser.`package`)
                    putString("download_url", browser.dataString)
                })
            } catch (_: Throwable) {
                finish(Activity.RESULT_CANCELED, Bundle().apply { putString("browser_check", "failed; no credentials captured") })
            } finally {
                activity?.let { host -> runOnMainSync { host.finish() } }
            }
        }.start()
    }
}
