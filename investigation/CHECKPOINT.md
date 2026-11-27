# phase04-005 — verified cache and compatibility

Adds a 30-minute app-private cache for the complete pinned catalog/AppSet pair, checksum/schema/reference validation, interrupted-write recovery and last-successful fallback after refresh failure. Damaged or mismatched data is rejected. API bounds, declared Android versions, architecture and device-type restrictions are checked without resolving package selections yet.

The separate test APK exercises actual network population, fresh-cache reuse, controlled transport failure, corrupted-cache rejection and compatibility in a task-owned private cache directory, then removes its fixtures. No new production permissions/dependencies. Normal-navigation integration follows selection prerequisites.

Verification: release lint, prior tests and cache/compatibility tests, unchanged merged-manifest expectations. Immediate device checks and manual scans follow on OnePlus then Poco; no device pass is claimed in advance.
