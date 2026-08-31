package com.jaaliska.activitycalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jaaliska.activitycalendar.ui.navigation.AppNavHost
import com.jaaliska.activitycalendar.ui.theme.ActivityCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ActivityCalendarApp).container
        setContent {
            ActivityCalendarTheme {
                AppNavHost(container)
            }
        }
    }
}
