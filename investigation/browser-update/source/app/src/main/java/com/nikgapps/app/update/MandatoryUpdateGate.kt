package com.nikgapps.app.update

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import com.nikgapps.app.presentation.ui.component.GlassSurface
import com.nikgapps.app.presentation.ui.component.GlassButton
import androidx.compose.material3.HorizontalDivider
import com.nikgapps.app.presentation.ui.component.GlassTonalButton as FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.nikgapps.app.presentation.ui.component.GlassTonalButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikgapps.app.utils.network.VersionFetcher
import com.nikgapps.dumps.getCurrentVersion

@Composable
fun MandatoryUpdateGate(
    enabled: Boolean,
    allowSettings: Boolean,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentVersion = remember { getCurrentVersion(context) }
    var checking by remember(enabled) { mutableStateOf(enabled) }
    var latestVersion by remember(enabled) { mutableStateOf<String?>(null) }
    var updateChangelog by remember(enabled) { mutableStateOf<List<ChangelogEntry>>(emptyList()) }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        checking = true
        val latest = VersionFetcher.fetchLatestVersion()
        latestVersion = latest.takeUnless { it == "Unknown" }
        if (latestVersion != null && VersionFetcher.isNewer(latest, currentVersion)) {
            updateChangelog = ChangelogRepository.between(
                ChangelogRepository.fetch(context, forceRefresh = true), currentVersion, latest
            )
        }
        checking = false
    }

    val updateRequired = enabled && latestVersion?.let {
        VersionFetcher.isNewer(it, currentVersion)
    } == true
    val showGate = !allowSettings && enabled && (checking || updateRequired)
    BackHandler(enabled = showGate) {}

    Box(Modifier.fillMaxSize()) {
        content()
        if (showGate && checking) {
            com.nikgapps.app.presentation.ui.component.StartupLogoScreen()
        } else if (showGate) {
            MandatoryUpdateScreen(
                checking = checking,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                changelog = updateChangelog,
                onUpdate = {
                    latestVersion?.let { AppUpdateBrowser.open(context, it) }
                },
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
internal fun MandatoryUpdateScreen(
    checking: Boolean,
    currentVersion: String,
    latestVersion: String?,
    changelog: List<ChangelogEntry>,
    onUpdate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showChangelog by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {}
        ),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (checking) CircularProgressIndicator()
                    else Icon(Icons.Outlined.SystemUpdateAlt, null)
                    Text(
                        if (checking) "Checking for updates..." else "Update required",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (checking) "Confirming that NikGapps is up to date."
                        else "NikGapps $latestVersion is available. Download it in your browser to update from $currentVersion and continue.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (!checking) {
                        FilledTonalButton(
                            onClick = { showChangelog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("What’s new") }
                        GlassButton(
                            onClick = onUpdate,
                            enabled = !checking,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Update now") }
                    }
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("NikGapps settings")
                    }
                }
            }
        }
    }
    if (showChangelog) {
        ChangelogDialog(
            title = "What’s new in ${latestVersion.orEmpty()}",
            entries = changelog,
            onDismiss = { showChangelog = false },
            onUpdate = {
                showChangelog = false
                onUpdate()
            }
        )
    }
}
