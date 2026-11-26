package com.nikgapps.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

internal val LocalRadialProjectMenu = staticCompositionLocalOf { true }

@Composable
internal fun ProjectActionMenu(projectName: String, expanded: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit,
    onCopy: () -> Unit, onDelete: () -> Unit) {
    if (LocalRadialProjectMenu.current) {
        if (expanded) ProjectRadialMenu(projectName, listOf(
            ProjectRadialAction("Edit project", Icons.Outlined.Edit, compactLabel = "Edit", onClick = onEdit),
            ProjectRadialAction("Copy project", Icons.Outlined.ContentCopy, compactLabel = "Copy", onClick = onCopy),
            ProjectRadialAction("Delete project", Icons.Outlined.DeleteOutline, destructive = true,
                compactLabel = "Delete", onClick = onDelete)
        ), onDismiss)
        return
    }
    val glass = LocalGlassEnabled.current
    val shape = RoundedCornerShape(12.dp)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = shape,
        containerColor = if (glass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (glass) 0.dp else MenuDefaults.TonalElevation,
        shadowElevation = if (glass) 0.dp else MenuDefaults.ShadowElevation) {
        GlassSurface(shape = shape, color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column {
                DropdownMenuItem(text = { Text("Edit project") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { onDismiss(); onEdit() })
                DropdownMenuItem(text = { Text("Copy project") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                    onClick = { onDismiss(); onCopy() })
                DropdownMenuItem(text = { Text("Delete project", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { onDismiss(); onDelete() })
            }
        }
    }
}

@Composable
internal fun DeleteProjectDialog(project: BuildProject, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface) {
            Column {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Delete project?", style = MaterialTheme.typography.titleLarge)
                    Text("Delete \"${project.name}\"? This cannot be undone.", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    GlassButton(onClick = onConfirm, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError)) { Text("Delete") }
                }
            }
        }
    }
}
