# Activity Calendar

A month calendar of workouts from a Garmin watch, where the type of every session is readable
at a glance — twenty of them, for any month of any year.

[![CI](https://github.com/jaaliska/ActivityCalendar/actions/workflows/ci.yml/badge.svg)](https://github.com/jaaliska/ActivityCalendar/actions/workflows/ci.yml)

## Why

Strava has a month calendar with activity icons, which is a good way to see how a week went —
but it cannot be paged back. Garmin Connect keeps the whole history, yet the types of workouts
are not told apart visually, so a month of training reads as a list, not as a shape.

This app takes the useful half of each: the calendar of Strava, the history of Garmin.

## Screenshots

| Month and the last seven days | A picked day | First run |
|---|---|---|
| ![Month](docs/screenshots/calendar.jpg) | ![Day](docs/screenshots/day.jpg) | ![First run](docs/screenshots/first-run.jpg) |

| Health Connect | Import | Settings |
|---|---|---|
| ![Health Connect](docs/screenshots/health-connect.jpg) | ![Import](docs/screenshots/import.jpg) | ![Settings](docs/screenshots/settings.jpg) |

## Where the workouts come from

| Source | What it gives |
|---|---|
| Health Connect | New workouts, on their own: in the background, and at every opening of the app |
| CSV from the Garmin Connect website | The history from before Health Connect was connected |
| Demo data | Sample workouts for anyone with neither a watch nor an export |

A local Room database is the only thing the screens read, which is what makes paging through
years instant and the app usable offline. **The app is a mirror of its sources, not an editor:**
workouts are corrected in Garmin, and a session deleted there disappears from the calendar after
the next sync.

Without a Garmin watch the app still works: load the demo data from the first screen, or import
any CSV in the format the Garmin Connect website exports.

## Built with

Kotlin, Jetpack Compose, Room, Health Connect, WorkManager, DataStore, navigation-compose.
Minimum Android 9 (API 28) — the Health Connect app does not exist below it.

- **MVVM with one immutable `UiState` per screen** ([ADR-002](docs/adr/ADR-002-presentation-layer.md)).
- **Manual dependency container instead of a DI framework** ([ADR-003](docs/adr/ADR-003-navigation-and-dependencies.md)).
- **Ports in `domain`, implementations in `data`, use cases where there is logic to hold**
  ([ADR-004](docs/adr/ADR-004-use-cases.md)). No `android.*` in the domain, no `data` imports in the UI.
- **Why these data sources and not the Garmin API** ([ADR-001](docs/adr/ADR-001-data-source.md)).
- 147 unit tests; CI runs them with lint and a debug build on every push.

## Build

Needs JDK 21.

```bash
./gradlew test lint assembleDebug
```

A release build is signed only if `local.properties` names a keystore, and that file is never
committed:

```properties
releaseKeystoreFile=keystore.jks
releaseKeystorePassword=…
releaseKeyAlias=…
releaseKeyPassword=…
```

Without those lines `./gradlew assembleRelease` still builds, and leaves the APK unsigned.

## Documentation

The design and decision documents are in Russian, in [`docs/`](docs/):

- [milestone-dev-plan.md](docs/plan/milestone-dev-plan.md) — what is built, in what order, and why.
- [mvp-backlog.md](docs/mvp-backlog.md) — user stories with acceptance criteria.
- [data-model-notes.md](docs/data-model-notes.md) — the fields of every source and the domain model.
- [data-sources.md](docs/data-sources.md) — where the data comes from and when there is none.
- [ux/](docs/ux/) — information architecture, theme, mockups.

## Status

A personal project, built for its author's own use. It is not on Google Play, and there is no
account, server or telemetry behind it: the data stays on the phone.
