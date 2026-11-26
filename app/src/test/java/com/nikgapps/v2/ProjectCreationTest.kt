package com.nikgapps.v2

import org.junit.Assert.*
import org.junit.Test

class ProjectCreationTest {
    private val supported = setOf(AndroidVersion.ANDROID_16, AndroidVersion.ANDROID_17)
    @Test fun rejectsBlankOversizedAndUnpublishedTargets() {
        assertThrows(IllegalArgumentException::class.java) { newProject("  ", AndroidVersion.ANDROID_16, supported, 0) }
        assertThrows(IllegalArgumentException::class.java) { newProject("n".repeat(21), AndroidVersion.ANDROID_16, supported, 0) }
        assertThrows(IllegalArgumentException::class.java) { newProject("test", AndroidVersion.ANDROID_15, supported, 0) }
    }
    @Test fun preservesChosenTargetAndCreatesArm64WithinRegularLimits() {
        val project = newProject(" test ", AndroidVersion.ANDROID_17, supported, 4)
        assertEquals("test", project.name)
        assertEquals(AndroidVersion.ANDROID_17, project.androidVersion)
        assertEquals(Architecture.ARM64, project.architecture)
        assertThrows(IllegalStateException::class.java) { newProject("sixth", AndroidVersion.ANDROID_17, supported, 5) }
    }
}
