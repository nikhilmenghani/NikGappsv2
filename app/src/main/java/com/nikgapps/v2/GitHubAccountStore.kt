package com.nikgapps.v2

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.*

/** Device-local encrypted token storage; account credentials are never part of project backups. */
internal class GitHubAccountStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("github-account", Context.MODE_PRIVATE)
    @Volatile private var tokenValue = ""
    var profile: GitHubProfile? by mutableStateOf(null)
        private set
    init { restore() }
    fun token() = tokenValue
    @Synchronized fun save(value: String, account: GitHubProfile) {
        val candidate = normalizedGitHubToken(value)
        require(account.login.isNotBlank() && account.login.length <= 256 && account.avatarUrl.length <= 2048)
        val payload = buildJsonObject { put("token", candidate); put("login", account.login); put("avatar", account.avatarUrl) }
            .toString().toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
            val encrypted = cipher.doFinal(payload)
            val envelope = buildJsonObject {
                put("version", 1); put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            }.toString()
            check(preferences.edit().putString("session", envelope).commit()) { "Could not save GitHub access" }
            tokenValue = candidate; profile = account
        } finally { payload.fill(0) }
    }
    private fun restore() {
        val envelope = preferences.getString("session", null) ?: return
        if (envelope.length > 65536) return
        var plaintext: ByteArray? = null
        try {
            val stored = Json.parseToJsonElement(envelope).jsonObject
            require(stored.getValue("version").jsonPrimitive.int == 1)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128,
                Base64.decode(stored.getValue("iv").jsonPrimitive.content, Base64.NO_WRAP)))
            plaintext = cipher.doFinal(Base64.decode(stored.getValue("ciphertext").jsonPrimitive.content, Base64.NO_WRAP))
            val decoded = Json.parseToJsonElement(plaintext.toString(Charsets.UTF_8)).jsonObject
            val token = normalizedGitHubToken(decoded.getValue("token").jsonPrimitive.content)
            val account = GitHubProfile(decoded.getValue("login").jsonPrimitive.content, decoded.getValue("avatar").jsonPrimitive.content)
            require(account.login.isNotBlank() && account.login.length <= 256)
            tokenValue = token; profile = account
        } catch (_: Exception) { tokenValue = ""; profile = null }
        finally { plaintext?.fill(0) }
    }
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(create) { "GitHub encryption key unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    companion object { private const val ALIAS = "NikGappsv2GitHubAccount" }
}
