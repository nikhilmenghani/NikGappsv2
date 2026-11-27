package com.nikgapps.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.nikgapps.BuildConfig
import com.nikgapps.app.utils.NotificationUtility
import com.nikgapps.app.utils.network.VersionFetcher
import java.net.URI

/** Public APK links only. No token, file URI or package-install permission is sent to the browser. */
object AppUpdateBrowser {
    internal fun validVersion(version: String) = Regex("[0-9]+(?:\\.[0-9]+){1,2}").matches(version)

    internal fun trustedDownloadUrl(version: String, resolvedUrl: String, development: Boolean): String {
        require(validVersion(version)) { "Invalid update version" }
        val uri = runCatching { URI(resolvedUrl) }.getOrNull()
        val tag = if (development) "dev-v$version" else "v$version"
        val expected = "/${VersionFetcher.RELEASE_REPOSITORY}/releases/download/$tag/"
        return if (uri != null && uri.scheme == "https" && uri.host == "github.com" &&
            uri.port == -1 && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.rawPath.startsWith(expected) && uri.rawPath.endsWith(".apk")) resolvedUrl
        else VersionFetcher.artifactUrl(VersionFetcher.RELEASE_REPOSITORY, version, development)
    }

    fun intent(context: Context, version: String): Intent {
        val url = trustedDownloadUrl(version, VersionFetcher.downloadUrl(version), BuildConfig.DEBUG)
        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE)
        val manager = context.packageManager
        val browsers = manager.queryIntentActivities(generic, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            .distinctBy { it.activityInfo.packageName }
        val preferred = manager.resolveActivity(generic, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.takeIf { name -> browsers.any { it.activityInfo.packageName == name } }
        fun target(name: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (preferred != null) return target(preferred)
        if (browsers.size == 1) return target(browsers.single().activityInfo.packageName)
        // Launching an HTTPS intent does not require package-query visibility.
        // Some Android builds hide browsers from queries even when they can open links.
        if (browsers.isEmpty()) return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val intents = browsers.map { target(it.activityInfo.packageName) }
        return Intent.createChooser(intents.first(), "Open with browser").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun open(context: Context, version: String): Boolean = try {
        context.startActivity(intent(context, version))
        NotificationUtility.clearUpdateNotification(context)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No browser is available. Install or enable a browser to download the update.", Toast.LENGTH_LONG).show()
        false
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open the update download. Please try again.", Toast.LENGTH_LONG).show()
        false
    }
}
