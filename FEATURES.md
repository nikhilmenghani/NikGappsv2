# Feature port checklist

Every capability receives its own immutable release. Stop at the first harmful warning, retest controls, isolate the delta and validate the matching correction in the original package.

- [x] 00 Empty framework app; ordinary/normalized signing controls
- [ ] 01 Branding, splash, navigation, shared UI components
- [ ] 02 Themes, dynamic colors, glass, backgrounds, accordions
- [ ] 03 Persistence, projects, menus, outdated deck and gestures
- [ ] 04 Network client, pinned registry, caching, dependencies, authenticated delivery, icons
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
