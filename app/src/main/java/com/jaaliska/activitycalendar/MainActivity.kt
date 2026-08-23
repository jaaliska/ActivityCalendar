package com.jaaliska.activitycalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.jaaliska.activitycalendar.ui.activities.ActivityListScreen
import com.jaaliska.activitycalendar.ui.theme.ActivityCalendarTheme
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ActivityCalendarApp).container
        setContent {
            ActivityCalendarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ActivityListScreen(
                        repository = container.activityRepository,
                        importer = container.csvImporter,
                        month = YearMonth.now(),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
