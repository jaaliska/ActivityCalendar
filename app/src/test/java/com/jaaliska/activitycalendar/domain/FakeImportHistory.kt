package com.jaaliska.activitycalendar.domain

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** An import history that lives only as long as the test. */
class FakeImportHistory : ImportHistory {

    override val lastImport: MutableStateFlow<LocalDate?> = MutableStateFlow(null)

    override suspend fun record(date: LocalDate) {
        lastImport.value = date
    }
}
