package com.nikgapps.v2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** The persisted identifiers match NikGapps; later capabilities extend this record in their own checkpoints. */
internal data class BuildProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val androidVersion: AndroidVersion,
    val architecture: Architecture = Architecture.ARM64,
    val createdAt: Long = System.currentTimeMillis()
)

internal enum class AndroidVersion(val displayName: String, val apiLevel: Int) {
    ANDROID_10("Android 10", 29), ANDROID_11("Android 11", 30), ANDROID_12("Android 12", 31),
    ANDROID_12L("Android 12L", 32), ANDROID_13("Android 13", 33), ANDROID_14("Android 14", 34),
    ANDROID_15("Android 15", 35), ANDROID_16("Android 16", 36), ANDROID_17("Android 17", 37)
}
internal enum class Architecture(val displayName: String, val value: String) {
    ARM64("ARM64", "arm64-v8a")
}

internal data class DecodedProjects(val projects: List<BuildProject>, val damaged: Boolean, val migrateTimestamp: Boolean)

internal object ProjectCodec {
    fun encode(projects: List<BuildProject>): String {
        require(projects.map { it.id }.distinct().size == projects.size) { "Duplicate project identifiers" }
        return JSONArray().apply {
            projects.forEach { p -> put(JSONObject().put("id", p.id).put("name", p.name)
                .put("androidVersion", p.androidVersion.name).put("architecture", p.architecture.name)
                .put("createdAt", p.createdAt)) }
        }.toString()
    }

    fun decode(text: String, now: Long): DecodedProjects {
        var damaged = false
        var migrate = false
        val ids = mutableSetOf<String>()
        val records = runCatching { JSONArray(text) }.getOrNull()
            ?: return DecodedProjects(emptyList(), damaged = true, migrateTimestamp = false)
        val projects = (0 until records.length()).mapNotNull { index ->
            try {
                val entry = records.getJSONObject(index)
                val id = entry.getString("id")
                require(id.isNotBlank() && id !in ids) { "Invalid project identifier" }
                val project = BuildProject(id = id, name = entry.getString("name"),
                    androidVersion = AndroidVersion.valueOf(entry.getString("androidVersion")),
                    architecture = Architecture.valueOf(entry.getString("architecture")),
                    createdAt = if (entry.has("createdAt")) entry.getLong("createdAt") else now)
                if (!entry.has("createdAt")) migrate = true
                ids.add(id)
                project
            } catch (_: Exception) { damaged = true; null }
        }
        return DecodedProjects(projects, damaged, migrate)
    }
}

internal interface ProjectStorage {
    fun read(): String?
    fun write(text: String): Boolean
    fun preserveRecovery(text: String): Boolean
}

private class PreferenceProjectStorage(context: Context) : ProjectStorage {
    private val preferences = context.applicationContext.getSharedPreferences("build_projects", Context.MODE_PRIVATE)
    override fun read(): String? = preferences.getString("projects", null)
    override fun write(text: String): Boolean = preferences.edit().putString("projects", text).commit()
    override fun preserveRecovery(text: String): Boolean {
        if (preferences.getString("recovery_backup", null) == text) return true
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return preferences.edit().putString("recovery_backup:$digest", text)
            .putString("recovery_backup", text).commit()
    }
}

internal class BuildProjectRepository(private val storage: ProjectStorage) {
    constructor(context: Context) : this(PreferenceProjectStorage(context))

    fun getProjects(): List<BuildProject> = synchronized(WRITE_LOCK) {
        val text = storage.read() ?: return@synchronized emptyList()
        val decoded = ProjectCodec.decode(text, System.currentTimeMillis())
        if (decoded.damaged) check(storage.preserveRecovery(text)) { "Could not preserve project recovery data" }
        if (decoded.migrateTimestamp && !decoded.damaged) saveProjects(decoded.projects)
        decoded.projects
    }

    fun addProject(project: BuildProject): List<BuildProject> = synchronized(WRITE_LOCK) {
        val existing = getProjects()
        require(existing.none { it.id == project.id }) { "Duplicate project identifier" }
        saveProjects(existing + project)
    }

    private fun saveProjects(projects: List<BuildProject>): List<BuildProject> {
        check(storage.write(ProjectCodec.encode(projects))) { "Could not save projects" }
        return projects
    }

    private companion object { val WRITE_LOCK = Any() }
}
