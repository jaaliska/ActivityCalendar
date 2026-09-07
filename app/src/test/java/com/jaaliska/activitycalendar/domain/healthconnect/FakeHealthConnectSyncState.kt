package com.jaaliska.activitycalendar.domain.healthconnect

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** What a test says the app remembers between synchronisations. */
class FakeHealthConnectSyncState(
    changesToken: String? = null,
    lastSync: Instant? = null,
    seenAnySession: Boolean = false,
    permissionsAsked: Boolean = false,
) : HealthConnectSyncState {

    override val changesToken: MutableStateFlow<String?> = MutableStateFlow(changesToken)
    override val lastSync: MutableStateFlow<Instant?> = MutableStateFlow(lastSync)
    override val seenAnySession: MutableStateFlow<Boolean> = MutableStateFlow(seenAnySession)
    override val permissionsAsked: MutableStateFlow<Boolean> = MutableStateFlow(permissionsAsked)

    override suspend fun record(changesToken: String, syncedAt: Instant, sawSessions: Boolean) {
        this.changesToken.value = changesToken
        lastSync.value = syncedAt
        seenAnySession.value = sawSessions || seenAnySession.value
    }

    override suspend fun recordPermissionsAsked() {
        permissionsAsked.value = true
    }
}
