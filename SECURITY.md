# Security policy

Kalimory has no internet permission, no account and no server, so most attacks that need a network don't apply. What remains is the app itself: the files it reads and writes, the data it keeps on the phone, and the APKs it ships as.

## Supported versions

Only the latest release gets security fixes. It is listed on the [releases page](https://github.com/WildeBeast2521/kalimory/releases/latest).

## Reporting a vulnerability

Please don't open a public issue for a security problem. Report it privately through [GitHub's vulnerability reporting](https://github.com/WildeBeast2521/kalimory/security/advisories/new) instead. Only the maintainer can see the report.

Include the app version (Settings shows it at the bottom), your device and Android version, and the steps that show the problem. As with any issue, don't attach your own backups or workout history; make up a small example file instead.

Kalimory is maintained by one person, so there is no guaranteed response time. You will get a reply in the advisory, and a confirmed problem is fixed in the next release with credit to you, unless you'd rather stay anonymous.

## What counts

For example:

- a backup, CSV or shared-program file that makes the app crash in a way that loses data, run code, or read or write files it shouldn't;
- another app on the phone reading Kalimory's data, or starting parts of it that should be private;
- workout data ending up somewhere the user didn't send it, including in a crash report;
- a release APK that doesn't match its published SHA-256 or isn't signed with the project's key.

Problems that need a rooted phone, physical access to an unlocked phone, or a change to Android itself are outside what the app can defend against. They are still welcome as normal issues if the app could do better.

## Threat model

What the app protects and from whom, so a report can say which part it concerns.

- **What is worth protecting:** a person's training history and plans, and their backup files. Nothing else
  (no account, no payment, no location) is held.
- **Where data can enter:** only files the person chooses (a backup, a CSV file, a shared file or a recovery
  file) through the system file picker. There is no network input, and nothing arrives from another app unless
  the person shares it to Kalimory. Every file is checked before it is used; a bad file is refused and the
  existing data is left alone.
- **Where data can leave:** only files and share sheets the person starts, and a crash report the person
  chooses to share. Nothing is sent automatically, and the release build has no internet permission.
- **What the app defends against:** malformed or hostile files, another app reading its data, and data
  appearing somewhere the person did not send it.
- **What it does not defend against:** a rooted or already compromised phone, someone with the unlocked phone,
  and a backup file the person leaves somewhere others can read. A backup is plain JSON.

Details of the file formats and the rules for changing them are in `docs/storage-and-backup.md`.
