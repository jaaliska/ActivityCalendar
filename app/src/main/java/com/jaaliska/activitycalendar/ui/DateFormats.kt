package com.jaaliska.activitycalendar.ui

import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates the way every screen writes them: 18 August 2026. */
val UI_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** Months the way the calendar names them: August 2026. */
val UI_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ENGLISH)
