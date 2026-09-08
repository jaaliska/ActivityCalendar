package com.jaaliska.activitycalendar.ui.calendar

import com.jaaliska.activitycalendar.domain.ActivityType

/** Whether a distance recorded for this type says anything worth showing. */
val ActivityType.showsDistance: Boolean
    get() = when (this) {
        ActivityType.CYCLING,
        ActivityType.RUNNING,
        ActivityType.WALKING,
        ActivityType.UNKNOWN,
        -> true

        ActivityType.BADMINTON,
        ActivityType.STRENGTH_TRAINING,
        ActivityType.YOGA,
        -> false
    }
