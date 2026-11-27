package com.nikgapps.app.presentation.ui.screen

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.nikgapps.app.presentation.ui.component.glassTopAppBarColors
import com.nikgapps.app.presentation.ui.component.GlassScaffold
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.cards.PermissionsCard
import com.nikgapps.app.utils.constants.PermissionConstants
import com.nikgapps.app.utils.constants.PermissionConstants.permissionMap
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.app.utils.settings.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    autoCompleteWhenGranted: Boolean = true,
    onAllPermissionsGranted: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var allPermissionsGranted by remember {
        mutableStateOf(Permissions.hasAllRequiredPermissions(context))
    }
    LaunchedEffect(allPermissionsGranted, autoCompleteWhenGranted) {
        if (autoCompleteWhenGranted && allPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }
    GlassScaffold(
        topBar = {
            TopAppBar(colors = glassTopAppBarColors(),
                title = { Text("Permissions") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Set up access to build ZIPs and get updates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            permissionMap.keys.forEach { permissionName ->
                PermissionsManagerCard(
                    permissionName = permissionName,
                    onPermissionStatusChanged = { isGranted ->
                        allPermissionsGranted = isGranted && Permissions.hasAllRequiredPermissions(context)
                    }
                )
            }
        }
    }
}

@SuppressLint("InlinedApi")
@Composable
fun PermissionsManagerCard(
    permissionName: String = PermissionConstants.NOTIFICATIONS,
    onPermissionStatusChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            Permissions.isPermissionGranted(
                context,
                permissionName
            )
        )
    }
    var permanentlyDenied by remember {
        mutableStateOf(
            Permissions.isPermissionPermanentlyDenied(
                context,
                permissionName
            )
        )
    }
    val requestPermissionLauncher = Permissions.requestPermission(
        context = context,
        permissionName = permissionName
    ) { isGranted, isPermanentlyDenied ->
        hasPermission = isGranted
        permanentlyDenied = isPermanentlyDenied
        onPermissionStatusChanged(isGranted)

    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Permissions.isPermissionGranted(context, permissionName)
                permanentlyDenied =
                    Permissions.isPermissionPermanentlyDenied(context, permissionName)
                onPermissionStatusChanged(hasPermission)

            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PermissionsCard(
        title = when (permissionName) {
            PermissionConstants.STORAGE -> "Files and downloads"
            else -> permissionName
        },
        icon = when (permissionName) {
            PermissionConstants.STORAGE -> Icons.Outlined.FolderOpen
            else -> Icons.Outlined.Notifications
        },
        description = permissionMap[permissionName]?.rationale
            ?: "We need this permission for better functionality.",
        isPermissionGranted = hasPermission,
        permissionsText = if (permanentlyDenied ||
            (permissionName == PermissionConstants.STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R))
            "Open settings" else "Allow access",
        onRequestPermission = {
            if (!hasPermission) {
                when {
                    permissionName == PermissionConstants.STORAGE &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        Settings.openSettings(context, permissionMap[permissionName]?.action ?: "")
                    }
                    else -> {
                        if (permanentlyDenied) {
                            Settings.openSettings(context, permissionMap[permissionName]?.action ?: "")
                        } else {
                            val permissions =
                                permissionMap[permissionName]?.permission.orEmpty()
                            if (permissions.isNotEmpty()) {
                                Permissions.markPermissionAsRequested(context, permissionName)
                                requestPermissionLauncher?.launch(permissions.map { it }.toTypedArray())
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
fun PreviewDarkPermissionsScreen() {
    NikGappsThemePreview() {
        PermissionsCard(
            title = "Permission Title",
            description = "Permission Description",
            isPermissionGranted = false,
            permissionsText = "Request Permission",
            onRequestPermission = {})
    }
}


