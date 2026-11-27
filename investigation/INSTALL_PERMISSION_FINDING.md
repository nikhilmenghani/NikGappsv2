# Install permission investigation in progress

- Baseline phase05-002-browser: no install permission or PostHog. Both manual scans passed; real browser OAuth exercised only on OnePlus.
- phase19-001-permission (source f8aa8d9e722d804861ef12506ee84017e95cb266, APK SHA256 963ab28add0371ea87c65747fe2ac7097c41a01c15804dceb0c06718fa76c610): adds REQUEST_INSTALL_PACKAGES only apart from version. DEX byte-identical to baseline. No installer runtime or PostHog code.
- OnePlus accepted it and manual scan reported No harmful apps found.
- Poco normal installer blocked it at 2026-11-27T03:10:47.682263+00:00: Harmful app blocked / This app may be harmful. No bypass performed.
- This is an association, not confirmed causation. Recheck previous bytes and permission-removed/reintroduced counterparts. Device classification differs.
- Cumulative feature additions stopped. Unreleased PostHog SDK preparation saved privately and excluded from these controls.
- phase19-002-removed removes the permission again; no new SDK/runtime functionality. Version identity changes necessarily accompany immutable releases.

Repeated original permission-only APK was blocked again on Poco after baseline normal reinstall and fresh clean scan. phase19-003-reintroduced restores the same permission for a second independently built control. Permission-removed counterpart device testing is in progress; preparing this artifact does not advance the feature port.

## Reproduced v2 result

| APK | Install permission | OnePlus | Poco |
|---|---|---|---|
| phase05-002-browser, 33 | Absent | Accepted, scan clean | Previous bytes reinstalled normally, fresh scan clean |
| phase19-001-permission, 34 | Present | Accepted, scan clean | Harmful app blocked, repeated twice |
| phase19-002-removed, 35 | Absent | Accepted, scan clean | Accepted, scan clean |
| phase19-003-reintroduced, 36 | Present | Accepted, scan clean | Harmful app blocked |

All four APKs have identical DEX bytes. Only AndroidManifest.xml and META-INF/version-control-info.textproto differ in ZIP entries. No PostHog SDK, APK downloader or installer invocation was included in these permission controls. No install-source access was requested for v2; no harmful warning was bypassed.

This identifies a reproducible permission-dependent detection pattern in v2 on Poco. It does not yet prove this is the sole cause of the original app's classification. Next: matched permission removal/reintroduction in isolated original v0.82 source, preserving installed original-app projects before upgrades, then a correction in current source if confirmed. PostHog and cumulative ports remain paused while this finding is confirmed.
