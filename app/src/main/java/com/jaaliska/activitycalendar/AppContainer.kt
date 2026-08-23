package com.jaaliska.activitycalendar

import android.content.Context
import androidx.room.Room
import com.jaaliska.activitycalendar.data.csv.CsvImporter
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.domain.ActivityRepository

/** Holds the objects that live as long as the app does. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room
        .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
        .build()

    val activityRepository: ActivityRepository = RoomActivityRepository(database.activityDao())

    val csvImporter: CsvImporter = CsvImporter(activityRepository)

    private companion object {
        const val DATABASE_NAME = "activity-calendar.db"
    }
}
