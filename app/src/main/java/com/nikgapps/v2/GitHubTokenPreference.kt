package com.nikgapps.v2

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.*

@Composable
internal fun GitHubAccountSection(account: GitHubAccountStore) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf(false) }
    GlassSurface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Account", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Collapse Account" else "Expand Account")
            }
            VerticalAccordion(expanded) {
                Column {
                GitHubAccountPreference(account)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().clickable { editor = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.Key, null)
                    Column {
                        Text("GitHub token", style = MaterialTheme.typography.bodyLarge)
                        Text(account.profile?.let { "Connected as ${it.login}" } ?: "Not configured",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                }
            }
        }
    }
    if (editor) GitHubTokenTopSheet(account) { editor = false }
}

@Composable
private fun GitHubTokenTopSheet(account: GitHubAccountStore, onDismiss: () -> Unit) {
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var operation by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val uri = LocalUriHandler.current
    fun dismiss() {
        if (closing) return
        operation?.cancel(); token = ""; closing = true; visible = false; keyboard?.hide()
        scope.launch { delay(240); onDismiss() }
    }
    fun save() {
        if (verifying || closing || token.isBlank()) return
        val candidate = token.trim()
        verifying = true; error = null; keyboard?.hide()
        operation = scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) { GitHubAccountClient().verify(candidate) }
                withContext(Dispatchers.IO) { account.save(candidate, profile) }
                token = ""; onDismiss()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: GitHubAuthException) { error = failure.message }
            catch (_: Exception) { error = "Could not save GitHub access. Please try again." }
            finally { verifying = false }
        }
    }
    LaunchedEffect(Unit) { visible = true; delay(120); if (!closing) { focus.requestFocus(); keyboard?.show() } }
    Dialog(onDismissRequest = ::dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .42f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::dismiss)) {
            AnimatedVisibility(visible, enter = slideInVertically { -it / 2 } + fadeIn(), exit = slideOutVertically { -it / 2 } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().navigationBarsPadding().imePadding()) {
                GlassSurface(Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp), color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp, shadowElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GitHub token", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = { keyboard?.hide(); showHelp = true }) { Icon(AppIcons.Info, "Token permissions and help") }
                            IconButton(onClick = ::dismiss) { Icon(Icons.Outlined.Close, "Close token editor") }
                        }
                        OutlinedTextField(value = token, onValueChange = { token = it; error = null }, label = { Text("Personal access token") },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !verifying,
                            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                            keyboardActions = KeyboardActions(onDone = { save() }))
                        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassTonalButton(onClick = { keyboard?.hide(); uri.openUri("https://github.com/settings/personal-access-tokens/new") },
                                enabled = !verifying && !closing, modifier = Modifier.weight(1f)) { Text("Create token") }
                            GlassButton(onClick = ::save, enabled = !verifying && !closing && token.isNotBlank(), modifier = Modifier.weight(1f)) {
                                Text(if (verifying) "Verifying…" else "Save token")
                            }
                        }
                    }
                }
            }
        }
        if (showHelp) AppDialog("Token access", { showHelp = false }) {
            Text("For package downloads, NikGapps needs read-only access to public repositories. No private repository or write permissions are needed.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
