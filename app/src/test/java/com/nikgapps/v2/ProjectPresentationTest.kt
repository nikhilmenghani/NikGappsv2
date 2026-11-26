package com.nikgapps.v2

import org.junit.Assert.*
import org.junit.Test

class ProjectPresentationTest {
    @Test fun onlyUnavailableOrIncompatibleProjectsAreOutdated() {
        assertFalse(isProjectOutdated(null, null))
        assertFalse(isProjectOutdated(emptySet(), ProjectUpdate(ProjectUpdateKind.AVAILABLE, "new", "Update")))
        assertTrue(isProjectOutdated(setOf("removed"), null))
        assertTrue(isProjectOutdated(emptySet(), ProjectUpdate(ProjectUpdateKind.INCOMPATIBLE, "new", "Review")))
    }
    @Test fun initialGroupingAndLaterRecordChangesKeepStableOrder() {
        fun p(id: String) = BuildProject(id, id, AndroidVersion.ANDROID_17)
        val input = listOf(p("outdated"), p("current"))
        val order = initialProjectDisplayOrder(input, mapOf("outdated" to setOf("removed")), emptyMap())
        assertEquals(listOf("current", "outdated"), order)
        assertEquals(listOf("current", "new"), projectsInDisplayOrder(listOf(p("new"), p("current")), order).map { it.id })
    }
}
