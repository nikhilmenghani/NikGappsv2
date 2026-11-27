package com.nikgapps.v2

internal data class ResolvedPackage(val catalogPackage: CatalogPackage, val versionKey: String,
    val version: PackageVersion, val channel: ReleaseChannel, val hidden: Boolean)
internal data class MultiAppSetResolution(val packages: List<ResolvedPackage>, val packageAppSets: Map<String, CatalogAppSet>)

internal class CatalogResolver(private val catalog: Catalog, private val appSets: AppSetCatalog,
    private val release: CatalogRelease? = null) {
    fun resolveAcrossAppSets(selections: Map<String, String>, defaultChannel: ReleaseChannel,
        overrides: Map<String, ReleaseChannel>, api: Int, architecture: String,
        deviceType: String = "phone"): MultiAppSetResolution {
        val resolved = linkedMapOf<String, ResolvedPackage>()
        val owners = linkedMapOf<String, CatalogAppSet>()
        selections.entries.groupBy({ it.value }, { it.key }).toSortedMap().forEach { (appSetId, ids) ->
            val appSet = appSets.appSets.firstOrNull { it.id == appSetId }
                ?: throw IllegalArgumentException("Unknown AppSet '$appSetId'")
            resolve(appSetId, ids.toSet(), defaultChannel, overrides, api, architecture, deviceType).forEach { pkg ->
                val existing = resolved[pkg.catalogPackage.id]
                if (existing != null && existing.versionKey != pkg.versionKey)
                    throw MetadataException("Package '${pkg.catalogPackage.id}' resolved to conflicting versions across AppSets")
                resolved[pkg.catalogPackage.id] = existing?.copy(hidden = existing.hidden && pkg.hidden) ?: pkg
                owners.putIfAbsent(pkg.catalogPackage.id, appSet)
            }
        }
        return MultiAppSetResolution(resolved.values.toList(), owners)
    }

    fun resolve(appSetId: String, selectedIds: Set<String>, defaultChannel: ReleaseChannel,
        overrides: Map<String, ReleaseChannel>, api: Int, architecture: String,
        deviceType: String = "phone"): List<ResolvedPackage> {
        val appSet = appSets.appSets.firstOrNull { it.id == appSetId }
            ?: throw IllegalArgumentException("Unknown AppSet '$appSetId'")
        val publicIds = catalog.publicPackages(appSet).map { it.id }.toSet()
        require(selectedIds.all { it in publicIds }) { "Selection includes a hidden or unavailable package" }
        val byId = catalog.packages.associateBy { it.id }
        val result = linkedMapOf<String, ResolvedPackage>()
        val visiting = mutableListOf<String>()
        fun visit(id: String, parent: String?) {
            if (id in result) return
            if (id in visiting) throw RegistryException.DependencyCycle(visiting.dropWhile { it != id } + id)
            val pkg = byId[id] ?: throw RegistryException.MissingDependency(id, parent ?: "selection")
            visiting += id
            val channel = overrides[id] ?: defaultChannel
            val versionKey = release?.packages?.get(id)
                ?: pkg.channels[channel.wireName]
                ?: throw RegistryException.MissingChannel(id, channel.wireName)
            val version = pkg.versions[versionKey] ?: throw MetadataException("Package '$id' channel references missing version '$versionKey'")
            validateCompatibility(id, version, api, architecture, deviceType)
            pkg.dependencies.filter { it.whenAppSet == null || it.whenAppSet == appSet.id }.forEach { visit(it.id, id) }
            visiting.removeAt(visiting.lastIndex)
            result[id] = ResolvedPackage(pkg, versionKey, version, channel, id !in selectedIds)
        }
        selectedIds.sorted().forEach { visit(it, null) }
        return result.values.toList()
    }

    private fun validateCompatibility(id: String, version: PackageVersion, api: Int, architecture: String, deviceType: String) =
        com.nikgapps.v2.validateCompatibility(id, version, api, architecture, deviceType, release?.androidVersion)
}
