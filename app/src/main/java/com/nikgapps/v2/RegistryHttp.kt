package com.nikgapps.v2

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class NetworkResponseException(val statusCode: Int) : IOException("Server returned HTTP $statusCode")
internal class MetadataSizeException : IOException("Metadata response exceeds the size limit")

/** Bounded HTTPS reads. Credentials are scoped before each request; cooldowns are not retried. */
internal class RegistryHttp(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS).followSslRedirects(false).build(),
    private val cooldown: RegistryCooldown = RegistryAvailability.cooldown,
    private val pause: (Long) -> Unit = { Thread.sleep(it) },
    private val token: () -> String = { RegistryDelivery.userToken() },
    private val maxAttempts: Int = 5) {
    init { require(maxAttempts in 1..5) }
    fun get(url: String, maxBytes: Int = 32 * 1024 * 1024): ByteArray {
        require(maxBytes > 0)
        val address = url.toHttpUrl()
        require(address.isHttps) { "HTTPS is required" }
        val request = RegistryDelivery.prepare(Request.Builder().url(address).header("User-Agent", "NikGappsv2").build(), token)
        for (attempt in 0 until maxAttempts) {
            cooldown.requireAvailable(address)
            try {
                return client.newCall(request).execute().use { response ->
                    RegistryRequestLimits.observe(response)
                    if (RegistryDelivery.rateLimited(response)) throw RegistryRateLimitedException(
                        cooldown.rateLimited(response.header("Retry-After"), response.header("X-RateLimit-Reset")))
                    if (!response.isSuccessful) throw NetworkResponseException(response.code)
                    val body = response.body
                    if (body.contentLength() > maxBytes) throw MetadataSizeException()
                    body.byteStream().use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (output.size().toLong() + count > maxBytes) throw MetadataSizeException()
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                }
            } catch (failure: IOException) {
                val retryable = when (failure) {
                    is RegistryRateLimitedException, is RegistryAuthenticationException, is MetadataSizeException -> false
                    is NetworkResponseException -> failure.statusCode in 500..599 || failure.statusCode == 429
                    else -> !Thread.currentThread().isInterrupted
                }
                if (!retryable || attempt == maxAttempts - 1) throw failure
            }
            // The failed response/body has closed before any backoff. Cancellation must preserve interruption.
            try { pause((1000L shl attempt).coerceAtMost(30_000)) }
            catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Registry request interrupted").apply { initCause(interrupted) }
            }
        }
        error("Registry request did not complete")
    }
}

internal object RegistryPin {
    const val COMMIT = "9a0d39ffc59d3c13a3c1e70a543579e0c9e87504"
    const val ROOT = "https://raw.githubusercontent.com/nikgapps/nikgapps-packages/$COMMIT"
    const val INDEX = "$ROOT/releases/index.json"
    const val CATALOG_SHA256 = "717315a9d06bdf950c8f63d31d74c40e0629a65ffa2d4edf3ada0606e8425a41"
    const val APPSETS_SHA256 = "6bb845fea61d26468ca709496a9fe64787d31d0b90ef3191dee59f3250426c50"
    const val ICONS_SHA256 = "1cf4edd1c583b3874af63b17cb3119c84ddfdf5699062416596a45078facf905"
    const val INDEX_SHA256 = "c16bf0972e1e7a42b98d79f1813c8ce0ef1e3a8674ec0ad3e097227ecafb6823"
    val RELEASE_HASHES = mapOf(
        "releases/android-17/arm64-v8a/stable/20260811T025719Z-243eb7d6.json" to "2d783f0749bcfdb040e29b65f40e36efd2758186f79d7a5bcc914c6810ed9c5b",
        "releases/android-16/arm64-v8a/stable/20260811T030332Z-4482dd23.json" to "bf6760a22c67a63be1c615a35d694b83fd2968ae3cc731a9c98ae4b23aa6358f",
        "releases/android-17/arm64-v8a/stable/20260811T034831Z-5b178339.json" to "55b6bd0ac0707eeb686b094be21ed10ba8723e8844bf31fc4a114fd7fca98e8b",
        "releases/android-17/arm64-v8a/stable/20261006T192020Z-951a5b50.json" to "882c0c5c2a6914d55eba0ba3cf7280f8c8f2103f660a518f465f7d9526dcaf44",
        "releases/android-17/arm64-v8a/stable/20261009T000641Z-6143ceee.json" to "61aac8c3714da16e54c50934e253649dcd72112f41ea48f7f55287d9a75f2a17",
        "releases/android-17/arm64-v8a/stable/20261012T171351331663Z-f442b13b.json" to "9008ea8b34d910f269b5506605c30c66a8d2742a615d73721e5821316d58afb5",
        "releases/android-17/arm64-v8a/stable/20261110T032745213955Z-8fe5150b.json" to "c925e4a581e22a0ba8697617baa69a12e6bf7489e44dd3c60e9304d86a1a19fd",
        "releases/android-17/arm64-v8a/stable/20261119T183345015544Z-cf43ebdf.json" to "0e50b97207bbd5facc43378b226aabbb6c656986ee72569a9c9bea461323ffa5"
    )

}
