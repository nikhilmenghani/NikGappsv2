# phase04-008 — image SDK inclusion only

Adds the original app's Coil 3.6.3 Compose and OkHttp integration dependencies. No image loader, registry icon lookup or downloaded-image UI is activated. This isolates dependency inclusion before the icon feature is implemented. No new permission is requested; the merged manifest remains guarded against unintended permissions and automatic startup changes.

Verification: release lint and existing 39 unit tests, resolved runtime dependency inventory, merged-manifest comparison, SDK class presence in the downloaded main APK, unchanged navigation and project persistence. Normal installation and immediate manual Play Protect scans follow OnePlus then Poco. No device pass is claimed before observation.
