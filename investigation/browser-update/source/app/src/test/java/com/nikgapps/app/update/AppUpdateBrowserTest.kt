package com.nikgapps.app.update

import org.junit.Assert.*
import org.junit.Test

class AppUpdateBrowserTest {
    @Test fun preservesLatestStableAsset() {
        val url = "https://github.com/nikhilmenghani/NikGappsReleases/releases/download/v0.82/NikGapps-v0.82.apk"
        assertEquals(url, AppUpdateBrowser.trustedDownloadUrl("0.82", url, false))
    }
    @Test fun developmentChannelStaysOnDevelopmentArtifact() {
        val url = "https://github.com/nikhilmenghani/NikGappsReleases/releases/download/dev-v0.80.22/NikGapps-debug-dev-v0.80.22.apk"
        assertEquals(url, AppUpdateBrowser.trustedDownloadUrl("0.80.22", url, true))
    }
    @Test fun rejectsUntrustedAndCredentialBearingLinks() {
        val expected = "https://github.com/nikhilmenghani/NikGappsReleases/releases/download/v0.82/NikGapps-v0.82.apk"
        for (url in listOf("http://github.com/x.apk", "https://github.com.evil.test/x.apk", "https://token@github.com/x.apk",
            expected + "?token=secret", expected + "#secret", expected.replace("v0.82/", "v0.81/"), expected.replace("NikGappsReleases", "NikGapps"))) {
            assertEquals(expected, AppUpdateBrowser.trustedDownloadUrl("0.82", url, false))
        }
    }
    @Test fun rejectsMalformedVersionsBeforeOpeningAnything() {
        for (version in listOf("Unknown", "../v0.82", "0.82?token=secret", "", "0.82-debug")) {
            assertFalse(AppUpdateBrowser.validVersion(version))
            assertThrows(IllegalArgumentException::class.java) { AppUpdateBrowser.trustedDownloadUrl(version, "", false) }
        }
    }
}
