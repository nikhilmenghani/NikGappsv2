package com.nikgapps.v2

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class GitHubDeviceAuthClientTest {
    private val challenge = GitHubChallenge("fixture-private-device", "ABCD-EFGH", "https://github.com/login/device", 900, 5)
    private fun response(request: Request, text: String, code: Int = 200, retry: String? = null) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture").body(text.toResponseBody())
        .apply { retry?.let { header("Retry-After", it) } }.build()
    @Test fun challengeRequiresGitHubVerificationUrlAndDoesNotLogItsCodes(): Unit = runBlocking {
        fun http(uri: String) = OkHttpClient.Builder().addInterceptor {
            assertEquals("https://github.com/login/device/code", it.request().url.toString())
            response(it.request(), """{"device_code":"fixture-private-device","user_code":"ABCD-EFGH","verification_uri":"$uri","expires_in":900,"interval":1}""")
        }.build()
        val result = GitHubDeviceAuthClient(http(challenge.verificationUri), now = { 0 }).start()
        assertEquals(5, result.interval)
        assertFalse(result.toString().contains(result.deviceCode)); assertFalse(result.toString().contains(result.userCode))
        assertThrows(GitHubSignInException::class.java) { runBlocking { GitHubDeviceAuthClient(http("https://evil.test/device"), now = { 0 }).start() } }
    }
    @Test fun pendingAndSlowdownRespectServerPollingIntervals() = runBlocking {
        var clock = 0L; val calls = mutableListOf<Long>()
        val replies = listOf("""{"error":"slow_down"}""", """{"error":"authorization_pending"}""", """{"access_token":"fixture-oauth-token"}""")
        val http = OkHttpClient.Builder().addInterceptor { calls.add(clock); response(it.request(), replies[calls.size - 1]) }.build()
        val client = GitHubDeviceAuthClient(http, { clock }, { clock += it })
        assertEquals("fixture-oauth-token", client.awaitToken(challenge))
        assertEquals(listOf(5000L,15000L,25000L), calls)
    }
    @Test fun denialExpiryAndUnknownProtocolErrorsDoNotKeepPolling() {
        for (error in listOf("access_denied", "expired_token", "unknown")) {
            var clock = 0L; var count = 0
            val http = OkHttpClient.Builder().addInterceptor { count++; response(it.request(), """{"error":"$error"}""") }.build()
            assertThrows(GitHubSignInException::class.java) { runBlocking { GitHubDeviceAuthClient(http, { clock }, { clock += it }).awaitToken(challenge) } }
            assertEquals(1, count)
        }
    }
    @Test fun transientNetworkAndServerLimitsRetryWithinExpiry() = runBlocking {
        var clock = 0L; var count = 0
        val http = OkHttpClient.Builder().addInterceptor {
            count++
            when(count) {
                1 -> throw IOException("controlled offline")
                2 -> response(it.request(), "unavailable", 503)
                3 -> response(it.request(), "limited", 429, "1")
                else -> response(it.request(), """{"access_token":"fixture-oauth-token"}""")
            }
        }.build()
        assertEquals("fixture-oauth-token", GitHubDeviceAuthClient(http, { clock }, { clock += it }).awaitToken(challenge))
        assertEquals(4, count); assertTrue(clock >= 80000)
    }
    @Test fun expiryLongSlowdownAndCancellationCannotSendLateRequests() {
        var clock = 0L; var calls = 0
        val http = OkHttpClient.Builder().addInterceptor { calls++; response(it.request(), """{"error":"slow_down","interval":3600}""") }.build()
        assertThrows(GitHubSignInException::class.java) { runBlocking { GitHubDeviceAuthClient(http, { clock }, { clock += it }).awaitToken(challenge.copy(expiresIn = 6)) } }
        assertEquals(1, calls); assertEquals(6000L, clock)
        calls = 0; clock = 0
        assertThrows(GitHubSignInException::class.java) { runBlocking { GitHubDeviceAuthClient(http, { clock }, { clock += it }).awaitToken(challenge.copy(expiresIn = 1)) } }
        assertEquals(0, calls)
        assertThrows(CancellationException::class.java) { runBlocking { GitHubDeviceAuthClient(http, { 0 }, { throw CancellationException() }).awaitToken(challenge) } }
        assertEquals(0, calls)
    }
}
