# phase04-004 — catalog retrieval and parsing

Adds reviewed catalog and AppSet parsing plus checksum-verified retrieval from the fixed registry revision. This includes package identity/version/compatibility declarations and dependency references; it does not resolve selections, parse installer/removal payload fields, download packages, or add package-selection UI.

The separate instrumentation APK exercises the actual repository against the pinned public documents (75 packages, 49 AppSets). Normal-navigation integration follows the registry/selection prerequisites. Complete test fixtures live only in JVM test resources, outside the main APK.

Verification: release lint, existing tests and parser tests covering real data, unsupported schema, duplicate IDs and missing AppSet references; manifest expectations unchanged. Device checks will verify real pinned retrieval/parsing, saved local projects and navigation, test-tool removal, then manual scans on OnePlus followed by Poco. No device pass is claimed in advance.
