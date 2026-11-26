package com.nikgapps.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private val themeLabels = listOf("Light", "Dark", "System")

@Composable
internal fun AppearanceSettings(mode: Int, onThemeChange: (Int) -> Unit, dynamicColor: Boolean, onDynamicChange: (Boolean) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showChoices by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showDynamicInfo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column {
                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.heightIn(min = 64.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Appearance", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        if (expanded) "Collapse Appearance" else "Expand Appearance")
                }
                if (expanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().clickable { onDynamicChange(!dynamicColor) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Use Dynamic Color", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { showDynamicInfo = true }) { Icon(AppIcons.Info, "About dynamic color") }
                        Switch(checked = dynamicColor, onCheckedChange = onDynamicChange)
                    }
                    Row(Modifier.fillMaxWidth().clickable { showChoices = true }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Theme", style = MaterialTheme.typography.bodyLarge)
                            Text(themeLabels.getOrElse(mode) { "System" }, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showInfo = true }) { Icon(AppIcons.Info, "About theme") }
                    }
                }
            }
        }
    }
    if (showChoices) AppDialog("Theme", { showChoices = false }) {
        themeLabels.forEachIndexed { index, label ->
            Row(Modifier.fillMaxWidth().selectable(selected = mode == index, role = Role.RadioButton,
                onClick = { onThemeChange(index); showChoices = false }).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected = mode == index, onClick = null)
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (showDynamicInfo) AppDialog("Dynamic color", { showDynamicInfo = false }) {
        Text("Android 12 and newer use your wallpaper's color palette. Earlier Android versions use the standard app colors.",
            style = MaterialTheme.typography.bodyMedium)
    }
    if (showInfo) AppDialog("Theme", { showInfo = false }) {
        Text("Choose a light or dark appearance, or follow your device's theme.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun AppDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    content()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp).heightIn(min = 48.dp)) { Text("Close") }
            }
        }
    }
}
