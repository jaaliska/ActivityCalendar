package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.ActivityFileParser
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.DemoDataFile
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Puts the sample activities shipped with the app into the repository. */
class LoadDemoData(
    private val repository: ActivityRepository,
    private val parser: ActivityFileParser,
    private val file: DemoDataFile,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * Stores the sample activities, moved forward by whole weeks so that the newest of them
     * falls within the last seven days and every one of them keeps its weekday.
     *
     * @return how many activities were new
     */
    suspend operator fun invoke(): Int {
        val parsed = file.open().use { parser.parse(it) }
        val newest = parsed.activities.maxOfOrNull { it.startTimeLocal } ?: return 0
        val weeks = ChronoUnit.WEEKS.between(newest.toLocalDate(), LocalDate.now(clock))
        val demo = parsed.activities.map { activity ->
            activity.copy(
                startTimeLocal = activity.startTimeLocal.plusWeeks(weeks),
                source = ActivitySourceType.DEMO,
            )
        }
        return repository.save(demo)
    }
}
