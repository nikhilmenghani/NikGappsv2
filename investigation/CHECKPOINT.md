# phase04-001 — network dependency inclusion

Adds only OkHttp 5.5.0, matching the original app dependency. No requests are made and no INTERNET permission is declared in this checkpoint. Activation and connection handling follow in a separate release.

Verification: release lint and 11 JVM tests, resolved release dependency graph and merged manifest. Device checks will verify unchanged local project/navigation behavior and that no network permission has been introduced, followed by manual Play Protect scans on OnePlus then Poco. No device pass is claimed in advance.
