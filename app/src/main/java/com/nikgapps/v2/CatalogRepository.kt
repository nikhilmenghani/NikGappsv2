package com.nikgapps.v2

import androidx.annotation.WorkerThread
import java.security.MessageDigest

internal data class CatalogSnapshot(val catalog: Catalog, val appSets: AppSetCatalog)
internal fun validateCatalog(catalog: Catalog, appSets: AppSetCatalog): CatalogSnapshot {
    require(catalog.schemaVersion == appSets.schemaVersion) { "Catalog and AppSet schema versions differ" }
    val ids = catalog.packages.map { it.id }.toSet()
    appSets.appSets.forEach { set -> require(set.packages.all { it in ids }) { "AppSet references a missing package" } }
    return CatalogSnapshot(catalog, appSets)
}

/** Frozen public metadata reads for controlled comparisons. Cache and authenticated delivery follow later. */
internal class CatalogRepository(private val http: RegistryHttp = RegistryHttp()) {
    @WorkerThread
    fun load(): CatalogSnapshot = validateCatalog(
        CatalogParser.parseCatalog(document("catalog.json", RegistryPin.CATALOG_SHA256)),
        CatalogParser.parseAppSets(document("appsets.json", RegistryPin.APPSETS_SHA256)))
    private fun document(name: String, expected: String): String {
        val bytes = http.get("${RegistryPin.ROOT}/$name")
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (digest != expected) throw MetadataException("Pinned metadata checksum mismatch")
        return bytes.toString(Charsets.UTF_8)
    }
}
