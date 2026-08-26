package com.jaaliska.activitycalendar.ui.csvimport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.ui.components.DetailTopBar

@Composable
fun ImportScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = stringResource(R.string.import_title), onBack = onBack) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        }
    }
}
