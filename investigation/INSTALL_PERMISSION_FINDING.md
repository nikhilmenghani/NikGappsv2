# Install permission investigation in progress

- Baseline phase05-002-browser: no install permission or PostHog. Both manual scans passed; real browser OAuth exercised only on OnePlus.
- phase19-001-permission (source f8aa8d9e722d804861ef12506ee84017e95cb266, APK SHA256 963ab28add0371ea87c65747fe2ac7097c41a01c15804dceb0c06718fa76c610): adds REQUEST_INSTALL_PACKAGES only apart from version. DEX byte-identical to baseline. No installer runtime or PostHog code.
- OnePlus accepted it and manual scan reported No harmful apps found.
- Poco normal installer blocked it at 2026-11-27T03:10:47.682263+00:00: Harmful app blocked / This app may be harmful. No bypass performed.
- This is an association, not confirmed causation. Recheck previous bytes and permission-removed/reintroduced counterparts. Device classification differs.
- Cumulative feature additions stopped. Unreleased PostHog SDK preparation saved privately and excluded from these controls.
- phase19-002-removed removes the permission again; no new SDK/runtime functionality. Version identity changes necessarily accompany immutable releases.
