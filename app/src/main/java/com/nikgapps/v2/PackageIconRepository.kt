package com.nikgapps.v2

import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

internal data class PackageIconAsset(val url: String, val sha256: String, val size: Long)
internal const val MAX_ICON_BYTES = 512 * 1024
internal fun parseIconAssets(text: String): Map<String, PackageIconAsset> {
    val root = Json.parseToJsonElement(text).jsonObject
    require(root.getValue("schemaVersion").jsonPrimitive.int == 1)
    return root.getValue("icons").jsonObject.mapNotNull { (name, raw) ->
        try {
            val entry = raw.jsonObject
            val asset = PackageIconAsset(entry.getValue("url").jsonPrimitive.content,
                entry.getValue("sha256").jsonPrimitive.content, entry.getValue("size").jsonPrimitive.long)
            require(name.matches(Regex("[a-zA-Z0-9_.]+")))
            require(asset.sha256.matches(Regex("[a-f0-9]{64}")) && asset.size in 1..MAX_ICON_BYTES.toLong())
            require(asset.url.matches(Regex("https://api\\.github\\.com/repos/nikgapps/nikgapps-packages/releases/assets/[0-9]+")))
            name to asset
        } catch (_: Exception) { null }
    }.toMap()
}
internal fun iconDigest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Optional images never block package metadata. Cached files are verified before use. */
internal class RegistryIconRepository(private val directory: File,
    private val http: RegistryHttp = RegistryHttp(maxAttempts = 1), private val now: () -> Long = System::currentTimeMillis) {
    private val mutex = locks.computeIfAbsent(directory.canonicalPath) { Mutex() }
    private var assets: Map<String, PackageIconAsset>? = null
    private var lastAttempt: Long? = null
    suspend fun icon(packageName: String): File? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val elapsed = lastAttempt?.let { now() - it }
                if (elapsed == null || elapsed < 0 || elapsed >= 30 * 60 * 1000L) {
                    lastAttempt = now()
                    assets = readManifest() ?: assets
                }
                val asset = assets?.get(packageName) ?: return@withLock null
                val file = File(directory, "${asset.sha256}.img")
                if (validImage(file, asset)) return@withLock file
                val bytes = http.get(asset.url, MAX_ICON_BYTES)
                if (!validImage(bytes, asset)) return@withLock null
                publish(file, bytes)
                file
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }
    }
    private fun readManifest(): Map<String, PackageIconAsset>? {
        val manifest = File(directory, "icons-${RegistryPin.COMMIT}.json")
        if (manifest.isFile && manifest.length() in 1..1024L * 1024) {
            val bytes = manifest.readBytes()
            if (iconDigest(bytes) == RegistryPin.ICONS_SHA256) return parseIconAssets(bytes.toString(Charsets.UTF_8))
        }
        return try {
            val bytes = http.get(RegistryDelivery.contents("icons.json"), 1024 * 1024)
            check(iconDigest(bytes) == RegistryPin.ICONS_SHA256)
            val parsed = parseIconAssets(bytes.toString(Charsets.UTF_8))
            publish(manifest, bytes); parsed
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }
    private fun validImage(file: File, asset: PackageIconAsset) = file.isFile && file.length() == asset.size && validImage(file.readBytes(), asset)
    private fun validImage(bytes: ByteArray, asset: PackageIconAsset): Boolean {
        if (bytes.size.toLong() != asset.size || iconDigest(bytes) != asset.sha256) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return bounds.outWidth in 1..1024 && bounds.outHeight in 1..1024
    }
    private fun publish(target: File, bytes: ByteArray) {
        check(directory.isDirectory || directory.mkdirs())
        val stage = File(directory, target.name + ".tmp")
        try {
            stage.outputStream().use { it.write(bytes); it.fd.sync() }
            // Existing valid immutable files are returned above; only damaged targets need replacement.
            check(!target.exists() || target.delete())
            check(stage.renameTo(target))
        } finally { stage.delete() }
    }
    companion object { private val locks = ConcurrentHashMap<String, Mutex>() }
}
