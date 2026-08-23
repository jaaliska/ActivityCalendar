package com.jaaliska.activitycalendar

import android.app.Application

class ActivityCalendarApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
