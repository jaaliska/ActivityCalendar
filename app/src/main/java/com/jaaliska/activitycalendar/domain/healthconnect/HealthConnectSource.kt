package com.jaaliska.activitycalendar.domain.healthconnect

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySource

/** Whether Health Connect can be read on this phone. */
enum class HealthConnectAvailability {
    AVAILABLE,

    /** Not installed or too old to talk to; the user can get it from the store. */
    NOT_INSTALLED,

    /** This phone cannot run Health Connect at all. */
    UNSUPPORTED,
}

/**
 * What Health Connect changed since a token was taken.
 *
 * @property activities the sessions added or updated since then
 * @property nextToken the token to ask with next time
 * @property expired the token was too old to answer with, everything has to be read again
 */
data class HealthConnectChanges(
    val activities: List<Activity>,
    val nextToken: String,
    val expired: Boolean,
)

/** Health Connect as this app reads it: a source of activities that also reports what changed. */
interface HealthConnectSource : ActivitySource {

    /** Every permission the app asks for, in one dialog: the required ones and background reading. */
    val permissions: Set<String>

    /** Whether Health Connect can be read on this phone. */
    fun availability(): HealthConnectAvailability

    /**
     * Whether the permissions the app cannot work without are granted: sessions, distance and
     * history deeper than 30 days. Anything missing here means the app is not connected.
     */
    suspend fun hasRequiredPermissions(): Boolean

    /**
     * Whether Health Connect may be read while the app is not on screen. Without it the app still
     * works, it just reads only while it is open.
     */
    suspend fun canReadInBackground(): Boolean

    /** Returns a token standing for the current end of the change log. */
    suspend fun changesToken(): String

    /** Returns everything that changed since [token]. */
    suspend fun changesSince(token: String): HealthConnectChanges
}
