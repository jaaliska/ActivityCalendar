package com.jaaliska.activitycalendar.ui

import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates the way every screen writes them: 18 August 2026. */
val UI_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
