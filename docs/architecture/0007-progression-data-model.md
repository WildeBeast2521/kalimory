# ADR 0007: Progression data model

- **Status:** Accepted
- **Date:** 2026-10-01

## Context

ADR 0005 defines the progression model and ADR 0006 the catalogue. The user's library, history and backups must be able to refer to catalogue steps, and custom exercises must be able to join built-in chains. Migrations stay additive (ADR 0003), and backups must cover all new data.

## Decisions

1. **The catalogue is not stored in the database.** It is compiled into the app (ADR 0006). The database stores only the user's choices about it.

2. **`exercises.catalogId`.**
   - A new nullable text column links a library exercise to a catalogue step, for example `push.incline`.
   - A unique index allows at most one library exercise per step. Several exercises may have no link, since SQLite treats nulls as distinct.
   - Adding a step to the library creates an ordinary `Exercise` row: its name, kind and laterality come from the catalogue, and its target from the step's move-on standard. The user can edit everything afterwards; the link stays.
   - History links to the library exercise, as it does today. Session snapshots keep what was trained (ADR 0002).

3. **`chain_placements`.** A new table places a custom exercise in a built-in chain:
   - `exerciseId` is the primary key, a foreign key to `exercises` that cascades on delete;
   - `chainId` is the chain;
   - `afterStepId` is the step it follows, or null to place it first.

   An exercise sits in at most one built-in chain; its group and level still describe its place in the user's own chains.

4. **Preferences, not tables, for settings.** The followed chains, the weekly goal, and "hide suggestions" are small settings kept in SharedPreferences, like the other workout settings. Followed chains are stored by chain id.
   - As built (PR #97): a chain with a step in the library is followed unless the user stops following it, so the preference stores the *unfollowed* chain ids. Adding a step follows its chain without another tap.

5. **Database version 25, backup format 11.**
   - Migration 24→25 adds the column, its unique index and the table. Nothing is rewritten.
   - Backup format 11 adds `catalogId` to each exercise and a `chainPlacements` list.
   - A format 1–10 backup restores with no links. Older app versions reject format 11 as unsupported rather than dropping the links.
   - Validation rejects a placement whose exercise is missing, and a duplicate `catalogId`. It reports an unknown `catalogId` or chain id as an anomaly and keeps it, because a newer catalogue may define it.

6. **CSV and community share.** The CSV formats do not change. Community share includes `catalogId`, so a shared exercise stays linked when both sides have the step.

## Consequences

- Links survive catalogue updates, because ids are stable (ADR 0006) and unknown ids are kept, not dropped.
- Deleting a library exercise removes its placement; history keeps its snapshots.
- `docs/development/supported-database-versions.md` and `docs/development/backup-validation.md` gain the new version and format.
