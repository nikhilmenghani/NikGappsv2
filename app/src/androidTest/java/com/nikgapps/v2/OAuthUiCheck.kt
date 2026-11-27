package com.nikgapps.v2

import android.app.Activity
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo

/** Test APK only. Browser fields/codes/passwords are never dumped, captured or returned. */
internal fun checkBrowserOAuth(instrumentation: Instrumentation) {
    val context = instrumentation.targetContext
    val before = GitHubAccountStore(context)
    val oldToken = before.token()
    val owner = checkNotNull(before.profile) { "Configure the authorized test account first" }
    check(oldToken.isNotBlank())
    instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (root == null) emptyList()
        else listOf(root) + (0 until root.childCount).flatMap { nodes(root.getChild(it)) }
    fun snapshot() = nodes(automation.rootInActiveWindow).filter { it.isVisibleToUser }
    fun publicText(node: AccessibilityNodeInfo) = if (node.isPassword || node.isEditable) "" else node.text?.toString().orEmpty()
    fun click(node: AccessibilityNodeInfo): Boolean {
        var action = node
        while (!action.isClickable && action.parent != null) action = action.parent
        return action.isEnabled && action.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    fun clickText(label: String) {
        repeat(30) {
            val node = snapshot().filter { publicText(it) == label }.maxByOrNull { n -> Rect().also(n::getBoundsInScreen).bottom }
            if (node != null && click(node)) return
            SystemClock.sleep(200)
        }
        error("Required sign-in control unavailable")
    }
    fun progress(phase: String) = instrumentation.sendStatus(1, Bundle().apply { putString("oauth_phase", phase) })
    clickText("Settings")
    if (snapshot().none { publicText(it) == "GitHub account" }) clickText("Account")
    clickText("GitHub account"); clickText("Switch account")
    val deadline = SystemClock.elapsedRealtime() + 10 * 60 * 1000
    var pasted = false
    var submitted = false
    var authorized = false
    var reported = ""
    try {
        while (SystemClock.elapsedRealtime() < deadline) {
            val saved = GitHubAccountStore(context)
            if (saved.token().isNotBlank() && saved.token() != oldToken) {
                if (saved.profile?.login != owner.login) {
                    saved.save(oldToken, owner)
                    error("The browser used a different account; the original test session was restored")
                }
                instrumentation.finish(Activity.RESULT_OK, Bundle().apply {
                    putString("oauth_status", "passed: browser device-code authorization, verified identity and encrypted token persistence")
                    putString("public_login", owner.login)
                })
                return
            }
            val current = snapshot()
            if (current.any { n -> publicText(n).let { it.contains("may be harmful", true) || it.contains("device is at risk", true) || it.contains("blocked to protect your device", true) } }) {
                progress("harmful_warning_observed")
                error("Harmful-app warning observed during separate browser test; stop the cumulative port")
            }
            val browser = current.firstOrNull()?.packageName?.toString().orEmpty()
            if (browser in setOf("com.android.chrome", "org.lineageos.jelly", "org.mozilla.firefox")) {
                val texts = current.map(::publicText)
                val loginRequired = current.any { it.isPassword } || texts.any { it.contains("Sign in to GitHub", ignoreCase = true) }
                val trustedOrigin = current.any { n ->
                    if (!n.isEditable || n.isPassword) false else {
                        val address = n.text?.toString().orEmpty()
                        address.startsWith("https://github.com/") || address.startsWith("github.com/")
                    }
                }
                var phase = "browser_confirmation_required"
                if (loginRequired) phase = "browser_sign_in_required"
                else if (trustedOrigin) {
                    val devicePage = texts.any { it.contains("device activation", true) || it.contains("Enter the code", true) }
                    if (devicePage && !pasted) {
                        val field = current.firstOrNull { n -> n.isEditable && !n.isPassword &&
                            generateSequence(n.parent) { it.parent }.take(16).any { it.className?.toString()?.contains("WebView") == true } }
                        if (field != null) {
                            field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            pasted = field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                        }
                        if (!pasted) phase = "browser_code_paste_required"
                    }
                    if (pasted && !submitted) {
                        val button = current.firstOrNull { publicText(it).equals("Continue", true) }
                        if (button != null && click(button)) submitted = true
                    }
                    val expected = "Signed in as ${owner.login}"
                    val ownerConfirmed = current.any { n -> !n.isPassword && !n.isEditable &&
                        (publicText(n).contains(expected, true) || n.contentDescription?.toString()?.contains(expected, true) == true) }
                    val approve = current.firstOrNull { publicText(it).startsWith("Authorize NikGapps", true) }
                    if (!authorized && ownerConfirmed && approve != null) authorized = click(approve)
                    phase = when {
                        devicePage && !pasted -> "browser_code_paste_required"
                        devicePage && !submitted -> "browser_code_continue_required"
                        authorized -> "browser_authorized"
                        else -> "browser_authorization_required"
                    }
                }
                if (phase != reported) { progress(phase); reported = phase }
            }
            SystemClock.sleep(500)
        }
        error("Browser authorization is still required; no OAuth success was claimed")
    } finally {
        // Preserve the previously working session on cancellation/failure; never revoke the shared repository PAT.
        val current = GitHubAccountStore(context)
        if (current.token().isBlank()) current.save(oldToken, owner)
    }
}
