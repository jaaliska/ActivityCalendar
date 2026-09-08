package com.jaaliska.activitycalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jaaliska.activitycalendar.data.healthconnect.HealthConnectSyncWorker
import com.jaaliska.activitycalendar.ui.navigation.AppNavHost
import com.jaaliska.activitycalendar.ui.navigation.Destination
import com.jaaliska.activitycalendar.ui.theme.ActivityCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ActivityCalendarApp).container
        // Opening the app is what asks for a catch-up sync; a rotation is not a new opening.
        if (savedInstanceState == null) {
            container.syncHealthConnectOnOpen()
            HealthConnectSyncWorker.schedule(this)
        }
        val startDestination = if (intent?.action in RATIONALE_ACTIONS) {
            Destination.HEALTH_CONNECT
        } else {
            Destination.CALENDAR
        }
        setContent {
            val scheme by container.colorScheme.collectAsState()
            ActivityCalendarTheme(scheme = scheme) {
                AppNavHost(container, startDestination)
            }
        }
    }

    private companion object {
        // Health Connect opens the app on these to have it explain why it reads the data.
        val RATIONALE_ACTIONS = setOf(
            "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE",
            "android.intent.action.VIEW_PERMISSION_USAGE",
        )
    }
}
