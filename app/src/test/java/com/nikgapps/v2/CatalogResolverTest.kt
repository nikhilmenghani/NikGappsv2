package com.nikgapps.v2

import java.io.File
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CatalogResolverTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun fixture(path: String) = requireNotNull(javaClass.getResource("/registry/$path")).readText()
    private fun http() = RegistryHttp(OkHttpClient.Builder().addInterceptor { chain ->
        val path = chain.request().url.encodedPath.substringAfter(RegistryPin.COMMIT + "/")
        val resource = if (path == "releases/index.json") "index.json" else path
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(fixture(resource).toResponseBody()).build()
    }.build())
    @Test fun realLatestAndroid16And17SnapshotsResolveCore() {
        val repo = ReleaseRepository(temporary.newFolder(), http())
        for (android in listOf("16", "17")) {
            val metadata = repo.load(android)
            assertEquals(android, metadata.release.androidVersion)
            val core = metadata.appSets.appSets.first { it.id == "core" }
            val selected = metadata.catalog.publicPackages(core).map { it.id }.toSet()
            val result = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolve(core.id, selected,
                ReleaseChannel.STABLE, emptyMap(), metadata.release.platformApi, "arm64-v8a")
            assertTrue(result.isNotEmpty())
            assertTrue(selected.all { id -> result.any { it.catalogPackage.id == id && !it.hidden } })
            assertEquals(result.size, result.map { it.catalogPackage.id }.distinct().size)
        }
    }
    @Test fun snapshotIdentityAndCompatibilityCannotBeCrossed() {
        val index = CatalogParser.parseReleaseIndex(fixture("index.json"))
        val a16 = selectRelease(index, "16", "stable", "arm64-v8a")
        assertThrows(MetadataException::class.java) { selectRelease(index, "17", "stable", "arm64-v8a", a16.id) }
        assertThrows(MetadataException::class.java) { selectRelease(index, "17", "stable", "x86_64") }
        assertThrows(MetadataException::class.java) { selectRelease(index, "15", "stable", "arm64-v8a") }
    }
    private fun graph(dependencies: Map<String, List<Dependency>>): Pair<Catalog, AppSetCatalog> {
        val base = CatalogParser.parseCatalog(fixture("catalog.json"))
        val version = PackageVersion("v", 1, null, AndroidCompatibility(null, null, null), listOf("arm64-v8a"), emptyList(), emptyList(), listOf("phone"))
        val packages = dependencies.map { (id, deps) -> CatalogPackage(id, id, true, false, deps, mapOf("stable" to "v"), mapOf("v" to version), emptyList()) }
        return base.copy(packages = packages) to AppSetCatalog(1, listOf(CatalogAppSet("set", "set", packages.map { it.id }, packages.map { it.id })))
    }
    @Test fun diamondDependenciesAreOrderedOnceAndExplicitChoicesRemainVisible() {
        val (catalog, sets) = graph(mapOf("a" to listOf(Dependency("b", null), Dependency("c", null)), "b" to listOf(Dependency("d", null)), "c" to listOf(Dependency("d", null)), "d" to emptyList()))
        val result = CatalogResolver(catalog, sets).resolve("set", setOf("a", "b"), ReleaseChannel.STABLE, emptyMap(), 37, "arm64-v8a")
        assertEquals(listOf("d", "b", "c", "a"), result.map { it.catalogPackage.id })
        assertFalse(result.first { it.catalogPackage.id == "b" }.hidden)
        assertTrue(result.first { it.catalogPackage.id == "d" }.hidden)
    }
    @Test fun cyclesAndMissingDependenciesAreRejected() {
        val (catalog, sets) = graph(mapOf("a" to listOf(Dependency("b", null)), "b" to listOf(Dependency("a", null))))
        assertThrows(RegistryException.DependencyCycle::class.java) { CatalogResolver(catalog, sets).resolve("set", setOf("a"), ReleaseChannel.STABLE, emptyMap(), 37, "arm64-v8a") }
        val missing = catalog.copy(packages = catalog.packages.filter { it.id != "b" })
        assertThrows(RegistryException.MissingDependency::class.java) { CatalogResolver(missing, sets).resolve("set", setOf("a"), ReleaseChannel.STABLE, emptyMap(), 37, "arm64-v8a") }
    }
    @Test fun conditionalDependenciesRespectTheirOwningAppSet() {
        val (catalog, sets) = graph(mapOf("a" to listOf(Dependency("b", "other")), "b" to emptyList()))
        val result = CatalogResolver(catalog, sets).resolve("set", setOf("a"), ReleaseChannel.STABLE, emptyMap(), 37, "arm64-v8a")
        assertEquals(listOf("a"), result.map { it.catalogPackage.id })
    }
}
