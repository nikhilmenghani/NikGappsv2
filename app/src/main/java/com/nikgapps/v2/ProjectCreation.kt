package com.nikgapps.v2

import android.content.Context
import org.json.JSONObject

internal const val REGULAR_PROJECT_LIMIT = 5
internal const val REGULAR_NAME_LIMIT = 20

internal fun newProject(name: String, version: AndroidVersion, supported: Set<AndroidVersion>, existingCount: Int): BuildProject {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Enter a project name" }
    require(trimmed.length <= REGULAR_NAME_LIMIT) { "Project names can have up to 20 characters" }
    require(version in supported) { "Choose a supported Android version" }
    check(existingCount < REGULAR_PROJECT_LIMIT) { "You can keep up to 5 projects" }
    return BuildProject(name = trimmed, androidVersion = version, architecture = Architecture.ARM64)
}

/** Offline target snapshot; app networking is introduced separately in phase 04. */
internal fun publishedProjectTargets(context: Context): List<AndroidVersion> {
    val json = context.assets.open("project-targets.json").bufferedReader().use { JSONObject(it.readText()) }
    require(json.getInt("schemaVersion") == 1 && json.getString("architecture") == "arm64-v8a")
    val entries = json.getJSONArray("androidVersions")
    val ids = (0 until entries.length()).map { entries.getString(it) }.toSet()
    return AndroidVersion.entries.filter { it.displayName.removePrefix("Android ").replace("12L", "12.1") in ids }
        .also { require(it.isNotEmpty()) }
}
