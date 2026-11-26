package com.nikgapps.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
internal fun OutdatedProjectsSection(projects: List<BuildProject>, card: @Composable (BuildProject) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        GlassSurface(Modifier.fillMaxWidth().semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), onClick = { expanded = !expanded }) {
            Row(Modifier.heightIn(min = 68.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.size(44.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderOff, null, tint = MaterialTheme.colorScheme.onErrorContainer) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Outdated projects", style = MaterialTheme.typography.titleMedium)
                    Text("${projects.size} ${if (projects.size == 1) "project" else "projects"}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Collapse outdated projects" else "Expand outdated projects")
            }
        }
        VerticalAccordion(expanded) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                projects.forEach { project -> key(project.id) { card(project) } }
            }
        }
    }
}
