package com.nikgapps.v2

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Temporary component views and a pinned public transport check. Whitelisted component checks; credential transfer is encrypted and ephemeral. */
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
        check(scenario in setOf("outdated", "outdated-long", "network", "catalog", "cache", "resolution", "credential-key", "auth", "icons-data", "icons-view", "icons-cleanup", "account-token"))
        if (scenario == "credential-key") {
            finish(Activity.RESULT_OK, Bundle().apply { putString("transfer_public_key", CredentialTransfer.publicKey(targetContext)) })
            return
        }
        if (scenario == "account-token") { checkManualTokenUi(this); return }
        if (scenario == "icons-cleanup") {
            CredentialTransfer.clear(targetContext)
            java.io.File(targetContext.cacheDir, "icons-component").deleteRecursively()
            finish(Activity.RESULT_OK, Bundle().apply { putString("icons_cleanup", "passed") })
            return
        }
        if (scenario == "icons-data") {
            val directory = java.io.File(targetContext.cacheDir, "icons-component")
            directory.deleteRecursively()
            var runtimeToken = CredentialTransfer.take(targetContext)
            RegistryDelivery.userToken = { runtimeToken }
            try {
                val repo = RegistryIconRepository(directory)
                val chrome = checkNotNull(repo.iconBlocking("com.android.chrome"))
                check(chrome.length() == 10871L)
                check(checkNotNull(repo.iconBlocking("com.google.android.gms")).length() == 7253L)
                check(repo.iconBlocking("com.nikgapps.missing.fixture") == null)
                var requests = 0
                val offline = RegistryHttp(okhttp3.OkHttpClient.Builder().addInterceptor {
                    requests++; throw java.io.IOException("Controlled offline icon check")
                }.build(), maxAttempts = 1)
                val cached = RegistryIconRepository(directory, offline)
                check(cached.iconBlocking("com.android.chrome") == chrome && requests == 0)
                chrome.writeText("broken")
                check(cached.iconBlocking("com.android.chrome") == null && requests == 1)
                check(checkNotNull(repo.iconBlocking("com.android.chrome")).length() == 10871L)
            } finally { runtimeToken = ""; RegistryDelivery.userToken = { "" }; CredentialTransfer.clear(targetContext) }
            finish(Activity.RESULT_OK, Bundle().apply { putString("icons_status", "passed: pinned Chrome/Gms, verified offline cache, corrupt rejection and standard fallback") })
            return
        }
        if (scenario == "auth") {
            val directory = java.io.File(targetContext.cacheDir, "auth-component")
            directory.deleteRecursively()
            try {
                var runtimeToken = CredentialTransfer.take(targetContext)
                RegistryDelivery.userToken = { runtimeToken }
                try {
                    val repo = ReleaseRepository(directory)
                    for (android in listOf("16", "17")) {
                        val metadata = repo.load(android)
                        check(metadata.release.androidVersion == android)
                        check(metadata.catalog.packages.size == 75)
                    }
                    val limit = checkNotNull(RegistryRequestLimits.latest.value)
                    check(limit.remaining != null && limit.limit != null && limit.status == 200)
                    sendStatus(1, Bundle().apply { putString("authenticated_metadata", "passed: pinned Android16/17 via repository API"); putBoolean("api_limit_headers", true) })
                    // A separate controlled client exercises real cooldown persistence and rejection without exhausting GitHub.
                    val prefs = targetContext.getSharedPreferences("investigation-cooldown", 0)
                    var clock = 100_000L
                    var calls = 0
                    val cooldown = RegistryCooldown { clock }.apply { restore(0) { prefs.edit().putLong("retry", it ?: 0).commit() } }
                    val limited = RegistryHttp(okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                        calls++
                        okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(403)
                            .message("Controlled rate limit").header("Retry-After", "10")
                            .body("fixture".toResponseBody()).build()
                    }.build(), cooldown, pause = { error("Cooldown must not retry") })
                    try { limited.get(RegistryDelivery.contents("catalog.json")); error("Cooldown expected") }
                    catch (_: RegistryRateLimitedException) { }
                    check(calls == 1)
                    val restored = RegistryCooldown { clock }.apply { restore(prefs.getLong("retry", 0)) {} }
                    try { restored.requireAvailable(RegistryDelivery.contents("catalog.json").toHttpUrl()); error("Restored cooldown expected") }
                    catch (_: RegistryRateLimitedException) { }
                    clock += 11_000
                    restored.requireAvailable(RegistryDelivery.contents("catalog.json").toHttpUrl())
                    prefs.edit().clear().commit()
                } finally { runtimeToken = ""; RegistryDelivery.userToken = { "" } }
            } finally { directory.deleteRecursively(); CredentialTransfer.clear(targetContext) }
            finish(Activity.RESULT_OK, Bundle().apply { putString("auth_status", "passed: authenticated pinned data, limit observation and persisted cooldown") })
            return
        }
        val theme = arguments.getString("theme", "dark")
        check(theme in setOf("dark", "light", "classic"))
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        if (scenario == "icons-view") {
            val latch = CountDownLatch(1)
            val requests = java.util.concurrent.atomic.AtomicInteger()
            val repo = RegistryIconRepository(java.io.File(targetContext.cacheDir, "icons-component"),
                RegistryHttp(okhttp3.OkHttpClient.Builder().addInterceptor { requests.incrementAndGet(); throw java.io.IOException("Controlled offline icon UI") }.build(), maxAttempts = 1))
            runOnMainSync {
                activity.setContent {
                    CompositionLocalProvider(LocalGlassEnabled provides (theme != "classic"), LocalGlassBackground provides GlassBackground.GOOGLE) {
                        AppTheme(activity, if (theme == "light") 0 else 1, dynamicColor = false, ready = true) {
                            ProjectsBackdrop {
                                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("App icons", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                                    for ((name, id) in listOf("Chrome" to "com.android.chrome", "Google Play services" to "com.google.android.gms", "Standard fallback" to "com.nikgapps.missing.fixture")) {
                                        ProjectGlassSurface(Modifier.fillMaxWidth()) {
                                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                PackageIcon(id, Modifier.size(40.dp), repo)
                                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    HorizontalDivider()
                                    GlassButton(onClick = { latch.countDown() }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                                }
                            }
                        }
                    }
                }
            }
            sendStatus(1, Bundle().apply { putString("icons_view", "ready") })
            check(latch.await(90, TimeUnit.SECONDS)) { "Icon component view timed out" }
            check(requests.get() == 0) { "Cached icon UI attempted a request" }
            finish(Activity.RESULT_OK, Bundle().apply { putString("icons_view", "passed") })
            return
        }
        if (scenario == "resolution") {
            val directory = java.io.File(targetContext.cacheDir, "resolution-component")
            directory.deleteRecursively()
            try {
                val repo = ReleaseRepository(directory)
                for (android in listOf("16", "17")) {
                    val metadata = repo.load(android)
                    val core = metadata.appSets.appSets.first { it.id == "core" }
                    val selected = metadata.catalog.publicPackages(core).map { it.id }.toSet()
                    val result = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolve(core.id, selected,
                        ReleaseChannel.STABLE, emptyMap(), metadata.release.platformApi, "arm64-v8a")
                    check(result.isNotEmpty() && result.map { it.catalogPackage.id }.distinct().size == result.size)
                    check(selected.all { id -> result.any { it.catalogPackage.id == id && !it.hidden } })
                    sendStatus(1, Bundle().apply { putString("resolved_android", android); putInt("resolved_packages", result.size) })
                }
            } finally { directory.deleteRecursively() }
            finish(Activity.RESULT_OK, Bundle().apply { putString("resolution_status", "passed: Android16/17 pinned snapshots and Core dependency resolution") })
            return
        }
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

private fun RegistryIconRepository.iconBlocking(name: String) = kotlinx.coroutines.runBlocking { icon(name) }
