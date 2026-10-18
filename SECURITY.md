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
