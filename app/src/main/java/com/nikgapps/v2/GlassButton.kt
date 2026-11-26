package com.nikgapps.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
internal fun GlassButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp), content: @Composable RowScope.() -> Unit) {
    val glass = LocalGlassEnabled.current
    val classic = ButtonDefaults.buttonColors()
    val colors = if (glass) ButtonDefaults.buttonColors(
        containerColor = classic.containerColor.copy(alpha = .12f),
        contentColor = classic.containerColor,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .10f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
    ) else classic
    GlassSurface(modifier.heightIn(min = 48.dp), shape = shape, color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp),
            shape = shape, colors = colors, elevation = if (glass) null else ButtonDefaults.buttonElevation(),
            contentPadding = if (glass) PaddingValues(12.dp, 8.dp) else ButtonDefaults.ContentPadding,
            content = content)
    }
}
