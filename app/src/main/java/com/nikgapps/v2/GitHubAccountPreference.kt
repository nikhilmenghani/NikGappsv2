package com.nikgapps.v2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*

@Composable
internal fun GitHubAccountPreference(account: GitHubAccountStore, clientOverride: GitHubDeviceAuthClient? = null,
    openPageOverride: ((GitHubChallenge) -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = clientOverride ?: remember { GitHubDeviceAuthClient() }
    var open by remember { mutableStateOf(false) }
    var challenge by remember { mutableStateOf<GitHubChallenge?>(null) }
    var loading by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var startJob by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    fun dismiss() {
        generation++; startJob?.cancel(); startJob = null
        challenge = null; loading = false; finishing = false; open = false
    }
    fun openBrowser(active: GitHubChallenge) {
        if (openPageOverride != null) { openPageOverride(active); return }
        try {
            val clip = ClipData.newPlainText("GitHub sign-in code", active.userCode)
            clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(active.verificationUri)))
        } catch (_: Exception) { error = "Could not open GitHub. Copy the code and open github.com/login/device." }
    }
    fun begin() {
        if (loading || challenge != null) return
        loading = true; error = null
        val request = ++generation
        startJob = scope.launch {
            try {
                val next = client.start()
                if (open && request == generation) { challenge = next; openBrowser(next) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (request == generation) error = if (failure is GitHubSignInException) failure.message else "Could not connect to GitHub. Please try again." }
            finally { if (request == generation) { loading = false; startJob = null } }
        }
    }
    Row(Modifier.fillMaxWidth().clickable { error = null; open = true }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Outlined.AccountCircle, null)
        Column {
            Text("GitHub account", style = MaterialTheme.typography.bodyLarge)
            Text(account.profile?.let { "Signed in as ${it.login}" } ?: "Sign in with a one-time code",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (open) {
        LaunchedEffect(challenge) {
            val active = challenge ?: return@LaunchedEffect
            val request = generation
            try {
                val token = client.awaitToken(active)
                finishing = true
                val profile = GitHubAccountClient().verifyAfterSignIn(token)
                if (open && request == generation) {
                    withContext(Dispatchers.IO) { account.save(token, profile) }
                    challenge = null; open = false
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (request == generation) {
                    error = if (failure is GitHubSignInException || failure is GitHubAuthException) failure.message else "GitHub sign-in could not complete. Please try again."
                    challenge = null
                }
            } finally { if (request == generation) finishing = false }
        }
        AccountActionDialog("GitHub account", ::dismiss, content = {
            when {
                challenge != null -> {
                    Text("Open GitHub and enter this code to authorize NikGapps:", style = MaterialTheme.typography.bodyMedium)
                    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        SelectionContainer { Text(challenge!!.userCode, Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(if (finishing) "Finishing GitHub sign-in…" else "Waiting for authorization…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Preparing GitHub sign-in…", style = MaterialTheme.typography.bodyMedium)
                }
                account.profile != null -> Text("${account.profile!!.login} is connected. You can switch to another GitHub account.", style = MaterialTheme.typography.bodyMedium)
                else -> Text("Use a one-time code to sign in with GitHub. Your password is entered only on GitHub's website.", style = MaterialTheme.typography.bodyMedium)
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }, actions = {
            if (loading || finishing) GlassButton(onClick = ::dismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTonalButton(onClick = ::dismiss, modifier = Modifier.weight(1f)) { Text("Close") }
                GlassButton(onClick = { challenge?.let(::openBrowser) ?: begin() }, modifier = Modifier.weight(1f)) {
                    Text(if (challenge != null) "Open GitHub" else if (account.profile != null) "Switch account" else "Sign in", textAlign = TextAlign.Center)
                }
            }
        })
    }
}

@Composable
private fun AccountActionDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .85f
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(Modifier.fillMaxWidth().heightIn(max = maxHeight), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    content()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
            }
        }
    }
}
