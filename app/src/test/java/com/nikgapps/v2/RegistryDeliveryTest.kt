package com.nikgapps.v2

import java.io.IOException
import java.io.InterruptedIOException
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class RegistryDeliveryTest {
    private val url = RegistryDelivery.contents("catalog.json")
    private fun response(request: Request, code: Int = 200, headers: Map<String, String> = emptyMap()) =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body("fixture".toResponseBody()).apply { headers.forEach { (key, value) -> header(key, value) } }.build()
    @Test fun tokenIsScopedAndReplacedOnlyForTheExactRepository() {
        val trusted = Request.Builder().url(url).header("Authorization", "stale").build()
        val prepared = RegistryDelivery.prepare(trusted) { "test-only" }
        assertEquals("Bearer test-only", prepared.header("Authorization"))
        assertEquals("application/vnd.github.raw+json", prepared.header("Accept"))
        for (untrusted in listOf("https://api.github.com/repos/other/repo/contents/file", "https://api.github.com/repos/nikgapps/nikgapps-packages-other/file",
            "https://api.github.com:444/repos/nikgapps/nikgapps-packages/file", "https://api.github.com.evil.test/repos/nikgapps/nikgapps-packages/file", "https://objects.githubusercontent.com/asset")) {
            val stripped = RegistryDelivery.prepare(trusted.newBuilder().url(untrusted).build()) { error("Must not read credential") }
            assertNull(stripped.header("Authorization"))
        }
        assertThrows(RegistryAuthenticationException::class.java) { RegistryDelivery.prepare(trusted) { "" } }
        assertThrows(MetadataException::class.java) { RegistryDelivery.prepare(trusted.newBuilder().url("https://gitlab.com/old").build()) }
    }
    @Test fun noCredentialFailsBeforeAnyNetworkRequest() {
        var sent = false
        val http = RegistryHttp(OkHttpClient.Builder().addInterceptor { sent = true; error("Must not send") }.build(), token = { "" })
        assertThrows(RegistryAuthenticationException::class.java) { http.get(url) }; assertFalse(sent)
    }
    @Test fun primaryAndSecondaryLimitsStopRetriesAndPersistAcrossRestart() {
        for (headers in listOf(mapOf("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "110"), mapOf("Retry-After" to "9"))) {
            var clock = 100_000L; var stored: Long? = null; var count = 0
            val cooldown = RegistryCooldown { clock }.apply { restore(0) { stored = it } }
            val client = OkHttpClient.Builder().addInterceptor { count++; response(it.request(), 403, headers) }.build()
            val http = RegistryHttp(client, cooldown, pause = { error("Must not retry cooldown") }, token = { "fixture" })
            val failure = assertThrows(RegistryRateLimitedException::class.java) { http.get(url) }
            assertEquals(1, count); assertEquals(failure.retryAtMillis, stored)
            val restored = RegistryCooldown { clock }.apply { restore(stored!!) { stored = it } }
            assertThrows(RegistryRateLimitedException::class.java) { restored.requireAvailable(url.toHttpUrl()) }
            restored.requireAvailable("https://example.com/cached-asset".toHttpUrl())
            clock = failure.retryAtMillis + 1; restored.requireAvailable(url.toHttpUrl()); assertNull(stored)
        }
    }
    @Test fun retryDatesAndMalformedValuesHaveBoundedFallbacks() {
        assertEquals(110_000, registryRetryAt("10", null, 100_000))
        assertEquals(120_000, registryRetryAt("Thu, 01 Jan 1970 00:02:00 GMT", null, 100_000))
        assertEquals(160_000, registryRetryAt("invalid", "invalid", 100_000))
        assertEquals(170_000, registryRetryAt("99999999999999999999999", "170", 100_000))
        assertEquals(101_000, registryRetryAt("0", null, 100_000))
        assertEquals(Long.MAX_VALUE, registryRetryAt("2", null, Long.MAX_VALUE - 10))
    }
    @Test fun transientFailuresAreRetriedButAuthorizationAndMissingDataAreNot() {
        var count = 0; val pauses = mutableListOf<Long>()
        val client = OkHttpClient.Builder().addInterceptor {
            count++; when(count) { 1 -> throw IOException("controlled offline"); 2 -> response(it.request(), 503); else -> response(it.request()) }
        }.build()
        assertEquals("fixture", RegistryHttp(client, pause = { pauses.add(it) }, token = { "fixture" }).get(url).decodeToString())
        assertEquals(listOf(1000L, 2000L), pauses)
        for (code in listOf(401, 403, 404)) {
            count = 0
            val failing = RegistryHttp(OkHttpClient.Builder().addInterceptor { count++; response(it.request(), code) }.build(), pause = { error("No retry") }, token = { "fixture" })
            assertEquals(code, assertThrows(NetworkResponseException::class.java) { failing.get(url) }.statusCode); assertEquals(1, count)
        }
    }
    @Test fun interruptionStopsBackoffWithoutAnotherRequest() {
        var count = 0
        val client = OkHttpClient.Builder().addInterceptor { count++; response(it.request(), 503) }.build()
        try {
            assertThrows(InterruptedIOException::class.java) { RegistryHttp(client, pause = { throw InterruptedException() }, token = { "fixture" }).get(url) }
            assertTrue(Thread.currentThread().isInterrupted); assertEquals(1, count)
        } finally { Thread.interrupted() }
    }
    @Test fun redirectHeaderObservationKeepsTheApiLimitWithoutTrustingTheCdn() {
        val api = response(Request.Builder().url(url).build(), 302, mapOf("X-RateLimit-Remaining" to "4123", "X-RateLimit-Limit" to "5000", "X-RateLimit-Resource" to "core"))
            .newBuilder().body("".toResponseBody()).build()
        val cdn = response(Request.Builder().url("https://objects.githubusercontent.com/file").build(), headers = mapOf("X-RateLimit-Remaining" to "fake"))
            .newBuilder().priorResponse(api).build()
        RegistryRequestLimits.observe(cdn)
        assertEquals(4123L, RegistryRequestLimits.latest.value!!.remaining)
        assertEquals("core", RegistryRequestLimits.latest.value!!.resource)
    }
}
