package com.nikgapps.v2

import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response

internal class RegistryAuthenticationException : IOException("Sign in to GitHub to download package data. Verified cached data remains available.")

/** Authentication belongs only to this repository's HTTPS API, never redirected asset hosts. */
internal object RegistryDelivery {
    const val REPOSITORY = "nikgapps/nikgapps-packages"
    const val API = "https://api.github.com/repos/$REPOSITORY"
    @Volatile var userToken: () -> String = { "" }
    fun trusted(url: HttpUrl) = url.isHttps && url.host == "api.github.com" && url.port == 443 &&
        url.encodedPath.startsWith("/repos/$REPOSITORY/")
    fun contents(path: String, commit: String = RegistryPin.COMMIT): String {
        require(path.isNotBlank() && path.split('/').none { it == ".." || it == "." } && !path.startsWith('/'))
        require(commit.matches(Regex("[a-f0-9]{40}")))
        return "$API/contents/$path?ref=$commit"
    }
    fun prepare(request: Request, token: () -> String = userToken): Request {
        if (request.url.host == "gitlab.com") throw MetadataException("Refresh package metadata to use the current download service.")
        val builder = request.newBuilder().removeHeader("Authorization")
        if (trusted(request.url)) {
            val value = token().trim()
            if (value.isBlank()) throw RegistryAuthenticationException()
            builder.header("Authorization", "Bearer $value")
                .header("Accept", when {
                    request.url.encodedPath.startsWith("/repos/$REPOSITORY/contents/") -> "application/vnd.github.raw+json"
                    request.url.encodedPath.startsWith("/repos/$REPOSITORY/releases/assets/") -> "application/octet-stream"
                    else -> "application/vnd.github+json"
                }).header("User-Agent", "NikGappsv2").header("X-GitHub-Api-Version", "2022-11-28")
        }
        return builder.build()
    }
    fun rateLimited(response: Response) = trusted(response.request.url) &&
        (response.code == 429 || response.code == 403 &&
            (response.header("X-RateLimit-Remaining") == "0" || response.header("Retry-After") != null))
}
