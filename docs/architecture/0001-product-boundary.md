# ADR 0001: Product boundary for the personal trainer overhaul

- **Status:** Accepted
- **Date:** 2026-08-30

## Context

Calisthenics Memory is already an Android 8+ privacy-focused bodyweight training tracker whose public promise is customization and local data export/import (`README.md:12-32`). It supports manual records, guided single/program/interval workouts, progress views, and JSON/CSV data management. It is GPL-3.0 software (`README.md:111-113`) and its timer can operate in the background with narrowly described foreground-service and wake-lock permissions (`README.md:88-93`).

The overhaul needs a product boundary before navigation and storage are reshaped. Without one, existing modes could simply be renamed while their fragmented flows remain, or online features could displace the offline, user-owned core.

## Decision

The product remains a **private, personal-first, offline calisthenics trainer**. The GPL-3.0 license, local-first operation, privacy, data portability, and deep customization are durable constraints rather than optional features.

The primary information architecture has four destinations:

1. **Today** — the immediate plan and continuity surface. It shows scheduled or manually queued work, a resumable in-progress workout, and concise recent context. It must remain useful with no network connection.
2. **Train** — one coherent workout flow. A user may start an ad-hoc exercise, a reusable template/program, or an interval workout, then record, time, skip, reorder where allowed, pause, save, resume, and finish through shared session semantics.
3. **Progress** — history and reflection. It presents completed sessions, set history, calendars, trends, and personal records from the same durable workout data rather than mode-specific reporting silos.
4. **Library** — user-owned definitions and reusable structure: exercises, groups, templates/programs, interval templates, defaults, and customization. Editing a library item must not rewrite historical workout snapshots.

Settings, backup/import/export, licenses, and other utilities remain secondary destinations. Existing capabilities may be bridged into the four destinations incrementally; this ADR does not require an immediate navigation rewrite.

### Non-goals for this overhaul

The following are explicitly deferred:

- accounts, identity, sign-in, or cross-device account recovery;
- social feeds, follows, leaderboards, public profiles, messaging, or community competition;
- hosted AI coaching, generative plans, or automatic form evaluation;
- video upload, video hosting, streaming, or cloud media processing;
- first-party cloud synchronization or a mandatory backend;
- telemetry, advertising, growth tracking, or monetization infrastructure;
- replacing user-controlled JSON/CSV backup and export with a closed format;
- expanding beyond the single-user personal training use case during the foundation work.

Local file sharing/import can remain, but it must not quietly become an online social system.

## Consequences

- Product decisions can be evaluated against four stable user intents instead of legacy screen names.
- Workout modes must converge on a common session model while retaining specialized timing and input behavior.
- Offline behavior, exportability, and restoration are acceptance criteria for new data, not cleanup work.
- Historical records must remain interpretable after exercise or template edits.
- Features that require accounts, hosted services, AI, social graphs, video hosting, or cloud sync need a later ADR and are not prerequisites for the overhaul.
- The boundary deliberately favors reliability and user ownership over rapid expansion of surface area.
