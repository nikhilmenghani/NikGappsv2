package com.nikgapps.v2

import java.io.File
import java.io.IOException
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CatalogCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun transport(count: () -> Unit = {}, fail: Boolean = false) = RegistryHttp(OkHttpClient.Builder().addInterceptor { chain ->
        count()
        if (fail) throw IOException("offline")
        val name = chain.request().url.pathSegments.last()
        val text = requireNotNull(javaClass.getResource("/registry/$name")).readText()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(text.toResponseBody()).build()
    }.build(), token = { "test-only-token" }, pause = {})
    @Test fun freshCacheSurvivesRepositoryRestartAndSkipsRequests() {
        val dir = temporary.newFolder(); var requests = 0
        assertEquals(MetadataSource.NETWORK, CatalogRepository(dir, transport({ requests++ }), { 1000 }).load().source)
        assertEquals(2, requests)
        assertEquals(MetadataSource.CACHE, CatalogRepository(dir, transport(fail = true), { 2000 }).load().source)
        assertEquals(75, CatalogRepository(dir).cached()!!.catalog.packages.size)
    }
    @Test fun expiredOrFailedRefreshKeepsTheVerifiedSnapshot() {
        val dir = temporary.newFolder()
        CatalogRepository(dir, transport(), { 1000 }).load()
        val offline = CatalogRepository(dir, transport(fail = true), { 1000 + CatalogRepository.CACHE_TTL_MILLIS + 1 })
        assertEquals(MetadataSource.FALLBACK, offline.load().source)
        assertEquals(MetadataSource.FALLBACK, offline.load(forceRefresh = true).source)
        assertEquals(49, offline.cached()!!.appSets.appSets.size)
    }
    @Test fun damagedSnapshotCannotBecomeOfflineMetadata() {
        val dir = temporary.newFolder(); File(dir, "snapshot.json").writeText("broken")
        val repo = CatalogRepository(dir, transport(fail = true))
        assertNull(repo.cached())
        assertThrows(IOException::class.java) { repo.load() }
    }
    @Test fun interruptedPublicationRecoversTheLastCompletePair() {
        val dir = temporary.newFolder(); CatalogRepository(dir, transport()).load()
        assertTrue(File(dir, "snapshot.json").renameTo(File(dir, "snapshot.backup")))
        File(dir, "snapshot.tmp").writeText("partial")
        assertEquals(75, CatalogRepository(dir).cached()!!.catalog.packages.size)
    }
    @Test fun changedDocumentIsRejectedWithoutReplacingSuccessfulCache() {
        val dir = temporary.newFolder(); CatalogRepository(dir, transport()).load()
        val changed = RegistryHttp(OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body("changed".toResponseBody()).build()
        }.build(), token = { "test-only-token" }, pause = {})
        assertEquals(MetadataSource.FALLBACK, CatalogRepository(dir, changed).load(forceRefresh = true).source)
        assertEquals(75, CatalogRepository(dir).cached()!!.catalog.packages.size)
    }
    @Test fun failedPublicationPreservesARecoveredSnapshot() {
        val dir = temporary.newFolder(); CatalogRepository(dir, transport()).load()
        assertTrue(File(dir, "snapshot.json").renameTo(File(dir, "snapshot.backup")))
        val blocked = File(dir, "snapshot.json"); assertTrue(blocked.mkdir())
        File(blocked, "occupied").writeText("block publication")
        assertEquals(MetadataSource.FALLBACK, CatalogRepository(dir, transport()).load(forceRefresh = true).source)
        assertEquals(75, CatalogRepository(dir).cached()!!.catalog.packages.size)
    }

}
