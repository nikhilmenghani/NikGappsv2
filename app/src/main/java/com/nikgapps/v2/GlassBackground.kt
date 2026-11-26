package com.nikgapps.v2

enum class GlassBackground(val id: String, val label: String) {
    GOOGLE("google", "Google colors"), OCEAN("ocean", "Ocean"), FOREST("forest", "Forest"),
    DUSK("dusk", "Dusk"), GRAPHITE("graphite", "Graphite");
    companion object {
        fun fromId(id: String): GlassBackground = entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}
