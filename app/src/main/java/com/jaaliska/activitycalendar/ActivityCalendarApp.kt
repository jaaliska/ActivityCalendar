package com.jaaliska.activitycalendar

import android.app.Application
import com.jaaliska.activitycalendar.data.source.FixtureActivitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ActivityCalendarApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Runs on every start; already stored activities are skipped.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.activityRepository.save(FixtureActivitySource.ALL)
        }
    }
}
