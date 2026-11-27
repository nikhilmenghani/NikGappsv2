package com.nikgapps.v2

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class GitHubProfile(val login: String, val avatarUrl: String)
internal class GitHubAuthException(message: String, val retryable: Boolean = false) : IOException(message)
internal fun normalizedGitHubToken(value: String): String {
    val candidate = value.trim()
    if (candidate.isEmpty() || candidate.length > 4096 || candidate.any { it.code !in 33..126 })
        throw GitHubAuthException("Enter a valid GitHub personal access token.")
    return candidate
}

/** Only public account identity is used; raw responses, credentials and private profile fields are not logged. */
internal class GitHubAccountClient(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
    .followRedirects(false).followSslRedirects(false).build()) {
    suspend fun verifyAfterSignIn(value: String, pause: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }): GitHubProfile {
        repeat(12) { attempt ->
            try { return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { verify(value) } }
            catch (failure: GitHubAuthException) {
                if (!failure.retryable || attempt == 11) throw failure
                pause(5000)
            }
        }
        error("Account verification did not finish")
    }
    fun verify(value: String): GitHubProfile {
        val token = normalizedGitHubToken(value)
        val request = Request.Builder().url("https://api.github.com/user")
            .header("Authorization", "Bearer $token").header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28").header("User-Agent", "NikGappsv2").build()
        try {
            return client.newCall(request).execute().use { response ->
                if (response.code == 401) throw GitHubAuthException("This GitHub token is invalid or has expired.")
                if (!response.isSuccessful) throw GitHubAuthException("GitHub could not verify this token (HTTP ${response.code}). Try again later.", retryable = response.code in 500..599)
                if (response.body.contentLength() > 65536) throw GitHubAuthException("GitHub returned an unexpected account response.")
                val bytes = response.body.byteStream().use { it.readNBytesBounded(65536) }
                try {
                    val profile = Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
                    val login = profile.getValue("login").jsonPrimitive.content
                    require(login.isNotBlank() && login.length <= 256 && login.none { it.isISOControl() })
                    val avatar = profile["avatar_url"]?.jsonPrimitive?.contentOrNull.orEmpty().takeIf { value ->
                        value.length <= 2048 && value.toHttpUrlOrNull()?.let { it.isHttps && it.port == 443 &&
                            it.host in setOf("avatars.githubusercontent.com", "github.com") } == true
                    }.orEmpty()
                    GitHubProfile(login, avatar)
                } catch (_: Exception) { throw GitHubAuthException("GitHub returned incomplete account information.") }
            }
        } catch (failure: GitHubAuthException) { throw failure }
        catch (_: IOException) { throw GitHubAuthException("Could not reach GitHub. Check your connection and try again.", retryable = true) }
    }
}
private fun java.io.InputStream.readNBytesBounded(maximum: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (output.size() + count > maximum) throw GitHubAuthException("GitHub returned an unexpected account response.")
        output.write(buffer, 0, count)
    }
}
