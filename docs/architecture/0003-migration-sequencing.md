# ADR 0003: Sequence migrations around data preservation

- **Status:** Accepted
- **Date:** 2026-08-30
- **Implemented:** stages 1 to 7 (PRs #1 to #35). Stage 8, cutting reads over and retiring the legacy tables, remains. There is no feature flag (ADR 0004 decision 1). The Context section describes the code as of 2026-08-30.

## Context

The production database registers migrations from 9 through 21 but also enables `fallbackToDestructiveMigration()` (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt:33-42`). Room schema export is disabled (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt:11-15`), limiting reproducible historical migration fixtures. Existing instrumentation coverage concentrates on 10-to-11 and includes an `allMigrations_fromVersion10` test that only registers `MIGRATION_10_11` (`app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/MigrationTest.kt:14-17,216-238`).

Legacy training records contain separate `date` (`YYYY-MM-DD`) and `time` (`HH:mm`) strings (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/TrainingRecord.kt:20-32`). They identify sets but not session boundaries. Existing JSON import is described as a complete overwrite (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/viewmodel/TrainingViewModel.kt:907-910`), while the UI already offers a user-selected backup-before-import flow (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/BackupScreen.kt:78-115`). The overhaul must strengthen these paths before v2 data becomes authoritative.

## Decision

Migrate in guarded, reversible stages:

1. **Fail closed immediately.** Remove `fallbackToDestructiveMigration()` in Phase 0, before adding v2 schema work. A database with a missing or unsupported migration must refuse to open and preserve its bytes rather than erase user data. The supported-version matrix and recovery UX remain later release gates; neither is a prerequisite for stopping silent erasure.
2. **Establish fixtures and observability.** Enable Room schema export, commit schemas for every newly supported version, build representative databases/backups from supported legacy versions, and record row counts, foreign-key violations, parse failures, duplicate candidates, orphan links, and unknown enum/string values.
3. **Make only additive production schema changes initially.** Add v2 tables, indexes, nullable linkage/provenance columns, and converters without dropping or repurposing legacy tables or columns. New migrations are idempotent where Room permits and are tested from each declared supported starting version.
4. **Retain legacy readers and columns during transition.** Before enabling v2 writes, route fallback/legacy history UI through a compatibility reader that returns both retained legacy sessions and v2-created sessions, and test that disabling the feature flag cannot hide v2-only workouts. Existing history, export, and recovery readers remain available until v2 backfill, dual-read comparison, export/import, and rollback gates pass. This read compatibility does not require dual writes; avoid long-lived dual writes, and if a short compatibility window requires them, define one transaction owner and assert both representations reconcile.
5. **Backfill conservatively and report anomalies.** Conversion produces a structured, user-safe migration/import report with counts and reasons. Unparseable dates/times, missing exercises, conflicting group names/IDs, invalid values, and ambiguous session grouping are retained or quarantined for legacy reading rather than silently dropped or coerced.
6. **Never fabricate session precision.** Parse valid legacy local date/minute values under an explicitly documented device-zone policy, but mark them as legacy-derived. Do not invent seconds, set completion times, durations, or exact gaps. Group records into a migrated session only when a deterministic rule is documented and tested; otherwise use conservative per-record/per-unambiguous-batch sessions or leave the records on the legacy path. A synthetic timestamp must never be presented as observed fact.
7. **Protect every destructive import or one-way upgrade.** Before complete-overwrite import, require a successful, readable pre-import backup and provide a cancel path. Before a migration release, document APK/database rollback limits and provide an export/backup path from the prior version. Import validation and staging occur before deleting current rows; restore is transactional or recoverable from the backup.
8. **Cut reads over before cleanup.** Switch one bounded use case at a time to v2 reads, compare counts and representative values, then stop any compatibility writes. Legacy columns/readers are removed only in a later migration after at least one released compatibility window and explicit acceptance evidence.

Each stage has a separately reviewable commit and can ship without requiring the next stage.

## Consequences

- The database and read layer are temporarily larger and more complex.
- Migration reports make imperfect legacy data visible instead of converting it into plausible but false history.
- Schema fixtures and path matrices add maintenance work but turn data survival into a testable release gate.
- Users retain a legacy recovery path while v2 behavior is proven.
- Destructive fallback is removed immediately so migration gaps fail closed; comprehensive migration matrices and recovery UX are still required before release.
- Cleanup is intentionally delayed; schedule pressure does not justify dropping legacy data, bypassing backup, or inventing timestamps.
