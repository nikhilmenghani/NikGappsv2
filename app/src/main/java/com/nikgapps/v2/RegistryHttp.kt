package com.nikgapps.v2

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class NetworkResponseException(val statusCode: Int) : IOException("Server returned HTTP $statusCode")
internal class MetadataSizeException : IOException("Metadata response exceeds the size limit")

/** Shared HTTPS transport. Authentication, registry parsing and download retries are later checkpoints. */
internal class RegistryHttp(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS).followSslRedirects(false).build()) {
    fun get(url: String, maxBytes: Int = 32 * 1024 * 1024): ByteArray {
        require(maxBytes > 0)
        val address = url.toHttpUrl()
        require(address.isHttps) { "HTTPS is required" }
        val request = Request.Builder().url(address).header("User-Agent", "NikGappsv2").build()
        return client.newCall(request).execute().use { response ->
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
    }
}

internal object RegistryPin {
    const val COMMIT = "9a0d39ffc59d3c13a3c1e70a543579e0c9e87504"
    const val ROOT = "https://raw.githubusercontent.com/nikgapps/nikgapps-packages/$COMMIT"
    const val INDEX = "$ROOT/releases/index.json"
    const val CATALOG_SHA256 = "717315a9d06bdf950c8f63d31d74c40e0629a65ffa2d4edf3ada0606e8425a41"
    const val APPSETS_SHA256 = "6bb845fea61d26468ca709496a9fe64787d31d0b90ef3191dee59f3250426c50"
    const val INDEX_SHA256 = "c16bf0972e1e7a42b98d79f1813c8ce0ef1e3a8674ec0ad3e097227ecafb6823"
}
