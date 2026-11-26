# Phase 03.001 - Local project storage inclusion

Ports the minimal project record and existing JSON field identifiers, app-private preferences, serialized writes, timestamp migration and damaged-record recovery. Home reads its local project list. No projects or test fixtures are seeded into the app. Creation, editing, selection and account flows remain separate future checkpoints.

Release-variant JVM tests cover escaped-name/target/timestamp round trips, partially damaged records, malformed JSON, one-time timestamp migration, duplicate identifiers and failed saves. JUnit and JSON-java are test-only dependencies and absent from the release runtime classpath. Android supplies the production JSON implementation. The existing CI lint verification also runs these tests. The production compiler and signing process remain pinned.

Device checks cover empty-store reading, restart and navigation. Actual project creation and disk write/reload are exercised by the next capability; this checkpoint records storage inclusion without claiming that creation UI already exists.

Immediate installation and manual-scan observations are recorded in the [ledger](https://github.com/nikhilmenghani/NikGappsv2/blob/main/investigation/RESULTS.md).
