package com.nikgapps.v2

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class RegistryHttpTest {
    private fun transport(code: Int = 200, body: String = "fixture"): RegistryHttp = RegistryHttp(
        OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code)
                .message("test").body(body.toResponseBody()).build()
        }.build(), pause = {})
    @Test fun readsSuccessfulHttpsResponse() {
        assertEquals("fixture", transport().get("https://example.com/metadata").decodeToString())
    }
    @Test fun refusesCleartextBeforeSendingAnything() {
        var sent = false
        val http = RegistryHttp(OkHttpClient.Builder().addInterceptor { sent = true; error("Must not run") }.build(), pause = {})
        assertThrows(IllegalArgumentException::class.java) { http.get("http://example.com/metadata") }
        assertFalse(sent)
    }
    @Test fun nonSuccessStatusCannotBecomeValidMetadata() {
        val failure = assertThrows(NetworkResponseException::class.java) { transport(503).get("https://example.com/metadata") }
        assertEquals(503, failure.statusCode)
    }
    @Test fun oversizedResponseIsRejected() {
        assertThrows(MetadataSizeException::class.java) { transport(body = "1234567890").get("https://example.com/metadata", 5) }
    }
}
