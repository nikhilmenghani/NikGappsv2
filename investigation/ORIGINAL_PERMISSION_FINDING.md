# Original NikGapps confirmation

Source: original v0.82 commit 43efaa19392185ce2bb023a6765f68d21ffa1772. Same application ID com.nikgapps, version 0.82/code82, non-debuggable, production certificate and normalization.

| Original APK | OnePlus | Poco |
|---|---|---|
| Official / permission-present rebuild | Harmful app blocked on repeat | Not repeated in this original control |
| Manifest-only permission removal | Normal install + manual scan clean | Normal install + manual scan clean |
| Permission removal + startup entry cleanup | Normal install, startup stable, manual scan clean | Normal install, startup stable, manual scan clean |

The permission-present rebuild is byte-identical to official v0.82 (SHA256 ce0f62992d4ca722c73e503b40e7fe085bdbe19d3552ebcf0db916a2259b310e). Manifest-only removal (a1644ff4ac2da85400bb55476e9fda17a9e28785bd233c07e942d247b448e526) differs in AndroidManifest.xml alone: all DEX/resources/other ZIP entries are identical. PostHog remains included and initialized as in the original app.

Manifest-only removal exposed a startup SecurityException from the old canRequestPackageInstalls call. The usable diagnostic (d9e7976d92d078e4fa912d8a9dc46a95a194757318f984a7d4a0439f777c7950) also removes INSTALL_APPS from the startup permission map. Both phones now have this usable original-app diagnostic. No GitHub setup or additional storage grants were required for these install/startup checks. OnePlus project data was privately backed up and verified identical after upgrades. Poco debug app was untouched. Files by Google temporary install access was restored OFF; no harmful warning was bypassed.

The two-file removal/startup cleanup has also been applied, uncommitted, to current original NikGapps source. Current source unit tests and release lint passed; its four pre-existing dirty files are unchanged. Device tests used the isolated original v0.82 source, not a new APK from current HEAD.

Before a stable release, direct in-app APK installation and remaining updater permission checks need a browser/file-manager handoff. These diagnostics are prereleases only in the investigation repository; normal release channels were not changed. Scans are immediate observations at the recorded times.

See original-permission-evidence.json for timestamps, hashes and device results. Private project backups and credentials are not published.
