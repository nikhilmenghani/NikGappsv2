# Scope and source boundaries

Reference source: `959f3f4f7561a1647c8432133b3243069991c34e`, with the pending-change digest recorded separately in `source-baseline.json`. The known blocked official v0.82 comes from `43efaa19392185ce2bb023a6765f68d21ffa1772`; it is a comparison artifact, not the source of every later feature.

- Downloads currently has a destination and placeholder. It is not an implemented guest package downloader.
- Normal ZIP creation requires a GitHub account. Retired guest, unsupported-Android and root-installation flows are not active features to port.
- The current registry client uses the migrated package delivery sources. Stock-removal rules originate in `nikgapps-release/nikgapps_release/engine/build/PackageConstants.py:get_package_deletes` and pipeline overrides. Consumers use the resolved package version's `install.removeFiles`; do not reproduce those mappings in Android. Check `nikgapps-operations/MIGRATION.md` before any hosted changes.
- Recovery shell scripts and binaries are generated ZIP contents. They must receive separate static-inclusion, retrieval and assembly checkpoints. Their presence does not mean Android executes them.
- Original navigation still compiles routes for installed-app browsing and pull requests. A registered route is not proof that normal Home navigation reaches it. Reconcile those routes separately from the active feature list.
- `utils/InstallationPipeline.kt` still defines a callback-based `runInstallation` helper. Treat unused compiled helpers, demonstrations and extraction paths separately if the active-feature port remains clear.
- Original updater targets `NikGappsReleases`; every experimental v2 updater target must stay in `NikGappsv2`. Stable/dev version files and original production release triggers are not part of this investigation.
- Preserve three stable or six debug successful builds per six-hour window when porting current allowance behavior. The local counter is not authoritative server-side enforcement.
- Use a separate diagnostic PostHog project. Capture SDK inclusion before initialization and individual events; do not silently add production analytics during early phases.
- Build-time latest-version enforcement, confirmed event delivery before successful ZIP publication, authoritative remote quotas and incremental backup import are tracked enhancements, not existing parity requirements.

The original APK's permission list does not contain SMS reading/receiving, a notification-listener service or an accessibility service. Broad package visibility, file access, self-update installation, foreground/background work and battery-exemption requests remain separate later checkpoints. This inspection does not explain Google's classification.

An immediate passing v2 scan is evidence for that APK, device, installation path and time. It is not permanent clearance or a confirmed fix for the original application ID. Stop on a harmful verdict, retest the last passing control, minimize the change and perform removal/reintroduction comparisons before modifying original NikGapps.

## Dependency manifest audit

OkHttp 5.5.0 contributes INTERNET and PlatformInitializer automatically. phase04-001 records those defaults (the original prerelease wording was corrected after the merged-manifest audit); phase04-001-isolated removes both for a clean code-inclusion control. Audit merged permissions and initializers rather than inferring them from the app manifest or explicit initialization calls.
