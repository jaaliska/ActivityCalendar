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
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> ActivityType.BASKETBALL
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> ActivityType.BOXING

    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    -> ActivityType.CYCLING

    ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> ActivityType.DANCING
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ActivityType.HIKING
    ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> ActivityType.MARTIAL_ARTS

    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    -> ActivityType.RUNNING

    ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> ActivityType.SKIING
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> ActivityType.SNOWBOARDING
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> ActivityType.SOCCER

    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    -> ActivityType.STRENGTH_TRAINING

    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
    -> ActivityType.STRETCHING

    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    -> ActivityType.SWIMMING

    ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> ActivityType.TABLE_TENNIS
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> ActivityType.TENNIS
    ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> ActivityType.VOLLEYBALL
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ActivityType.WALKING
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> ActivityType.YOGA
    else -> ActivityType.UNKNOWN
}
