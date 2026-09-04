package com.jaaliska.activitycalendar.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.jaaliska.activitycalendar.domain.ActivityType

/**
 * Maps the `exerciseType` of a Health Connect session onto a domain type.
 *
 * @param exerciseType one of the `EXERCISE_TYPE_*` values
 * @return the domain type, [ActivityType.UNKNOWN] for anything not supported
 */
internal fun healthConnectActivityTypeOf(exerciseType: Int): ActivityType = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> ActivityType.BADMINTON
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ActivityType.CYCLING
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> ActivityType.RUNNING
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> ActivityType.STRENGTH_TRAINING
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ActivityType.WALKING
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> ActivityType.YOGA
    else -> ActivityType.UNKNOWN
}
