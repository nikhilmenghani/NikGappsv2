package com.nikgapps.v2

import java.io.IOException
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class GitHubAccountClientTest {
    private fun client(code: Int, body: String) = GitHubAccountClient(OkHttpClient.Builder().addInterceptor { chain ->
        assertEquals("https://api.github.com/user", chain.request().url.toString())
        assertEquals("Bearer fixture-token", chain.request().header("Authorization"))
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture").body(body.toResponseBody()).build()
    }.build())
    @Test fun publicIdentityDoesNotRequirePrivateProfileFields() {
        val account = client(200,"""{"login":"octocat","avatar_url":"https://avatars.githubusercontent.com/u/1","email":"unused@example.test","private_gists":42}""").verify(" fixture-token ")
        assertEquals(GitHubProfile("octocat", "https://avatars.githubusercontent.com/u/1"), account)
    }
    @Test fun invalidCredentialsRedirectsAndMalformedProfilesCannotEstablishAnAccount() {
        for (code in listOf(301,401,403,500)) assertThrows(GitHubAuthException::class.java) { client(code,"private response").verify("fixture-token") }
        for (text in listOf("{}","not json","""{"login":""}""")) assertThrows(GitHubAuthException::class.java) { client(200,text).verify("fixture-token") }
        assertThrows(GitHubAuthException::class.java) { client(200,"x".repeat(65537)).verify("fixture-token") }
    }
    @Test fun invalidHeaderCharactersFailBeforeSendingAnyRequest() {
        var sent = false
        val account = GitHubAccountClient(OkHttpClient.Builder().addInterceptor { sent = true; error("Must not run") }.build())
        for (value in listOf("", "bad\nheader", "two words", "x".repeat(4097))) assertThrows(GitHubAuthException::class.java) { account.verify(value) }
        assertFalse(sent)
    }
    @Test fun transportDetailsCannotLeakIntoTheUserMessage() {
        val account = GitHubAccountClient(OkHttpClient.Builder().addInterceptor { throw IOException("SENSITIVE fixture detail") }.build())
        val failure = assertThrows(GitHubAuthException::class.java) { account.verify("fixture-token") }
        assertFalse(failure.message!!.contains("SENSITIVE"));assertTrue(failure.message!!.contains("connection"))
    }
}
