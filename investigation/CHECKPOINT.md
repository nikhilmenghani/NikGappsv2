# phase03-005 — outdated-project component

Adds the original outdated-project predicate, stable display ordering, a collapsed-by-default outdated group and read-only project cards. Outdated projects expose Copy/Delete, with Edit omitted. Tapping an outdated card reveals its information.

Registry integration and package selection follow in later checkpoints. This release tests classification and ordering with JVM tests and renders the real component using in-memory inputs from a separate, temporary Android instrumentation APK. Fixtures are not in the main APK, do not modify saved projects, and are not public release attachments. Stack/gesture/bounce transitions follow separately.

Verification: release lint and 11 JVM tests; main APK is non-debuggable and has no instrumentation. Device results will be recorded after normal installation of the downloaded release APK, component checks, removal of the temporary test APK, and immediate manual Play Protect scans on OnePlus then Poco. No device pass is claimed in advance.
