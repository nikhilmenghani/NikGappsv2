# phase04-001-isolated — stripped OkHttp inclusion control

Keeps OkHttp 5.5.0 code/dependencies, while explicitly removing its library-contributed INTERNET permission and PlatformInitializer. No application request code is included. The preceding phase04-001 captured the library defaults and passed immediate checks on both devices; its release notes include the merged-manifest correction.

Adds a pre-publication guard comparing actual merged permissions and startup initializers with a reviewed expectation file. Published APK bytes and source tags from earlier checkpoints remain unchanged.

Verification: release lint, 11 JVM tests and merged-manifest comparison. Device checks will cover normal installation and saved-project/navigation behavior, with a fresh v2 installation on Poco to separate that path from OnePlus upgrades. Only task-created v2 fixtures are reset; original NikGapps is untouched. Immediate manual scans follow on OnePlus, then Poco. No device pass is claimed in advance.
