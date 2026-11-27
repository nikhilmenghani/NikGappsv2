# phase04-002 — network transport and connection handling

Activates the existing OkHttp dependency and its platform initializer, declares INTERNET and ACCESS_NETWORK_STATE, and adds bounded HTTPS metadata reads. The app follows the active validated connection, debounces temporary loss, preserves navigation behind the offline gate and offers themed Retry/network-settings actions with hidden help. Connection checks remain behind the startup logo.

The separate test APK will read the pinned public registry index and verify its SHA-256, exercise HTTP404 and cleartext rejection, and render the real offline component with controlled input. Network toggles on the wireless OnePlus connection are deliberately avoided; controlled offline rendering is not claimed as a physical disconnection test. Registry parsing/caching, authenticated delivery and package downloads are not included yet.

Verification: release lint, existing project tests and transport tests for successful reads, HTTPS enforcement, failure status and size bounds; merged manifest must match declared expectations. Device checks will cover real validated-network startup, transport outcomes, offline component actions/appearances, activity recreation and saved-project preservation, followed by test-tool removal and manual scans on OnePlus then Poco. No device pass is claimed in advance.
