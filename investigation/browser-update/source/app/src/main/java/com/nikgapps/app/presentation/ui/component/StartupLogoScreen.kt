package com.nikgapps.app.presentation.ui.component

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nikgapps.R
import com.nikgapps.app.presentation.theme.LocalStartupLogoCount

internal val LocalSystemStartupLogo = staticCompositionLocalOf<ImageBitmap?> { null }

/** A shared loading surface prevents separate connection/update cards flashing at startup. */
@Composable
internal fun StartupLogoScreen() {
    val startupLogos = LocalStartupLogoCount.current
    DisposableEffect(startupLogos) {
        startupLogos?.let { it.intValue++ }
        onDispose { startupLogos?.let { it.intValue-- } }
    }
    Surface(Modifier.fillMaxSize().clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}
    ).semantics {
        contentDescription = "Loading NikGapps"
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }, color = Color.Black) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val systemLogo = LocalSystemStartupLogo.current
            if (systemLogo != null) {
                with(LocalDensity.current) {
                    Image(systemLogo, null, Modifier.size(systemLogo.width.toDp(), systemLogo.height.toDp()))
                }
            } else {
                val logoSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 142.dp else 160.dp
                Image(painterResource(R.drawable.nikgapps_logo), null, Modifier.size(logoSize))
            }
        }
    }
}
