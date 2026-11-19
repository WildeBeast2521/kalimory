# Storage and backup

How Kalimory keeps data and moves it in and out, for anyone changing code that touches it. The user-facing
version is the wiki page "Backups and your data". Nothing here sends data anywhere: there is no network access,
and every file moves through the Storage Access Framework or a share sheet that the person starts.

## What is stored

- **The database (Room).** Exercises, groups, programs, interval routines, to-dos and workout history. History is
  kept in two places: the original per-set records, and the newer workout model (sessions, exercises and sets).
  Both are read together, so a person sees one history. Migrations only add; they never drop a row, and a
  committed schema file is never edited.
- **Settings** live in SharedPreferences, not in the database. They are **not** part of a backup: restoring on a
  new phone starts with default settings. The Backup screen says so.
- **Crash reports** are kept on the phone and offered for sharing on the next launch. They are left out of
  Android's own backup and are never sent automatically.

## Files that go in and out

| File | Written by | Read by | What it does on import |
| --- | --- | --- | --- |
| Full backup (JSON) | Settings › Your data › Full backup | Restore backup | Replaces **all** current data, after the person has saved a backup of what is there |
| CSV (groups, exercises, records) | CSV files | CSV files | Adds to the data and skips what is already there; never replaces |
| Shared file (groups, exercises, programs, interval programs) | Share | Share | Adds what it holds, after checking its format version; a file from a newer format is refused |
| Recovery file (a zip of the database) | the recovery screen, or the Backup screen | the same | Restores the raw database, but only once the unpacked copy opens, passes SQLite's integrity check and has a supported version; the current files are moved aside to a `before-restore` folder, never deleted |

The JSON backup has a version number. The current version is `BackupService.CURRENT_VERSION`; the oldest the
app still reads is `MIN_VERSION`. A backup that carries data an older app version cannot hold (for example
warm-up marks and effort ratings) is refused by that older version rather than silently losing the data.

## Rules for changing any of this

1. **Round trip.** Anything the app writes it must read back. A value the app can save must be accepted on
   import (a negative distance once was not, and that was a bug). Add a test that writes, reads and compares.
2. **Never invent a value.** Import and migration use what the file or the old data contains. A missing number
   stays missing; it is not turned into a default that looks like the person's.
3. **Validate before touching data.** A file is checked as a whole before anything is written. An invalid file is
   refused with a message that says what is wrong, and the existing data is left as it was. The summary
   messages are resources, in every language the app ships. The per-line reasons in a CSV or shared-file
   import ("Line 4: Invalid date ...") are still English text; new ones should be resources, and moving the
   existing ones is open work.
4. **Restore needs a verified backup first**, unless the database is empty. The Import button is enabled only
   after a backup of the current data has been written, read back byte for byte, and parsed as a valid backup
   (`BackupFileIo.writeAndVerify`). Do not bypass it.
5. **Additive formats.** A new field is optional on read, so older files still load. A change that older app
   versions cannot hold bumps the backup version and is listed under **Changed** or **Fixed** in the changelog,
   because it breaks what a user may have.
6. **No private material in tests.** Fixtures use made-up data. Never commit a real backup or export.

## Where the code is

- `viewmodel/BackupService.kt`: building, validating and restoring the JSON backup.
- `data/BackupDao.kt`: the database snapshot and the row counts the restore preview shows.
- `data/RecoveryRestore.kt`: the recovery screen's export and restore.
- `viewmodel/CsvTransfer.kt` and `viewmodel/CommunityShare.kt`: CSV and shared files.
- `app/src/test` and `app/src/androidTest`: backup, CSV and share round-trip tests; run them with the full check
  in `AGENTS.md` after any change here.
