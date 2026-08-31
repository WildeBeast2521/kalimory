# Supported database versions

## Decision

The current Room database version is **21**. The supported installed Room database source versions are exactly **9 through 21**, inclusive.

`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt` declares version 21 and registers the contiguous path:

`MIGRATION_9_10` → `MIGRATION_10_11` → `MIGRATION_11_12` → `MIGRATION_12_13` → `MIGRATION_13_14` → `MIGRATION_14_15` → `MIGRATION_15_16` → `MIGRATION_16_17` → `MIGRATION_17_18` → `MIGRATION_18_19` → `MIGRATION_19_20` → `MIGRATION_20_21`.

Installed Room databases at versions 1–8 are unsupported. Any future source version for which the registered migration path to the current version has a gap is also unsupported. Unsupported databases must fail closed; destructive fallback is not permitted.

This policy concerns the installed Room database schema version only. It is separate from the JSON backup format and its versions v1–v8; a JSON backup version does not establish support for the correspondingly numbered Room database version.

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
| 21 | `eeaa5ae4` (current) |

## Required evidence

This declaration owns the migration fixture and matrix coverage for every supported source version 9–21 through the current version 21. Coverage must use representative historical database fixtures and validate each complete registered path, not merely individual migration constants.

The table above records provenance, not a claim that migration tests currently pass.

## Unsupported recovery

When an installed database is unsupported:

1. Do not uninstall the app, clear its storage, or overwrite its database bytes.
2. Preserve or copy the app data when technically possible.
3. Use an official historical app, signed with the same signing key, that can open that database safely.
4. Export a JSON backup from that historical app, then import it through the current app's validation path.
5. If safe recovery cannot be performed, stop and seek manual assistance.

## Change control

Changing the supported range, current-version declaration, provenance, migration path, or required fixture/matrix coverage requires explicit evidence and review. A schema bump must update this policy and provide a contiguous registered migration path; any gap remains unsupported and must fail closed.
