package com.nikgapps.v2

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PackageIconManifestTest {
    private fun fixture() = requireNotNull(javaClass.getResource("/registry/icons.json")).readText()
    @Test fun pinnedManifestParsesKnownChromeAndGmsAssets() {
        val text = fixture(); assertEquals(RegistryPin.ICONS_SHA256, iconDigest(text.toByteArray()))
        val assets = parseIconAssets(text)
        assertEquals(50, assets.size); assertEquals(10871L, assets.getValue("com.android.chrome").size)
        assertEquals(7253L, assets.getValue("com.google.android.gms").size)
        assertNull(assets["com.nikgapps.missing.fixture"])
    }
    @Test fun unsafeDestinationsAndInvalidEntriesAreOmitted() {
        val base = Json.parseToJsonElement(fixture()).jsonObject.getValue("icons").jsonObject.getValue("com.android.chrome").jsonObject
        val malformed = listOf("https://evil.test/icon", "https://api.github.com.evil.test/repos/nikgapps/nikgapps-packages/releases/assets/1", "https://api.github.com:444/repos/nikgapps/nikgapps-packages/releases/assets/1", "https://api.github.com/repos/other/repo/releases/assets/1", "https://api.github.com/repos/nikgapps/nikgapps-packages/releases/assets/1?token=ignored")
        for (url in malformed) {
            val text = buildJsonObject { put("schemaVersion",1); putJsonObject("icons") { put("com.example", JsonObject(base + ("url" to JsonPrimitive(url)))) } }.toString()
            assertTrue(parseIconAssets(text).isEmpty())
        }
        for ((key, value) in listOf("sha256" to JsonPrimitive("broken"), "size" to JsonPrimitive(0), "size" to JsonPrimitive(MAX_ICON_BYTES + 1))) {
            val text = buildJsonObject { put("schemaVersion",1); putJsonObject("icons") { put("com.example", JsonObject(base + (key to value))) } }.toString()
            assertTrue(parseIconAssets(text).isEmpty())
        }
    }
    @Test fun unsupportedSchemaCannotBecomeCachedMetadata() {
        assertThrows(IllegalArgumentException::class.java) { parseIconAssets("""{"schemaVersion":2,"icons":{}}""") }
    }
}
