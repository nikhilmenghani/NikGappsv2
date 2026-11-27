# phase04-006 — release snapshots and dependency resolution

Adds pinned release-index/manifest parsing, identity and version-reference validation, immutable-document caching and Android/channel/architecture selection. Dependency traversal handles conditional dependencies, missing nodes/channels, cycles, deterministic ordering and deduplication across AppSets. Explicit selections stay visible even when also reached as dependencies. Mutually exclusive UI selection rules follow in the package-selection phase.

All eight manifest paths/checksums and the registry commit remain fixed. Installer/config-template/payload parsing is not included. Repository components are exercised through the separate test APK; normal-navigation integration follows its prerequisites.

Verification: release lint, existing tests and resolver tests using real latest Android16/17 Core snapshots plus synthetic diamond, cycle, missing-node and conditional graphs. Immediate device checks and manual scans follow OnePlus then Poco. No device pass is claimed in advance.
