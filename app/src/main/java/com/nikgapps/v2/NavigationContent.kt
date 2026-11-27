@file:JvmName("NavigationContent")
package com.nikgapps.v2

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StartupState {
    var ready by mutableStateOf(false)
}

fun installNavigation(activity: ComponentActivity, startupView: View, startup: StartupState) {
    RegistryAvailability.initialize(activity)
    val account = GitHubAccountStore(activity.applicationContext)
    RegistryDelivery.userToken = account::token
    activity.setContent {
        val preferences = remember(activity.applicationContext) { AppearancePreferences(activity.applicationContext) }
        val connection = remember(activity.applicationContext) { NetworkConnection(activity.applicationContext) }
        val networkState by connection.state.collectAsState()
        DisposableEffect(connection) { connection.start(); onDispose { connection.close() } }
        val ready = startup.ready && networkState != ConnectionState.CHECKING
        CompositionLocalProvider(LocalGlassEnabled provides preferences.liquidGlass,
            LocalGlassBackground provides preferences.background,
            LocalRadialProjectMenu provides preferences.radialProjectMenu) {
            AppTheme(activity, preferences.themeMode, preferences.dynamicColor, ready) {
                ProjectsBackdrop {
                    Box(Modifier.fillMaxSize()) {
                        if (startup.ready) ConnectionGate(networkState, connection::retry, onSettings = {
                            activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                        }) { NavigationScreen(preferences, account) }
                        if (!ready) AndroidView(factory = { startupView }, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationScreen(preferences: AppearancePreferences, account: GitHubAccountStore) {
    val dark = usesDarkTheme(preferences.themeMode)
    val context = LocalContext.current
    val repository = remember(context) { BuildProjectRepository(context) }
    var projects by remember(context) { mutableStateOf(repository.getProjects()) }
    val targets = remember(context) { publishedProjectTargets(context) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var limitReached by remember { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun perform(action: () -> List<BuildProject>) {
        scope.launch {
            try { projects = withContext(Dispatchers.IO) { action() } }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { actionError = true }
        }
    }
    val navigation = rememberNavController()
    val entry by navigation.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "Projects"
    val labels = listOf("Projects", "Downloads", "Settings")
    val icons = listOf(Icons.Default.FolderSpecial, Icons.Default.FileDownload, Icons.Default.Settings)
    Scaffold(containerColor = if (LocalGlassEnabled.current) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            GlassSurface(modifier = Modifier.weight(1f, fill = false).widthIn(max = 264.dp),
                shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.width(264.dp).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    labels.forEachIndexed { index, label ->
                        val active = route == label
                        Surface(onClick = {
                            if (!active) navigation.navigate(label) {
                                popUpTo("Projects") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .semantics { role = Role.Tab; selected = active },
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent,
                            contentColor = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) {
                            Column(Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)) {
                                Icon(icons[index], null, Modifier.size(22.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            GlassSurface(shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = { if (projects.size >= REGULAR_PROJECT_LIMIT) limitReached = true else creating = true }) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Create project") }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(route, Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall)
                if (route == "Projects") GlassSurface(shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    IconButton(onClick = { preferences.changeTheme(if (dark) 0 else 1) }, modifier = Modifier.size(48.dp)) {
                        Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            if (dark) "Switch to light theme" else "Switch to dark theme",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            NavHost(navigation, startDestination = "Projects", modifier = Modifier.weight(1f)) {
                composable("Projects") { LocalProjects(projects,
                    onEdit = { editingId = it.id },
                    onCopy = { project ->
                        if (projects.size >= REGULAR_PROJECT_LIMIT) limitReached = true
                        else perform { repository.copyProject(project.id, targets.toSet()) }
                    }, onDelete = { deletingId = it.id }) }
                composable("Downloads") { DestinationMessage("Coming soon") }
                composable("Settings") { AppearanceSettings(preferences) { GitHubAccountSection(account) } }
            }
        }
    }
    if (creating) ProjectCreationSheet(repository, targets, onSaved = { projects = it; creating = false }, onDismiss = { creating = false })
    projects.firstOrNull { it.id == editingId }?.let { project ->
        ProjectCreationSheet(repository, targets, onSaved = { projects = it; editingId = null },
            onDismiss = { editingId = null }, project = project)
    }
    projects.firstOrNull { it.id == deletingId }?.let { project ->
        DeleteProjectDialog(project, onDismiss = { deletingId = null }, onConfirm = {
            deletingId = null
            perform { repository.deleteProject(project.id) }
        })
    }
    if (actionError) AppDialog("Couldn't update project", { actionError = false }) {
        Text("Please try again.", style = MaterialTheme.typography.bodyMedium)
    }
    if (limitReached) AppDialog("Project limit reached", { limitReached = false }) {
        Text("You can keep up to 5 projects. Delete a project before creating another.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DestinationMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.headlineSmall)
    }
}
