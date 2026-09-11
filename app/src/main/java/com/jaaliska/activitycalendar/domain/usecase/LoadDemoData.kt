package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.ActivityFileParser
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.DemoDataFile
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Puts the sample activities shipped with the app into the repository. */
class LoadDemoData(
    private val repository: ActivityRepository,
    private val parser: ActivityFileParser,
    private val file: DemoDataFile,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * Stores the sample activities, moved forward by whole weeks so that the last week of the
     * file becomes the current week and every activity keeps its weekday. What would land later
     * than now is left out: a calendar of past workouts must not show workouts still to come.
     *
     * @return how many activities were new
     */
    suspend operator fun invoke(): Int {
        val parsed = file.open().use { parser.parse(it) }
        val newest = parsed.activities.maxOfOrNull { it.startTimeLocal } ?: return 0
        val now = LocalDateTime.now(clock)
        val weeks = ChronoUnit.WEEKS.between(
            newest.toLocalDate().mondayOfWeek(),
            now.toLocalDate().mondayOfWeek(),
        )
        val demo = parsed.activities
            .map { activity ->
                activity.copy(
                    startTimeLocal = activity.startTimeLocal.plusWeeks(weeks),
                    source = ActivitySourceType.DEMO,
                )
            }
            .filter { it.startTimeLocal <= now }
        return repository.save(demo)
    }

    private fun LocalDate.mondayOfWeek(): LocalDate =
        with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
