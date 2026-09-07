package com.jaaliska.activitycalendar.ui

import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates the way every screen writes them: 18 August 2026. */
val UI_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** Months the way the calendar names them: August 2026. */
val UI_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ENGLISH)

/** Days the way the panel heads them: Thu, Sep 3. */
val UI_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)

/** The day a period starts on: Sep 1. */
val UI_PERIOD_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

/** The day a period ends on when it started in the same month: just the 7 of Sep 1 – 7. */
val UI_PERIOD_DAY_OF_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d", Locale.ENGLISH)

/** Start times inside a day: 07:12. */
val UI_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
