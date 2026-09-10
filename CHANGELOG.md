# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[semantic versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — unreleased

The first release: a month calendar of Garmin workouts where the type of every session is
readable at a glance, for any month of any year.

### Added

- **Month calendar.** A 7×6 grid, Monday first, with an icon per activity type and a colour per
  family of sports; a day with several sessions shows a stack and `+N`. Days with a workout are
  shaded, so the shape of the week is visible without counting.
- **Paging through time.** Swipe or arrows move month by month, `Today` returns to the current
  month, and a picker jumps to any month of any year.
- **Day details and the last seven days.** The panel under the grid shows the activities of the
  picked day, or — while no day is picked — what the last seven days held, by type.
- **Health Connect.** Workouts arrive on their own: in the background, and at every opening of
  the app for anyone who did not grant background reading. Manual `Sync now` reports what it did.
- **Mirroring.** A workout deleted in Garmin disappears from the calendar too: every sync
  re-reads the last 30 days, and `Rebuild from Health Connect` re-reads the whole history.
- **CSV import.** The export from the Garmin Connect website fills in the history that Health
  Connect never had, with a report of what was added, what was a duplicate and what was skipped.
- **Export.** The stored history is written back as a CSV this app can import again — a backup
  that survives reinstalling.
- **Demo data.** Sample workouts for anyone without a watch or an export; they are marked as
  demo data and removed with one action.
- **Twenty activity types**, from running and cycling to tennis, swimming and martial arts.
  Anything else is shown as `Other` rather than dropped.
- **Colour schemes.** Four schemes to choose from; light and dark follow the phone.

### Notes

- Activities are never edited in the app: it mirrors its sources, and workouts are corrected
  in Garmin.
- Requires Android 9 or newer. Health Connect receives Garmin workouts on Android 14 and newer;
  below that the CSV import is the way in.
