package com.nikgapps.v2

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CatalogParserTest {
    private fun fixture(name: String) = requireNotNull(javaClass.getResource("/registry/$name")).readText()
    @Test fun parsesPinnedRegistryAndValidatesReferences() {
        val snapshot = validateCatalog(CatalogParser.parseCatalog(fixture("catalog.json")), CatalogParser.parseAppSets(fixture("appsets.json")))
        assertEquals(75, snapshot.catalog.packages.size)
        assertEquals(49, snapshot.appSets.appSets.size)
        assertTrue(snapshot.catalog.packages.any { it.selectable && !it.internal })
    }
    @Test fun unsupportedSchemaIsRejected() {
        val root = Json.parseToJsonElement(fixture("catalog.json")).jsonObject
        val changed = JsonObject(root + ("schemaVersion" to JsonPrimitive(2)))
        assertThrows(MetadataException::class.java) { CatalogParser.parseCatalog(changed.toString()) }
    }
    @Test fun duplicatePackageIdsAreRejected() {
        val root = Json.parseToJsonElement(fixture("catalog.json")).jsonObject
        val item = root.getValue("packages").jsonArray.first()
        val changed = JsonObject(root + ("packages" to JsonArray(listOf(item, item))))
        assertThrows(MetadataException::class.java) { CatalogParser.parseCatalog(changed.toString()) }
    }
    @Test fun appSetCannotReferToMissingPackage() {
        val catalog = CatalogParser.parseCatalog(fixture("catalog.json"))
        val sets = CatalogParser.parseAppSets(fixture("appsets.json"))
        val changed = sets.copy(appSets = listOf(sets.appSets.first().copy(packages = listOf("missing-package"))))
        assertThrows(IllegalArgumentException::class.java) { validateCatalog(catalog, changed) }
    }
}
