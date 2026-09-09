package com.jaaliska.activitycalendar.ui.calendar

import com.jaaliska.activitycalendar.domain.ActivityType

/** Whether a distance recorded for this type says anything worth showing. */
val ActivityType.showsDistance: Boolean
    get() = when (this) {
        ActivityType.CYCLING,
        ActivityType.HIKING,
        ActivityType.RUNNING,
        ActivityType.SKIING,
        ActivityType.SNOWBOARDING,
        ActivityType.SWIMMING,
        ActivityType.WALKING,
        ActivityType.UNKNOWN,
        -> true

        ActivityType.BADMINTON,
        ActivityType.BASKETBALL,
        ActivityType.BOXING,
        ActivityType.DANCING,
        ActivityType.MARTIAL_ARTS,
        ActivityType.SOCCER,
        ActivityType.STRENGTH_TRAINING,
        ActivityType.STRETCHING,
        ActivityType.TABLE_TENNIS,
        ActivityType.TENNIS,
        ActivityType.VOLLEYBALL,
        ActivityType.YOGA,
        -> false
    }
