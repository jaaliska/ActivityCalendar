package com.jaaliska.activitycalendar.ui.settings

import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.FakeAppearanceSettings
import com.jaaliska.activitycalendar.domain.FakeImportHistory
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val appearance = FakeAppearanceSettings()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `picked scheme is remembered and shown`() = runTest(dispatcher) {
        val viewModel = viewModel()
        // The state is only built while the screen looks at it.
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.selectColorScheme(ColorSchemeChoice.GREEN)
        advanceUntilIdle()

        assertEquals(ColorSchemeChoice.GREEN, appearance.colorScheme.value)
        assertEquals(ColorSchemeChoice.GREEN, viewModel.state.value.colorScheme)
    }

    @Test
    fun `screen opens on the scheme the app is painted with`() = runTest(dispatcher) {
        appearance.colorScheme.value = ColorSchemeChoice.CRIMSON

        val state = viewModel().state.first { it.colorScheme != ColorSchemeChoice.BLUE }

        assertEquals(ColorSchemeChoice.CRIMSON, state.colorScheme)
    }

    private fun viewModel() = SettingsViewModel(
        importHistory = FakeImportHistory(),
        appearanceSettings = appearance,
        getHealthConnectStatus = GetHealthConnectStatus(
            FakeHealthConnectSource(),
            FakeHealthConnectSyncState(),
        ),
    )
}
