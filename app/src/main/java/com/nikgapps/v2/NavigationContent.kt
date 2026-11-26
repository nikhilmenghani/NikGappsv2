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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.compose.*

class StartupState {
    var ready by mutableStateOf(false)
}

fun installNavigation(activity: ComponentActivity, startupView: View, startup: StartupState) {
    activity.setContent {
        val preferences = remember(activity.applicationContext) { AppearancePreferences(activity.applicationContext) }
        CompositionLocalProvider(LocalGlassEnabled provides preferences.liquidGlass) {
            AppTheme(activity, preferences.themeMode, preferences.dynamicColor, startup.ready) {
                ProjectsBackdrop {
                    if (startup.ready) NavigationScreen(preferences)
                    else AndroidView(factory = { startupView }, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun NavigationScreen(preferences: AppearancePreferences) {
    val dark = usesDarkTheme(preferences.themeMode)
    val navigation = rememberNavController()
    val entry by navigation.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "Projects"
    val labels = listOf("Projects", "Downloads", "Settings")
    val icons = listOf(Icons.Default.FolderSpecial, Icons.Default.FileDownload, Icons.Default.Settings)
    Scaffold(containerColor = if (LocalGlassEnabled.current) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 6.dp),
            horizontalArrangement = Arrangement.Center) {
            GlassSurface(shape = CircleShape, color = MaterialTheme.colorScheme.surface,
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
                composable("Projects") { DestinationMessage("No projects yet") }
                composable("Downloads") { DestinationMessage("Coming soon") }
                composable("Settings") { AppearanceSettings(preferences) }
            }
        }
    }
}

@Composable
private fun DestinationMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.headlineSmall)
    }
}
