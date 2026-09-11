package com.jaaliska.activitycalendar.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @Test
    fun `rows of the dropped fixture become demo data and the rest keep their source`() {
        val database = versionOneDatabase()
        database.execSQL(
            "INSERT INTO activities " +
                "(startTimeLocal, type, durationSeconds, distanceMeters, title, sourceId, source) " +
                "VALUES ('2026-08-05T07:15:00', 'RUNNING', 2280, 6500.0, NULL, NULL, 'MANUAL'), " +
                "('2026-08-06T07:15:00', 'RUNNING', 2280, 6500.0, NULL, NULL, 'GARMIN_CSV')"
        )

        AppDatabase.MIGRATION_1_2.migrate(database)

        database.query("SELECT source FROM activities ORDER BY startTimeLocal").use { rows ->
            rows.moveToFirst()
            assertEquals("DEMO", rows.getString(0))
            rows.moveToNext()
            assertEquals("GARMIN_CSV", rows.getString(0))
        }
    }

    private fun versionOneDatabase(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(TABLE_V1)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        return FrameworkSQLiteOpenHelperFactory()
            .create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(null)
                    .callback(callback)
                    .build(),
            )
            .writableDatabase
    }

    private companion object {
        const val TABLE_V1 =
            "CREATE TABLE IF NOT EXISTS `activities` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTimeLocal` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, `distanceMeters` REAL, " +
                "`title` TEXT, `sourceId` TEXT, `source` TEXT NOT NULL)"
    }
}
