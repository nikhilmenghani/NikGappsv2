package com.nikgapps.v2

import kotlinx.serialization.json.*

internal data class CatalogDefaults(val architectures: List<String>, val deviceTypes: List<String>)
internal data class Catalog(val schemaVersion: Int, val updatedAt: String, val androidVersion: String,
    val platformApi: Int, val architecture: String, val packages: List<CatalogPackage>, val defaults: CatalogDefaults)
internal data class CatalogPackage(val id: String, val name: String, val selectable: Boolean, val internal: Boolean,
    val dependencies: List<Dependency>, val channels: Map<String, String>, val versions: Map<String, PackageVersion>, val appSets: List<String>)
internal data class Dependency(val id: String, val whenAppSet: String?)
internal data class AndroidCompatibility(val minApi: Int?, val targetApi: Int?, val maxApi: Int?)
internal data class PackageVersion(val versionName: String, val versionCode: Long, val packageName: String?,
    val android: AndroidCompatibility, val architectures: List<String>, val supportedAndroidVersions: List<String>,
    val sources: List<String>, val deviceTypes: List<String>)
internal data class AppSetCatalog(val schemaVersion: Int, val appSets: List<CatalogAppSet>)
internal data class CatalogAppSet(val id: String, val name: String, val packages: List<String>, val resolvedPackages: List<String>)
internal enum class ReleaseChannel { STABLE, BETA, CANARY; val wireName get() = name.lowercase() }
internal data class ReleaseSummary(val id: String, val androidVersion: String, val platformApi: Int,
    val architecture: String, val channel: String, val source: String, val createdAt: String, val manifest: String)
internal data class ReleaseIndex(val schemaVersion: Int, val updatedAt: String,
    val latest: Map<String, Map<String, Map<String, String>>>, val releases: List<ReleaseSummary>)
internal data class CatalogRelease(val schemaVersion: Int, val id: String, val createdAt: String,
    val androidVersion: String, val platformApi: Int, val architecture: String, val channel: String,
    val source: String, val packages: Map<String, String>, val appSets: List<CatalogAppSet>)

internal class MetadataException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Reviewed catalog/AppSet parsing only. Release snapshots, installer/payload fields and selection rules follow separately. */
internal object CatalogParser {
    private val json = Json { isLenient = false }
    fun parseCatalog(text: String): Catalog = wrap("catalog") {
        val root = json.parseToJsonElement(text).jsonObject
        schema(root)
        val defaultsObject = root["defaults"]?.jsonObject
        val defaults = CatalogDefaults(
            defaultsObject?.optionalStrings("architectures")?.ifEmpty { listOf("arm64-v8a") } ?: listOf(root.string("architecture")),
            defaultsObject?.optionalStrings("deviceTypes")?.ifEmpty { listOf("phone") } ?: listOf("phone"))
        Catalog(root.int("schemaVersion"), root.string("updatedAt"), root.string("androidVersion"), root.int("platformApi"),
            root.string("architecture"), root.array("packages").map { item ->
                val p = item.jsonObject
                CatalogPackage(p.string("id"), p.string("name"), p.bool("selectable"), p.bool("internal"),
                    p.array("dependencies").map { d -> val dependency = d.jsonObject
                        Dependency(dependency.string("id"), dependency["when"]?.jsonObject?.get("appSet")?.jsonPrimitive?.content)
                    }, p.obj("channels").mapValues { it.value.jsonPrimitive.content },
                    p.obj("versions").mapValues { (_, value) -> version(value.jsonObject, defaults) }, p.optionalStrings("appSets"))
            }, defaults).also { catalog ->
                require(catalog.packages.isNotEmpty()) { "packages is empty" }
                require(catalog.packages.map { it.id }.distinct().size == catalog.packages.size) { "duplicate package id" }
                require(catalog.packages.all { it.versions.isNotEmpty() }) { "package has no versions" }
            }
    }
    fun parseAppSets(text: String): AppSetCatalog = wrap("AppSet metadata") {
        val root = json.parseToJsonElement(text).jsonObject
        schema(root)
        AppSetCatalog(root.int("schemaVersion"), root.array("appSets").map { item -> val a = item.jsonObject
            CatalogAppSet(a.string("id"), a.string("name"), a.strings("packages"), a.strings("resolvedPackages"))
        }).also {
            require(it.appSets.isNotEmpty()) { "appSets is empty" }
            require(it.appSets.map { a -> a.id }.distinct().size == it.appSets.size) { "duplicate AppSet id" }
        }
    }
    fun parseReleaseIndex(text: String): ReleaseIndex = wrap("release index") {
        val root = json.parseToJsonElement(text).jsonObject; schema(root)
        val latest = root.obj("latest").mapValues { (_, channels) -> channels.jsonObject.mapValues { (_, architectures) ->
            architectures.jsonObject.mapValues { it.value.jsonPrimitive.content }
        } }
        ReleaseIndex(root.int("schemaVersion"), root.string("updatedAt"), latest, root.array("releases").map { value ->
            val r = value.jsonObject
            ReleaseSummary(r.string("id"), r.string("androidVersion"), r.int("platformApi"), r.string("architecture"),
                r.string("channel"), r["source"]?.jsonPrimitive?.content ?: "legacy", r.string("createdAt"), r.string("manifest"))
        }).also { require(it.releases.map { r -> r.id }.distinct().size == it.releases.size) { "duplicate release id" } }
    }
    fun parseRelease(text: String): CatalogRelease = wrap("release manifest") {
        val root = json.parseToJsonElement(text).jsonObject; schema(root)
        CatalogRelease(root.int("schemaVersion"), root.string("id"), root.string("createdAt"), root.string("androidVersion"),
            root.int("platformApi"), root.string("architecture"), root.string("channel"), root["source"]?.jsonPrimitive?.content ?: "legacy",
            root.obj("packages").mapValues { (_, value) -> value.jsonObject.string("version") },
            root.array("appSets").map { value -> val a = value.jsonObject
                CatalogAppSet(a.string("id"), a.string("name"), a.strings("packages"), a.strings("resolvedPackages")) })
    }
    private fun version(v: JsonObject, defaults: CatalogDefaults): PackageVersion {
        val android = v.obj("android")
        return PackageVersion(v.string("versionName"), v.getValue("versionCode").jsonPrimitive.long,
            v.nullableString("packageName"), AndroidCompatibility(android.nullableInt("minApi"), android.nullableInt("targetApi"), android.nullableInt("maxApi")),
            if (v.containsKey("architectures")) v.strings("architectures") else defaults.architectures,
            v.optionalStrings("supportedAndroidVersions"), v.optionalStrings("sources"),
            if (v.containsKey("deviceTypes")) v.strings("deviceTypes") else defaults.deviceTypes)
    }
    private fun schema(root: JsonObject) = require(root.int("schemaVersion") == 1) { "Unsupported schemaVersion" }
    private inline fun <T> wrap(name: String, block: () -> T): T = try { block() } catch (e: Exception) {
        if (e is MetadataException) throw e else throw MetadataException("Invalid $name: ${e.message}", e)
    }
    private fun JsonObject.string(k: String) = getValue(k).jsonPrimitive.content.also { require(it.isNotBlank()) { "$k is blank" } }
    private fun JsonObject.int(k: String) = getValue(k).jsonPrimitive.int
    private fun JsonObject.bool(k: String) = getValue(k).jsonPrimitive.boolean
    private fun JsonObject.array(k: String) = getValue(k).jsonArray
    private fun JsonObject.obj(k: String) = getValue(k).jsonObject
    private fun JsonObject.strings(k: String) = array(k).map { it.jsonPrimitive.content }
    private fun JsonObject.optionalStrings(k: String) = get(k)?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
    private fun JsonObject.nullableString(k: String) = get(k).let { if (it == null || it is JsonNull) null else it.jsonPrimitive.content }
    private fun JsonObject.nullableInt(k: String) = get(k).let { if (it == null || it is JsonNull) null else it.jsonPrimitive.int }
}

private val NEVER_SELECTABLE = setOf("extrafiles", "extrafilesgo", "gms_core_support_common", "gms_core_support_standard", "gms_core_support_go")
internal fun Catalog.publicPackages(appSet: CatalogAppSet): List<CatalogPackage> {
    val byId = packages.associateBy { it.id }
    return appSet.packages.mapNotNull(byId::get).filter { it.selectable && !it.internal && it.id.lowercase() !in NEVER_SELECTABLE && it.name.lowercase() !in NEVER_SELECTABLE }
}
