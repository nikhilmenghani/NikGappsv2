package com.nikgapps.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun LocalProjects(projects: List<BuildProject>, onEdit: (BuildProject) -> Unit, onCopy: (BuildProject) -> Unit, onDelete: (BuildProject) -> Unit) {
    if (projects.isEmpty()) Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("No projects yet", style = MaterialTheme.typography.headlineSmall)
    } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(projects, key = { it.id }) { project -> LocalProjectCard(project, onEdit, onCopy, onDelete) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalProjectCard(project: BuildProject, onEdit: (BuildProject) -> Unit,
    onCopy: (BuildProject) -> Unit, onDelete: (BuildProject) -> Unit) {
    var expanded by remember(project.id) { mutableStateOf(false) }
    var touch by remember(project.id) { mutableStateOf(Offset.Zero) }
    val glass = LocalGlassEnabled.current
    val lightGlass = glass && MaterialTheme.colorScheme.background.luminance() >= .5f
    val date = remember(project.createdAt) { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(project.createdAt)) }
    Box {
    GlassSurface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).pointerInput(Unit) {
        awaitEachGesture { touch = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position }
    }.combinedClickable(
        onClick = { expanded = true }, onLongClick = { expanded = true },
        onClickLabel = "Project actions", onLongClickLabel = "Project actions"), shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = MaterialTheme.shapes.medium,
                color = if (glass) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FolderSpecial, null, Modifier.size(if (glass) 28.dp else 24.dp),
                        tint = if (lightGlass) MaterialTheme.colorScheme.primary else if (glass) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(project.name, Modifier.weight(1f).alignByBaseline(), style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(project.androidVersion.displayName, Modifier.alignByBaseline(), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("0 apps · $date", Modifier.weight(1f).alignByBaseline(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("API ${project.androidVersion.apiLevel} · ARM64 build", Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    Box(Modifier.absoluteOffset { IntOffset(touch.x.roundToInt(), touch.y.roundToInt()) }.size(0.dp)) {
        ProjectActionMenu(project.name, expanded, { expanded = false }, { onEdit(project) }, { onCopy(project) }, { onDelete(project) })
    }
    }
}
