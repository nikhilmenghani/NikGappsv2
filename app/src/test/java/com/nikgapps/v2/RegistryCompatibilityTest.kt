package com.nikgapps.v2

import org.junit.Assert.*
import org.junit.Test

class RegistryCompatibilityTest {
    private val version = PackageVersion("v", 1, "example", AndroidCompatibility(34, 35, 37), listOf("arm64-v8a"), listOf("17"), emptyList(), listOf("phone"))
    @Test fun apiBoundsAreInclusiveAndTargetIsNotAnUpperBound() {
        validateCompatibility("p", version, 34, "arm64-v8a")
        validateCompatibility("p", version, 37, "arm64-v8a", androidVersion = "17")
        assertThrows(RegistryException.UnsupportedApi::class.java) { validateCompatibility("p", version, 33, "arm64-v8a") }
        assertThrows(RegistryException.UnsupportedApi::class.java) { validateCompatibility("p", version, 38, "arm64-v8a") }
    }
    @Test fun releaseArchitectureAndDeviceRestrictionsAreEnforced() {
        assertThrows(RegistryException.UnsupportedApi::class.java) { validateCompatibility("p", version, 37, "arm64-v8a", androidVersion = "16") }
        assertThrows(RegistryException.UnsupportedArchitecture::class.java) { validateCompatibility("p", version, 37, "x86_64") }
        assertThrows(RegistryException.UnsupportedDeviceType::class.java) { validateCompatibility("p", version, 37, "arm64-v8a", "tv") }
        validateCompatibility("p", version.copy(architectures = emptyList(), deviceTypes = emptyList()), 37, "x86_64", "tv")
    }
}
