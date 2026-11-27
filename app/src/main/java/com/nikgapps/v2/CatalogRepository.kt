package com.nikgapps.v2

import androidx.annotation.WorkerThread
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

internal enum class MetadataSource { NETWORK, CACHE, FALLBACK }
internal data class CatalogSnapshot(val catalog: Catalog, val appSets: AppSetCatalog,
    val source: MetadataSource = MetadataSource.NETWORK, val fetchedAtMillis: Long = 0)
internal fun validateCatalog(catalog: Catalog, appSets: AppSetCatalog): CatalogSnapshot {
    require(catalog.schemaVersion == appSets.schemaVersion) { "Catalog and AppSet schema versions differ" }
    val ids = catalog.packages.map { it.id }.toSet()
    appSets.appSets.forEach { set -> require(set.packages.all { it in ids }) { "AppSet references a missing package" } }
    return CatalogSnapshot(catalog, appSets)
}

/** A complete, verified pair is persisted together; a failed refresh preserves the last successful pair. */
internal class CatalogRepository(private val directory: File, private val http: RegistryHttp = RegistryHttp(),
    private val now: () -> Long = System::currentTimeMillis) {
    private val lock = locks.computeIfAbsent(directory.canonicalPath) { Any() }
    private val target get() = File(directory, "snapshot.json")
    private val backup get() = File(directory, "snapshot.backup")
    @WorkerThread
    fun cached(): CatalogSnapshot? = synchronized(lock) { readCache() }
    @WorkerThread
    fun load(forceRefresh: Boolean = false): CatalogSnapshot = synchronized(lock) {
        val cached = readCache()
        if (!forceRefresh && cached != null && now() - cached.fetchedAtMillis in 0 until CACHE_TTL_MILLIS)
            return@synchronized cached
        try {
            val catalogText = http.get("${RegistryPin.ROOT}/catalog.json").toString(Charsets.UTF_8)
            val appSetsText = http.get("${RegistryPin.ROOT}/appsets.json").toString(Charsets.UTF_8)
            val fetchedAt = now()
            val parsed = parse(catalogText, appSetsText, fetchedAt)
            writeCache(buildJsonObject {
                put("schemaVersion", 1); put("registryCommit", RegistryPin.COMMIT); put("fetchedAtMillis", fetchedAt)
                put("catalog", catalogText); put("appsets", appSetsText)
            }.toString())
            parsed
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            cached?.copy(source = MetadataSource.FALLBACK) ?: throw failure
        }
    }
    private fun parse(catalogText: String, appSetsText: String, time: Long): CatalogSnapshot {
        fun verify(text: String, expected: String) {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            if (digest != expected) throw MetadataException("Pinned metadata checksum mismatch")
        }
        verify(catalogText, RegistryPin.CATALOG_SHA256); verify(appSetsText, RegistryPin.APPSETS_SHA256)
        return validateCatalog(CatalogParser.parseCatalog(catalogText), CatalogParser.parseAppSets(appSetsText)).copy(fetchedAtMillis = time)
    }
    private fun readCache(): CatalogSnapshot? = readSnapshot(target) ?: readSnapshot(backup)
    private fun readSnapshot(file: File): CatalogSnapshot? {
        if (!file.isFile || file.length() !in 1..8L * 1024 * 1024) return null
        return try {
            val root = Json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
            require(root.getValue("schemaVersion").jsonPrimitive.int == 1)
            require(root.getValue("registryCommit").jsonPrimitive.content == RegistryPin.COMMIT)
            parse(root.getValue("catalog").jsonPrimitive.content, root.getValue("appsets").jsonPrimitive.content,
                root.getValue("fetchedAtMillis").jsonPrimitive.long).copy(source = MetadataSource.CACHE)
        } catch (_: Exception) { null }
    }
    private fun writeCache(text: String) {
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create metadata cache" }
        val stage = File(directory, "snapshot.tmp")
        try {
            stage.outputStream().use { output -> output.write(text.toByteArray(Charsets.UTF_8)); output.fd.sync() }
            check(stage.readText(Charsets.UTF_8) == text) { "Metadata cache write incomplete" }
            if (target.exists()) {
                val validTarget = readSnapshot(target) != null
                if (validTarget) {
                    check(!backup.exists() || backup.delete())
                    check(target.renameTo(backup)) { "Unable to preserve metadata cache" }
                } else check(target.delete()) { "Unable to replace damaged metadata cache" }
            }
            try { check(stage.renameTo(target)) { "Unable to publish metadata cache" } }
            catch (failure: Exception) { if (!target.exists() && backup.exists()) backup.renameTo(target); throw failure }
            backup.delete()
        } finally { stage.delete() }
    }
    companion object {
        const val CACHE_TTL_MILLIS = 30L * 60L * 1000L
        private val locks = ConcurrentHashMap<String, Any>()
    }
}
