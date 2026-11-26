@file:JvmName("NavigationContent")
package com.nikgapps.v2

import android.content.Context
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
        val preferences = remember(activity) { activity.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
        var themeMode by remember { mutableIntStateOf(preferences.getInt("theme_mode", 2)) }
        AppTheme(activity, themeMode, startup.ready) {
            if (startup.ready) NavigationScreen(themeMode) { selected ->
                preferences.edit().putInt("theme_mode", selected).apply()
                themeMode = selected
            }
            else AndroidView(factory = { startupView }, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun NavigationScreen(themeMode: Int, onThemeChange: (Int) -> Unit) {
    val navigation = rememberNavController()
    val entry by navigation.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "Projects"
    val labels = listOf("Projects", "Downloads", "Settings")
    val icons = listOf(Icons.Default.FolderSpecial, Icons.Default.FileDownload, Icons.Default.Settings)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 6.dp),
            horizontalArrangement = Arrangement.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface,
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
            Text(route, Modifier.padding(16.dp).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall)
            NavHost(navigation, startDestination = "Projects", modifier = Modifier.weight(1f)) {
                composable("Projects") { DestinationMessage("No projects yet") }
                composable("Downloads") { DestinationMessage("Coming soon") }
                composable("Settings") { AppearanceSettings(themeMode, onThemeChange) }
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
