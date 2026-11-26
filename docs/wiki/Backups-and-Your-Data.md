# Backups and your data

Your workouts live only on your phone. If the phone is lost or the app is removed, they are gone unless you have a backup. Make one regularly, and keep a copy somewhere other than the phone.

## Complete backup (JSON)

Settings › Data Management › Complete Backup (JSON) exports everything: exercises, groups, programs, interval routines and history. Settings shows when you last made one.

A complete backup also keeps warm-up marks and effort ratings, the muscles you chose for your own exercises, your body weights and each exercise's share of body weight. A backup that holds them can only be restored by version 1.2 or later; older versions refuse it rather than lose them.

Importing a complete backup replaces all your data, so the app first asks you to save a backup of your current data. Import becomes available once that backup is saved and checked. On a phone with no data yet, as when moving to a new phone, the import starts straight away.

## CSV

CSV export and import cover groups, exercises and records, for use in a spreadsheet. CSV import adds to your data and skips what is already there; it never replaces anything. Dates are written as 2026-10-08 and times as 07:30. An input template helps you prepare records to import. The records file also carries warm-up marks, effort ratings and, for a one-sided exercise, each side's own weight, assistance and distance, in optional last columns; the exercises file carries the muscles you chose for your own exercises. Older files import as before, and older app versions ignore the new columns. The body weight list has its own CSV file (a date and kg), and CSV files can also import an openScale CSV export, read by its date and weight columns as kilograms, with a preview and an undo of the latest import.

## Recovery files

If the database ever cannot be opened, the app shows a recovery screen instead of starting empty. From there, or from Settings › Complete Backup, you can export a recovery file or restore from one. A restore keeps your current data inside the app, in case you need it back.

## Moving to a new phone

1. On the old phone, make a complete backup and copy the file to the new phone.
2. On the new phone, install the app and import the backup.
