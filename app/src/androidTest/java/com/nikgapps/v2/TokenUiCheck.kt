package com.nikgapps.v2

import android.app.Activity
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.json.*

/** Test APK only. Plaintext crosses only in-process accessibility actions, never shell arguments or UI dumps. */
internal fun checkManualTokenUi(instrumentation: Instrumentation) {
    val context = instrumentation.targetContext
    var candidate = CredentialTransfer.take(context)
    try {
        instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
            if (root == null) return emptyList()
            return listOf(root) + (0 until root.childCount).flatMap { nodes(root.getChild(it)) }
        }
        fun snapshot() = nodes(automation.rootInActiveWindow)
        fun click(label: String) {
            repeat(30) {
                val matches = snapshot().filter { !it.isPassword && it.text?.toString() == label }
                val node = matches.maxByOrNull { n -> Rect().also(n::getBoundsInScreen).bottom }
                if (node != null) {
                    var action = node
                    while (!action!!.isClickable && action.parent != null) action = action.parent
                    if (action.isEnabled && action.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
                }
                SystemClock.sleep(200)
            }
            error("Required token UI control unavailable")
        }
        click("Settings")
        if (snapshot().none { !it.isPassword && it.text?.toString() == "GitHub token" }) click("Account")
        click("GitHub token")
        var field: AccessibilityNodeInfo? = null
        repeat(30) {
            if (field == null) field = snapshot().firstOrNull { it.className?.toString() == "android.widget.EditText" && it.isPassword }
            if (field == null) SystemClock.sleep(200)
        }
        checkNotNull(field) { "Password entry unavailable" }
        check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, candidate)
        })) { "Secure token entry failed" }
        SystemClock.sleep(300)
        click("Save token")
        var connected = false
        repeat(180) {
            if (!connected) {
                connected = snapshot().any { !it.isPassword && it.text?.toString()?.startsWith("Connected as ") == true }
                if (!connected) SystemClock.sleep(200)
            }
        }
        check(connected) { "Manual token verification did not finish successfully" }
        val restored = GitHubAccountStore(context)
        check(restored.token() == candidate && restored.profile != null)
        val preferences = context.getSharedPreferences("github-account", 0)
        val saved = checkNotNull(preferences.getString("session", null))
        check(!saved.contains(candidate)) { "Token must not be stored in plaintext" }
        val envelope = Json.parseToJsonElement(saved).jsonObject
        val bytes = Base64.decode(envelope.getValue("ciphertext").jsonPrimitive.content, Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        try {
            val changed = JsonObject(envelope + ("ciphertext" to JsonPrimitive(Base64.encodeToString(bytes, Base64.NO_WRAP))))
            check(preferences.edit().putString("session", changed.toString()).commit())
            val corrupted = GitHubAccountStore(context)
            check(corrupted.token().isEmpty() && corrupted.profile == null) { "Corrupt credentials must fail closed" }
        } finally { check(preferences.edit().putString("session", saved).commit()) }
        check(GitHubAccountStore(context).token() == candidate)
        instrumentation.finish(Activity.RESULT_OK, Bundle().apply {
            putString("manual_token_status", "passed: real UI verification, encrypted persistence and tamper rejection")
            putString("public_login", restored.profile!!.login)
        })
    } finally { candidate = ""; CredentialTransfer.clear(context) }
}
