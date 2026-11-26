package com.nikgapps.v2

import org.junit.Assert.*
import org.junit.Test

class BuildProjectRepositoryTest {
    private class MemoryStorage(var text: String? = null) : ProjectStorage {
        var recovery: String? = null
        var writable = true
        override fun read() = text
        override fun write(text: String): Boolean { if (!writable) return false; this.text = text; return true }
        override fun preserveRecovery(text: String): Boolean { recovery = text; return true }
    }
    private fun project(id: String = "one") = BuildProject(id, "NiK \u4F60\u597D \"test\"", AndroidVersion.ANDROID_12L,
        Architecture.ARM64, 123456789L)

    @Test fun roundTripPreservesEscapedNamesTargetAndTimestamp() {
        val storage = MemoryStorage()
        BuildProjectRepository(storage).addProject(project())
        assertEquals(listOf(project()), BuildProjectRepository(storage).getProjects())
    }
    @Test fun damagedRecordsDoNotDiscardValidProjectsAndOriginalDataIsPreserved() {
        val valid = ProjectCodec.encode(listOf(project())).removeSuffix("]")
        val original = valid + ",{" + "\"id\":\"bad\",\"androidVersion\":\"future\"}]"
        val storage = MemoryStorage(original)
        assertEquals(listOf(project()), BuildProjectRepository(storage).getProjects())
        assertEquals(original, storage.recovery)
        assertEquals(original, storage.text)
    }
    @Test fun unreadableJsonIsKeptForRecovery() {
        val storage = MemoryStorage("{broken")
        assertTrue(BuildProjectRepository(storage).getProjects().isEmpty())
        assertEquals("{broken", storage.recovery)
    }
    @Test fun oldTimestampIsMigratedOnceAndRemainsStable() {
        val array = org.json.JSONArray(ProjectCodec.encode(listOf(project())))
        array.getJSONObject(0).remove("createdAt")
        val storage = MemoryStorage(array.toString())
        val first = BuildProjectRepository(storage).getProjects()
        assertEquals(first, BuildProjectRepository(storage).getProjects())
        assertTrue(first.single().createdAt > 0)
        assertTrue(org.json.JSONArray(storage.text).getJSONObject(0).has("createdAt"))
    }
    @Test fun duplicateIdAndWriteFailureAreReportedWithoutSuccess() {
        val storage = MemoryStorage()
        val repository = BuildProjectRepository(storage)
        repository.addProject(project())
        assertThrows(IllegalArgumentException::class.java) { repository.addProject(project()) }
        storage.writable = false
        assertThrows(IllegalStateException::class.java) { repository.addProject(project("two")) }
        assertEquals(listOf(project()), repository.getProjects())
    }
}
