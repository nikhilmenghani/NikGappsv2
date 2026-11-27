package com.nikgapps.v2

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/** Test APK only. No credentials are compiled, logged, or passed as instrumentation arguments. */
internal object CredentialTransfer {
    private const val ALIAS = "NikGappsv2InvestigationTransfer"
    private fun file(context: Context) = File(context.getExternalFilesDir(null), "investigation-github.enc")
    fun publicKey(context: Context): String {
        clear(context)
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        generator.initialize(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(2048).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1).build())
        return Base64.encodeToString(generator.generateKeyPair().public.encoded, Base64.NO_WRAP)
    }
    fun take(context: Context): String {
        val encrypted = file(context)
        try {
            check(encrypted.length() == 256L)
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            cipher.init(Cipher.DECRYPT_MODE, store.getKey(ALIAS, null),
                OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT))
            return cipher.doFinal(encrypted.readBytes()).toString(Charsets.UTF_8).also { check(it.isNotBlank()) }
        } finally { clear(context) }
    }
    fun clear(context: Context) {
        file(context).delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(ALIAS)) deleteEntry(ALIAS) }
    }
}
