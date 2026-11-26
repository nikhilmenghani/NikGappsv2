package com.nikgapps.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    glass: Boolean = true,
    readable: Boolean = false,
    content: @Composable () -> Unit
) {
    ProjectGlassSurface(modifier = modifier, shape = shape, classicColor = color,
        contentColor = contentColor, classicBorder = border,
        classicTonalElevation = tonalElevation, classicShadowElevation = shadowElevation,
        onClick = onClick, enabled = enabled, glass = glass, readable = readable, content = content)
}

