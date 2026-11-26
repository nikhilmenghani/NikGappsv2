package com.nikgapps.v2

import android.content.Context
import androidx.compose.runtime.*

internal class AppearancePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    var themeMode by mutableIntStateOf(preferences.getInt("theme_mode", 2))
        private set
    var dynamicColor by mutableStateOf(preferences.getBoolean("dynamic_color", true))
        private set
    var liquidGlass by mutableStateOf(preferences.getBoolean("liquid_glass", true))
        private set

    var background by mutableStateOf(GlassBackground.fromId(preferences.getString("glass_background", "google").orEmpty()))
        private set

    fun changeBackground(value: GlassBackground) {
        preferences.edit().putString("glass_background", value.id).apply()
        background = value
    }

    fun changeTheme(value: Int) {
        preferences.edit().putInt("theme_mode", value).apply()
        themeMode = value
    }

    fun changeDynamicColor(value: Boolean) {
        preferences.edit().putBoolean("dynamic_color", value).apply()
        dynamicColor = value
    }

    fun changeLiquidGlass(value: Boolean) {
        preferences.edit().putBoolean("liquid_glass", value).apply()
        liquidGlass = value
    }
}
