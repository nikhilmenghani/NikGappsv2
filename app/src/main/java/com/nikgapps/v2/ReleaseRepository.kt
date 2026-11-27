package com.nikgapps.v2

import androidx.annotation.WorkerThread
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal data class RegistryMetadata(val catalog: Catalog, val appSets: AppSetCatalog, val index: ReleaseIndex, val release: CatalogRelease)
internal fun selectRelease(index: ReleaseIndex, android: String, channel: String, architecture: String, id: String? = null): ReleaseSummary {
    val selected = id ?: index.latest[android]?.get(channel)?.get(architecture) ?: throw MetadataException("No compatible release is available")
    return index.releases.firstOrNull { it.id == selected && it.androidVersion == android && it.channel == channel && it.architecture == architecture }
        ?: throw MetadataException("Release is absent or incompatible")
}

/** Snapshot paths and bytes stay pinned throughout the investigation. */
internal class ReleaseRepository(private val directory: File, private val http: RegistryHttp = RegistryHttp()) {
    private val lock = locks.computeIfAbsent(directory.canonicalPath) { Any() }
    @WorkerThread
    fun load(android: String, channel: String = "stable", architecture: String = "arm64-v8a", releaseId: String? = null): RegistryMetadata = synchronized(lock) {
        val base = CatalogRepository(File(directory, "catalog"), http).load()
        val index = CatalogParser.parseReleaseIndex(document("releases/index.json", RegistryPin.INDEX_SHA256))
        val summary = selectRelease(index, android, channel, architecture, releaseId)
        val expected = RegistryPin.RELEASE_HASHES[summary.manifest] ?: throw MetadataException("Release manifest is not in the pinned revision")
        val release = CatalogParser.parseRelease(document(summary.manifest, expected))
        require(release.id == summary.id && release.androidVersion == summary.androidVersion && release.platformApi == summary.platformApi &&
            release.architecture == summary.architecture && release.channel == summary.channel) { "Release identity differs from its index" }
        val byId = base.catalog.packages.associateBy { it.id }
        release.packages.forEach { (id, version) -> require(version in (byId[id]?.versions ?: emptyMap())) { "Release references an unavailable version" } }
        val sets = AppSetCatalog(release.schemaVersion, release.appSets)
        validateCatalog(base.catalog, sets)
        RegistryMetadata(base.catalog, sets, index, release)
    }
    private fun document(path: String, expected: String): String {
        val target = File(directory, "$expected.json")
        fun valid(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == expected
        if (target.isFile && target.length() < 8L * 1024 * 1024) {
            val bytes = target.readBytes()
            if (valid(bytes)) return bytes.toString(Charsets.UTF_8)
        }
        val bytes = http.get(RegistryDelivery.contents(path))
        if (!valid(bytes)) throw MetadataException("Pinned release checksum mismatch")
        check(directory.isDirectory || directory.mkdirs())
        val stage = File(directory, "$expected.tmp")
        try {
            stage.outputStream().use { it.write(bytes); it.fd.sync() }
            check(!target.exists() || target.delete())
            check(stage.renameTo(target)) { "Unable to publish release metadata" }
        } finally { stage.delete() }
        return bytes.toString(Charsets.UTF_8)
    }
    companion object { private val locks = ConcurrentHashMap<String, Any>() }

}
