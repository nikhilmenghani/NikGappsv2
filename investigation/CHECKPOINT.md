# Phase 01.003 — UI framework dependency inclusion

Adds Compose compiler 2.4.20, Compose BOM 2026.09.00, activity-compose 1.13.0, Material3 1.5.0-rc01 and navigation-compose 2.10.1 using the existing app's versions. The native splash Activity remains unchanged. This separates library inclusion and merged AndroidX components from screen activation.

No explicit network, storage, package-query, analytics or installation permissions are introduced. The release's merged manifest and dependency report record any automatically added components and language-runtime changes. Production-normalized signing; device checks pending.
