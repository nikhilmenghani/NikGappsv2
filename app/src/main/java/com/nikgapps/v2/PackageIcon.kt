package com.nikgapps.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import java.io.File

/** Registry images with a theme-tinted standard icon while missing, loading or undecodable. */
@Composable
internal fun PackageIcon(packageName: String?, modifier: Modifier = Modifier, repository: RegistryIconRepository? = null) {
    val context = LocalContext.current
    val source = repository ?: remember(context.applicationContext) { RegistryIconRepository(File(context.cacheDir, "registry-icons")) }
    val file by produceState<File?>(null, source, packageName) {
        value = if (packageName.isNullOrBlank()) null else source.icon(packageName)
    }
    var decoded by remember(file) { mutableStateOf(false) }
    Box(modifier) {
        if (!decoded) Icon(Icons.Outlined.Android, null, Modifier.matchParentSize())
        if (file != null) AsyncImage(file, null, Modifier.matchParentSize(),
            onSuccess = { decoded = true }, onError = { decoded = false })
    }
}
