package com.nikgapps.v2

import android.os.SystemClock
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

internal class GitHubSignInException(message: String) : IOException(message)
internal data class GitHubChallenge(val deviceCode: String, val userCode: String, val verificationUri: String,
    val expiresIn: Int, val interval: Int) {
    override fun toString() = "GitHubChallenge(codes redacted)"
}
private class SignInHttpException(val status: Int, val waitMillis: Long) : IOException("GitHub sign-in request failed (HTTP $status).")

/** Public-client device flow. No client secret, embedded credential, or password collection. */
internal class GitHubDeviceAuthClient(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
    .followRedirects(false).followSslRedirects(false).build(),
    private val now: () -> Long = SystemClock::elapsedRealtime,
    private val pause: suspend (Long) -> Unit = { delay(it) }) {
    suspend fun start(clientId: String = GITHUB_OAUTH_CLIENT_ID): GitHubChallenge = withContext(Dispatchers.IO) {
        val result = post("https://github.com/login/device/code", FormBody.Builder().add("client_id", clientId).build())
        try {
            val code = result.getValue("device_code").jsonPrimitive.content
            val user = result.getValue("user_code").jsonPrimitive.content
            val uri = result.getValue("verification_uri").jsonPrimitive.content
            val expires = result.getValue("expires_in").jsonPrimitive.int
            val interval = result["interval"]?.jsonPrimitive?.intOrNull ?: 5
            require(code.isNotBlank() && code.length <= 512 && code.none { it.isISOControl() })
            require(user.isNotBlank() && user.length <= 32 && user.none { it.isISOControl() })
            require(uri == "https://github.com/login/device" && expires in 1..3600 && interval in 1..300)
            GitHubChallenge(code, user, uri, expires, interval.coerceAtLeast(5))
        } catch (_: Exception) { throw GitHubSignInException("Could not prepare GitHub sign-in. Please try again.") }
    }
    suspend fun awaitToken(challenge: GitHubChallenge, clientId: String = GITHUB_OAUTH_CLIENT_ID): String {
        val deadline = now() + challenge.expiresIn * 1000L
        var interval = challenge.interval.coerceAtLeast(5)
        while (now() < deadline) {
            pause(minOf(interval * 1000L, (deadline - now()).coerceAtLeast(0)))
            if (now() >= deadline) break
            val result = try {
                withContext(Dispatchers.IO) {
                    post("https://github.com/login/oauth/access_token", FormBody.Builder().add("client_id", clientId)
                        .add("device_code", challenge.deviceCode).add("grant_type", "urn:ietf:params:oauth:grant-type:device_code").build())
                }
            } catch (failure: SignInHttpException) {
                if (failure.status == 429) { pause(minOf(maxOf(60_000L, failure.waitMillis), (deadline - now()).coerceAtLeast(0))); continue }
                if (failure.status !in 500..599) throw failure
                continue
            } catch (failure: GitHubSignInException) { throw failure }
            catch (_: IOException) { continue }
            result["access_token"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
                try { return normalizedGitHubToken(it) }
                catch (_: GitHubAuthException) { throw GitHubSignInException("GitHub returned an unexpected sign-in response.") }
            }
            when (result["error"]?.jsonPrimitive?.contentOrNull) {
                "authorization_pending" -> Unit
                "slow_down" -> interval = maxOf((interval.toLong() + 5).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    (result["interval"]?.jsonPrimitive?.intOrNull ?: 0).coerceAtLeast(0))
                "expired_token" -> throw GitHubSignInException("GitHub sign-in code expired. Try again.")
                "access_denied" -> throw GitHubSignInException("GitHub sign-in was denied.")
                else -> throw GitHubSignInException("GitHub sign-in could not complete. Please try again.")
            }
        }
        throw GitHubSignInException("GitHub sign-in code expired. Try again.")
    }
    private fun post(url: String, body: FormBody): JsonObject {
        val request = Request.Builder().url(url).post(body).header("Accept", "application/json").header("User-Agent", "NikGappsv2").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val wall = System.currentTimeMillis()
                throw SignInHttpException(response.code, registryRetryAt(response.header("Retry-After"), response.header("X-RateLimit-Reset"), wall) - wall)
            }
            if (response.body.contentLength() > 65536) throw GitHubSignInException("GitHub returned an unexpected sign-in response.")
            val bytes = response.body.byteStream().use { input ->
                val output = ByteArrayOutputStream(); val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    if (output.size() + count > 65536) throw GitHubSignInException("GitHub returned an unexpected sign-in response.")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            try { Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject }
            catch (_: Exception) { throw GitHubSignInException("GitHub returned an unexpected sign-in response.") }
        }
    }
}
