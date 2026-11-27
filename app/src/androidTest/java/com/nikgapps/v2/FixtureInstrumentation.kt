package com.nikgapps.v2

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Temporary component views and a pinned public transport check. No storage writes, credentials or arbitrary commands. */
class FixtureInstrumentation : Instrumentation() {
    private var arguments = Bundle()
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments ?: Bundle()
        start()
    }
    override fun onStart() {
        check(targetContext.packageName == "com.nikgapps.v2")
        val scenario = arguments.getString("scenario", "outdated")
        check(scenario in setOf("outdated", "outdated-long", "network", "catalog", "cache"))
        val theme = arguments.getString("theme", "dark")
        check(theme in setOf("dark", "light", "classic"))
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        if (scenario == "cache") {
            val directory = java.io.File(targetContext.cacheDir, "cache-component")
            directory.deleteRecursively()
            try {
                val repo = CatalogRepository(directory)
                val network = repo.load()
                check(network.source == MetadataSource.NETWORK)
                check(repo.load().source == MetadataSource.CACHE)
                val offline = RegistryHttp(okhttp3.OkHttpClient.Builder().addInterceptor { throw java.io.IOException("Controlled transport failure") }.build())
                check(CatalogRepository(directory, offline).load(forceRefresh = true).source == MetadataSource.FALLBACK)
                val version = network.catalog.packages.flatMap { it.versions.values }.first { v ->
                    (v.android.minApi == null || v.android.minApi <= 37) && (v.android.maxApi == null || v.android.maxApi >= 37) &&
                    (v.architectures.isEmpty() || "arm64-v8a" in v.architectures) && (v.deviceTypes.isEmpty() || "phone" in v.deviceTypes)
                }
                validateCompatibility("fixture", version, 37, "arm64-v8a")
                try { validateCompatibility("fixture", version.copy(architectures = listOf("unsupported")), 37, "arm64-v8a"); error("Architecture accepted") }
                catch (_: RegistryException.UnsupportedArchitecture) { }
                java.io.File(directory, "snapshot.json").writeText("broken")
                check(CatalogRepository(directory, offline).cached() == null)
                try { CatalogRepository(directory, offline).load(); error("Broken cache accepted") }
                catch (_: java.io.IOException) { }
            } finally { directory.deleteRecursively() }
            finish(Activity.RESULT_OK, Bundle().apply { putString("cache_status", "passed: network, fresh cache, fallback, corrupt rejection and compatibility") })
            return
        }
        if (scenario == "catalog") {
            val snapshot = CatalogRepository(java.io.File(targetContext.cacheDir, "catalog-component")).load(forceRefresh = true)
            check(snapshot.catalog.packages.size == 75 && snapshot.appSets.appSets.size == 49)
            finish(Activity.RESULT_OK, Bundle().apply {
                putString("catalog_status", "passed: pinned checksums, schema and references")
                putInt("packages", snapshot.catalog.packages.size)
                putInt("appsets", snapshot.appSets.appSets.size)
            })
            return
        }
        if (scenario == "network") {
            val connection = NetworkConnection(targetContext)
            try {
                check(connection.state.value == ConnectionState.ONLINE) { "Active network is not validated" }
                connection.start()
            } finally { connection.close() }
            val http = RegistryHttp()
            val bytes = http.get(RegistryPin.INDEX)
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            check(digest == RegistryPin.INDEX_SHA256)
            try { http.get(RegistryPin.INDEX + ".missing-fixture"); error("Missing document unexpectedly succeeded") }
            catch (failure: NetworkResponseException) { check(failure.statusCode == 404) }
            try { http.get("http://example.com/"); error("Cleartext unexpectedly allowed") }
            catch (_: IllegalArgumentException) { }
            sendStatus(1, Bundle().apply {
                putString("network", "validated HTTPS read, pinned SHA, HTTP404 and cleartext rejection passed")
                putString("sha256", digest)
            })
        }
        val projects = (1..if (scenario == "outdated-long") 18 else 5).map { index -> BuildProject(id = "fixture-$index", name = "Fixture project $index",
            androidVersion = AndroidVersion.ANDROID_17, createdAt = 1795737600000L) }
        val unavailable = projects.drop(2).associate { it.id to setOf("retired-fixture-package") }
        runOnMainSync {
            activity.setContent {
                CompositionLocalProvider(LocalGlassEnabled provides (theme != "classic"),
                    LocalGlassBackground provides GlassBackground.GOOGLE, LocalRadialProjectMenu provides false) {
                    AppTheme(activity, if (theme == "light") 0 else 1, dynamicColor = false, ready = true) {
                        ProjectsBackdrop {
                            Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
                                topBar = { Text("Projects", Modifier.statusBarsPadding().padding(16.dp), style = MaterialTheme.typography.headlineSmall) },
                                bottomBar = { Text("Component test", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                                    style = MaterialTheme.typography.labelMedium) }) { padding ->
                                Box(Modifier.fillMaxSize().padding(padding)) {
                                    if (scenario == "network") {
                                        var recovered by remember { mutableStateOf(false) }
                                        if (recovered) Text("Connection restored", Modifier.padding(24.dp))
                                        else OfflineConnectionCard(onRetry = { recovered = true }, onSettings = {
                                            activity.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                                        })
                                    } else LocalProjects(projects, onEdit = {}, onCopy = {}, onDelete = {}, unavailable = unavailable)
                                }
                            }
                        }
                    }
                }
            }
        }
        sendStatus(1, Bundle().apply { putString("fixture", "ready") })
        CountDownLatch(1).await(15, TimeUnit.MINUTES)
        finish(Activity.RESULT_OK, Bundle().apply { putString("fixture", "finished") })
    }
}
