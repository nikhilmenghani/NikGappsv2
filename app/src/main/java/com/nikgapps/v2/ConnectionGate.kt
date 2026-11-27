package com.nikgapps.v2

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ConnectionGate(state: ConnectionState, onRetry: () -> Unit, onSettings: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(enabled = state != ConnectionState.ONLINE) { /* Keep the current destination while connection returns. */ }
    Box(Modifier.fillMaxSize()) {
        content()
        if (state == ConnectionState.OFFLINE) Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            ProjectsBackdrop { OfflineConnectionCard(onRetry, onSettings) }
        }
    }
}

@Composable
internal fun OfflineConnectionCard(onRetry: () -> Unit, onSettings: () -> Unit) {
    var help by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassSurface(Modifier.widthIn(max = 420.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
                    Text("You're offline", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { help = true }) { Icon(AppIcons.Info, "About connection") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTonalButton(onClick = onSettings, modifier = Modifier.weight(1f)) { Text("Wi-Fi settings") }
                    GlassButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Retry") }
                }
            }
        }
    }
    if (help) AppDialog("Connection", { help = false }) {
        Text("Connect to the internet to load NikGapps packages. Your projects stay saved while you reconnect.", style = MaterialTheme.typography.bodyMedium)
    }
}
