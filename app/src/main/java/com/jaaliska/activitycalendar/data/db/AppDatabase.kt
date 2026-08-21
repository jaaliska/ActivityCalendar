package com.jaaliska.activitycalendar.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ActivityEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}
