package com.jaaliska.activitycalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.jaaliska.activitycalendar.ui.theme.ActivityCalendarTheme
import kotlinx.coroutines.launch

// Одноразовый экран спайка (Блок 0). В main не мержится.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityCalendarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpikeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun SpikeScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf(listOf("Нажми «Разрешения», потом «30 дней».")) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        lines = listOf("Выдано после диалога: ${granted.size} из ${SPIKE_PERMISSIONS.size}") +
            granted.map { "  + $it" }
    }

    fun read(days: Long) {
        lines = listOf("Читаю за $days дней…")
        scope.launch {
            lines = runCatching { runSpike(context, days) }
                .getOrElse { listOf("Упало: ${it::class.simpleName}: ${it.message}") }
        }
    }

    Column(modifier = modifier.padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { permissionLauncher.launch(SPIKE_PERMISSIONS) }) { Text("Разрешения") }
            Button(onClick = { read(30) }) { Text("30 дней") }
            Button(onClick = { read(1095) }) { Text("3 года") }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(lines) { line ->
                Text(text = line, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
