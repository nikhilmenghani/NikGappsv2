package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nikgapps.app.data.ReleaseNoticeRepository
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.registry.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal data class RegistryReleaseNotice(
    val release: ReleaseSummary,
    val day: LocalDate?,
    val changes: List<String>
)

@Composable
internal fun rememberRegistryReleaseNotice(
    online: Boolean,
    projects: List<BuildProject>,
    updates: Map<String, ProjectUpdate>,
    metadataByProject: Map<String, RegistryMetadata>
): RegistryReleaseNotice? {
    val context = LocalContext.current
    val preferences = remember(context) { ReleaseNoticeRepository(context) }
    val latest = remember(metadataByProject) {
        metadataByProject.values.mapNotNull { metadata ->
            val release = metadata.release ?: return@mapNotNull null
            val index = metadata.releaseIndex ?: return@mapNotNull null
            val latestId = index.latest[release.androidVersion]?.get(release.channel)?.get(release.architecture)
            index.releases.firstOrNull { it.id == latestId && it.id == release.id }
        }.distinctBy { it.id }
    }
    val known = remember(latest, projects) { latest.filter { releaseKnownToProjects(it, projects) } }
    LaunchedEffect(known) { known.forEach { preferences.acknowledge(it.id) } }
    val release = remember(latest, known, projects, updates) {
        latest.filter { it.channel == "stable" && releaseAvailableToProjects(it, projects, updates) }
            .maxByOrNull { it.createdAt }
            ?.takeUnless { it in known || preferences.dismissed(it.id) }
    } ?: return null
    return key(release.id) {
        val prepared by produceState<RegistryReleaseNotice?>(null, release, online) {
            val day = runCatching { Instant.parse(release.createdAt).atOffset(ZoneOffset.UTC).toLocalDate() }.getOrNull()
            val changes = day?.let { PackageReleaseNotesRepository.changesOn(context.cacheDir, it, online) }.orEmpty()
            value = RegistryReleaseNotice(release, day, changes)
        }
        prepared
    }
}

@Composable
internal fun RegistryReleaseBanner(notice: RegistryReleaseNotice?) {
    val context = LocalContext.current
    val preferences = remember(context) { ReleaseNoticeRepository(context) }
    var dismissTarget by remember { mutableStateOf<ReleaseSummary?>(null) }
    var dismissedId by remember { mutableStateOf<String?>(null) }
    val visibleNotice = notice?.takeUnless { it.release.id == dismissedId || preferences.dismissed(it.release.id) }
    val visibility = remember { MutableTransitionState(false) }.apply { targetState = visibleNotice != null }
    AnimatedVisibility(
        visibleState = visibility,
        enter = expandVertically(tween(240), expandFrom = Alignment.Top),
        exit = shrinkVertically(tween(240), shrinkTowards = Alignment.Top)
    ) {
        var lastNotice by remember { mutableStateOf(visibleNotice) }
        SideEffect { if (visibleNotice != null) lastNotice = visibleNotice }
        (visibleNotice ?: lastNotice)?.let { displayed ->
            key(displayed.release.id) { RegistryReleaseCard(displayed) { dismissTarget = displayed.release } }
        }
    }
    dismissTarget?.let { target ->
        DismissRegistryReleaseDialog(onDismiss = { dismissTarget = null }, onConfirm = {
            preferences.dismiss(target.id)
            dismissedId = target.id
            dismissTarget = null
        })
    }
}

@Composable
private fun RegistryReleaseCard(notice: RegistryReleaseNotice, onDismiss: () -> Unit) {
    val published = notice.release
    var expanded by rememberSaveable { mutableStateOf(false) }
    var changes by remember { mutableStateOf(notice.changes) }
    val locale = LocalLocale.current.platformLocale
    val releaseDate = remember(notice.day, locale) {
        notice.day?.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'UTC'", locale))
    }
    ProjectGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        classicColor = MaterialTheme.colorScheme.surfaceContainerLow,
        classicBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        classicTonalElevation = 0.dp
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide release details" else "Show release details") {
                    if (!expanded) changes = notice.changes
                    expanded = !expanded
                }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Icon(Icons.Outlined.SystemUpdateAlt, contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("NikGapps release available", style = MaterialTheme.typography.titleSmall)
                    Text(listOfNotNull("Android ${published.androidVersion}", releaseDate).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded,
                enter = expandVertically(tween(240), expandFrom = Alignment.Top),
                exit = shrinkVertically(tween(240), shrinkTowards = Alignment.Top)
            ) {
                Column {
                    Text("Updated packages and fixes are available. Press and hold a compatible project and choose Build latest ZIP to update.",
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (changes.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Changelog", style = MaterialTheme.typography.labelLarge)
                            changes.forEach { change ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(change, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
                        GlassTonalButton(onClick = onDismiss, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text("Dismiss", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DismissRegistryReleaseDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dismiss this release announcement?", style = MaterialTheme.typography.titleLarge)
                    Text("Project update indicators will remain visible. You'll see another announcement when a newer release is available.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("Keep", style = MaterialTheme.typography.labelMedium)
                    }
                    FilledTonalButton(onClick = onConfirm, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
