package com.nikgapps.v2

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal data class GlassAccent(
    val primary: Color, val onPrimary: Color,
    val container: Color, val onContainer: Color
)

/** Backgrounds and their control accents live together, rather than in individual widgets. */
internal data class GlassBackgroundPalette(
    val darkGround: Color,
    val lightGround: Color,
    val lights: List<Color>,
    val darkAccent: GlassAccent,
    val lightAccent: GlassAccent
)

internal val GoogleGlassPalette = GlassBackgroundPalette(
    Color(0xFF102A24), Color(0xFFF0F6F2),
    listOf(Color(0xFF4285F4), Color(0xFFFBBC05), Color(0xFFEA4335), Color(0xFF34A853), Color(0xFF4285F4)),
    GlassAccent(Color(0xFF89D5C6), Color(0xFF00382F), Color(0xFF005046), Color(0xFFA5F2E3)),
    GlassAccent(Color(0xFF006B5D), Color.White, Color(0xFFA5F2E3), Color(0xFF002019))
)
internal fun ColorScheme.withGlassAccent(dark: Boolean): ColorScheme {
    val palette = GoogleGlassPalette
    val accent = if (dark) palette.darkAccent else palette.lightAccent
    return copy(
        primary = accent.primary, onPrimary = accent.onPrimary,
        primaryContainer = accent.container, onPrimaryContainer = accent.onContainer,
        secondary = lerp(accent.primary, onSurface, .55f), onSecondary = accent.onPrimary,
        secondaryContainer = lerp(accent.container, surface, .35f),
        onSecondaryContainer = lerp(accent.onContainer, onSurface, .5f),
        surfaceTint = accent.primary,
        inversePrimary = if (dark) palette.lightAccent.primary else palette.darkAccent.primary
    )
}

