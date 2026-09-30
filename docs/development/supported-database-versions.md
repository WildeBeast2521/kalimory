# Supported database versions

## Decision

The current Room database version is **24**. The supported installed Room database source versions are exactly **9 through 24**, inclusive.

`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt` declares version 24 and registers the contiguous path (`AppDatabase.ALL_MIGRATIONS`):

`MIGRATION_9_10` → `MIGRATION_10_11` → `MIGRATION_11_12` → `MIGRATION_12_13` → `MIGRATION_13_14` → `MIGRATION_14_15` → `MIGRATION_15_16` → `MIGRATION_16_17` → `MIGRATION_17_18` → `MIGRATION_18_19` → `MIGRATION_19_20` → `MIGRATION_20_21` → `MIGRATION_21_22` → `MIGRATION_22_23` → `MIGRATION_23_24`.

Version 22 only adds the v2 workout tables (`workout_sessions`, `session_exercises`, `set_entries`; ADR 0002). Every table from version 21 is unchanged, as the committed schemas `21.json` and `22.json` show. Version 23 only replaces the unique index on `set_entries.legacyTrainingRecordId` with one on `(legacyTrainingRecordId, side)`. One legacy unilateral record holds both sides, so it converts to one entry per side. Version 24 only adds four nullable columns to `workout_sessions` (`intervalWorkSeconds`, `intervalRestSeconds`, `intervalRounds`, `intervalRoundRestSeconds`) for the settings of interval workouts.

The following installed databases are unsupported and must fail closed:

- versions 1–8;
- any version newer than the current version, for example after an older build is installed over a newer database;
- any future source version whose registered migration path to the current version has a gap.

Neither destructive migration fallback nor destructive downgrade fallback is permitted.

`AppDatabase.build` enforces this before Room opens the file: `InstalledDatabaseVersion` reads `user_version` from the SQLite header and throws `UnsupportedDatabaseVersionException` for a version outside 9–24, leaving the database and its journal files byte-identical. Without this check, Room's switch to WAL mode rewrites header bytes of a rollback-journal database before the missing migration is detected. When a non-empty `-wal` or `-journal` file exists, the header alone is not authoritative; the check then defers to Room, which still fails closed without migrating, although SQLite may apply the pending journal to the main file.

This policy concerns the installed Room database schema version only. It is separate from the JSON backup format and its versions 1–10; a JSON backup version does not establish support for the correspondingly numbered Room database version.

## Corrupt databases

SQLite's default corruption handling deletes the database file. `AppDatabase.build` replaces it with `CorruptionPreservingOpenHelperFactory`: on a corruption report, the database and its `-wal`, `-shm`, and `-journal` files are copied once into `<name>.corrupt` next to the database, and the live file is kept. The error still reaches the caller. The first copy is never overwritten. At start-up, `DatabaseStartupCheck` opens the database before any screen uses it. When the database is unsupported, fails to open, or has an unacknowledged corruption copy, `DatabaseUnavailableScreen` explains what happened. It lets the user export the database, its sidecar files, the corruption copy and any files an earlier restore replaced, as a zip through the Storage Access Framework.

Such a zip can be restored in the app, from that screen or from Settings › Complete Backup (`RecoveryRestore`).
- Only the database and its sidecar files are taken from the zip, into a staging folder.
- The staged copy must pass `PRAGMA integrity_check` and carry a version from 9 to the current one. Its write-ahead log is folded into the main file.
- The current files are then moved into `<name>.before-restore-<time>/`, never deleted, and the staged file takes their place. If a move fails, it is rolled back.
- The app restarts, and Room migrates an older version as on any open.

## Rationale

Support begins at version 9 because it is the earliest source in the contiguous migration chain registered by `AppDatabase`. The audited schema-version provenance is:

| Room version | Introducing commit |
|---:|:---|
| 9 | `d60b3658` |
| 10 | `435bdce8` |
| 11 | `398769b4` |
| 12 | `47173b86` |
| 13 | `c0f434bd` |
| 14 | `c0ec49db` |
| 15 | `ae18ad88` |
| 16 | `71ec3947` |
| 17 | `c65da6a1` |
| 18 | `7ce99eeb` |
| 19 | `6b37e094` |
| 20 | `e6661266` |
| 21 | `eeaa5ae4` |
| 22 | `bd8f3e9`: additive v2 workout tables |
| 23 | per-side unique legacy link on `set_entries` |
| 24 | PR #34 (current): interval settings on `workout_sessions` |

## Required evidence

This declaration owns the migration fixture and matrix coverage for every supported source version 9–24 through the current version 24. Coverage must use representative historical database fixtures and validate each complete registered path, not merely individual migration constants.

The table above records provenance, not a claim that migration tests currently pass.

## Release history

Every release up to and including v1.26.0 called `fallbackToDestructiveMigration()`; commit `ee97ae5` removed it, and no release tag contains that commit yet. A historical build therefore erases the database whenever its own Room version differs from the installed database version and it has no registered migration path from that version.

| Release tags | Room version | Registered migrations |
|:---|---:|:---|
| v1.0.0–v1.1.0 | 8 | none |
| v1.2.0–v1.6.0 | 9 | none |
| v1.7.0–v1.8.1.1 | 10 | 9→10 |
| v1.9.0–v1.10.0 | 11 | 9→10 through 10→11 |
| v1.11.0–v1.12.0 | 12 | 9→10 through 11→12 |
| v1.13.0–v1.14.1 | 13 | 9→10 through 12→13 |
| beta-1.15.0, v1.15.0 | 14 | 9→10 through 13→14 |
| v1.16.0–v1.16.1 | 15 | 9→10 through 14→15 |
| v1.17.0–v1.17.1 | 16 | 9→10 through 15→16 |
| v1.18.0 | 20 | 9→10 through 19→20 |
| v1.19.0–v1.26.0 | 21 | 9→10 through 20→21 |

Versions 1–7 and 17–19 were never the version of a tagged release; they exist only in development builds. Versions 17–19 remain supported because the registered path covers them.

No tagged release can migrate a version 1–8 database. The only builds that open a version 8 database without erasing it are v1.0.0 and v1.1.0, and installing either over a newer app is a downgrade (see step 4 below). In practice, recovery of a version 1–8 database ends at step 6.

## Unsupported recovery

When an installed database is unsupported:

1. Do not uninstall the app, clear its storage, or overwrite its database bytes. Uninstalling or clearing storage erases the database.
2. Preserve or copy the app data when technically possible.
3. Do not assume that any historical build is safe (see "Release history"). A historical build is a recovery candidate only when its declared Room version equals the installed database version, or when it registers a contiguous migration path from that version to its own.
4. Android normally refuses to install an older `versionCode` over a newer one without uninstalling first, which erases the data. Do not uninstall to make a downgrade possible.
5. Android also refuses an update signed with a different key. Builds from different distribution channels (for example F-Droid and GitHub releases) may be signed with different keys. Do not uninstall to switch channels. Use a candidate from step 3 only when it installs over the current app without data loss. Export a JSON backup from it, then import that backup through the current app's validation path.
6. If safe recovery cannot be performed, stop and seek manual assistance.

## Change control

Changing the supported range, current-version declaration, provenance, migration path, or required fixture/matrix coverage requires explicit evidence and review. A schema bump must update this policy and provide a contiguous registered migration path; any gap remains unsupported and must fail closed.
