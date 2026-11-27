# Feature port checklist

Every checkpoint receives its own immutable release. Under the revised investigation, related capabilities may be grouped, then split if a warning appears. Stop at the first harmful warning, retest controls, isolate the delta and validate the matching correction in the original package.

- [x] 00 Empty framework app; ordinary/normalized signing controls
- [x] 01 Branding, splash, navigation, shared UI components
- [x] 02 Themes, dynamic colors, glass, backgrounds, accordions
- [x] 03 Persistence, projects, menus, outdated deck and gestures (component inputs; registry integration follows)
- [x] 04 Network client, pinned registry, caching, dependencies, authenticated delivery, icons (components; package-list integration follows phase07)
- [ ] 05 PAT, OAuth, sessions, required sign-in, avatar, token help
- [ ] 06 Elite membership, badge, project/name limits, capability checks
- [ ] 07 AppSets, selection/dependencies/exclusions, search, sort/filter, Review Build
- [ ] 08 Package identity, icons, partition and size information
- [ ] 09 Config import/export, validation, 0/1/2 and installer preferences
- [ ] 10 Versioned backups, replacement restore, Elite choices, outdated import
- [ ] 11 Targeted app checks, broad visibility permission, installed-app browsing
- [ ] 12 Staging, Downloads, legacy/all-files storage, FileProvider
- [ ] 13 Fixture ZIPs, builder assets, scripts/binaries, guard, artifacts, real assembly
- [ ] 14 Metadata, credits, compression, dates, storage, atomic output, duplicates, reopen
- [ ] 15 Stock removals, confirmations, debloater validation/config/instructions
- [ ] 16 WorkManager, foreground build, persistence, progress and notifications
- [ ] 17 Allowances, idempotence, exhaustion, Elite reset, persistence
- [ ] 18 Release cards, project updates, dated notes/YAML, animation, app changelog
- [ ] 19 Release lookup/cache/gate, scheduling, install permission, APK validation/install
- [ ] 20 Android ID, Virtual MAC, PostHog inclusion/init/events/identity (diagnostic project)
- [ ] 21 Logcat/export, biometrics, battery, permission gate, developer options, help
- [ ] 22 Original backups, regular/Elite parity, later fixes, replacement review

Downloads remains a placeholder. Retired guest/root/unsupported-Android paths stay outside the active port. Build-time update enforcement, acknowledged analytics delivery, remote quotas and incremental restores remain proposed enhancements.

## Observation fields

Release, source SHA, feature delta, APK SHA-256, certificate, merged manifest, dependency list, device/OS/Play versions, installer source, fresh/upgrade path, UTC time, feature result, manual scan result, exact warning and screenshot references. Immediate passes are provisional.

## Revised priority

User requested updater and analytics experiments before the remaining parity port. Permission declaration and SDK inclusion remain isolated controls. Runtime prerequisites may be grouped (up to four related capabilities). A warning implicates the tested combination, not a proven individual cause; use removal/reintroduction and matching previous controls before claiming causation.

1. phase19-001-permission: REQUEST_INSTALL_PACKAGES declaration only; no request/grant, updater code, APK download, FileProvider or installer invocation.
2. Updater activation: release lookup, APK download, validation, installer handoff. Test inclusion separately from install-source grant/runtime action. V2 targets only.
3. PostHog 3.70.0 SDK inclusion without initialization.
4. PostHog initialization and representative logging against a separate diagnostic project, matching original capture settings.

Deferred cumulative GitHub checkpoint: OnePlus browser authorization and scan passed; Poco installation and manual scan passed, but browser authorization was not exercised after an ADB server interruption. Do not count this as full two-device OAuth completion.
