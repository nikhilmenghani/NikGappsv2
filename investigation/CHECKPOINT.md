# phase04-009 — verified registry app icons and standard fallback

Activates the previously isolated Coil library for app icons. Icon metadata and asset checksums are pinned to the same registry revision. Downloads use authenticated repository API requests; credentials are removed on cross-host redirects. Installed-app discovery remains excluded until its own checkpoint.

Optional images use private caching with SHA-256, byte-size and image-dimension checks. Missing metadata, authentication/network errors, invalid images and corrupt cached files fall back to the theme-tinted standard Android icon. Errors cannot block package metadata. The displayed image comes from a verified local file rather than a separate unscoped image downloader.

Verification: release lint and 42 JVM tests, including the real 50-entry manifest and invalid host/port/repository/query/hash/size/schema cases. Separate instrumentation exercises real pinned Chrome/Google Play services downloads, offline cache reuse, corrupt-cache rejection and missing-entry fallback; then renders the real icon component in light/dark/classic surfaces. Task caches and test tooling are removed before immediate manual scans, OnePlus then Poco. Normal package-list integration follows phase07.
