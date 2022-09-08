package com.uf.automoth

import com.uf.automoth.data.PendingSession
import com.uf.automoth.imaging.AutoStopMode
import com.uf.automoth.imaging.ImagingScheduler.SchedulingConflictType
import com.uf.automoth.imaging.ImagingSettings
import com.uf.automoth.imaging.conflictWith
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class PendingSessionConflictUnitTest {
    @Test
    fun testSchedulingConflict() {
        val existing = pendingSessionWithDuration(8, 0, 60)
        var new = pendingSessionWithDuration(8, 30, 60)
        assertEquals(new.conflictWith(existing), SchedulingConflictType.WILL_CANCEL)
        assertEquals(existing.conflictWith(new), SchedulingConflictType.WILL_BE_CANCELLED)
        new = pendingSessionWith(7, 0)
        assertEquals(new.conflictWith(existing), SchedulingConflictType.WILL_BE_CANCELLED)
        assertEquals(existing.conflictWith(new), SchedulingConflictType.WILL_CANCEL)
        new = pendingSessionWithDuration(7, 0, 59)
        assertEquals(new.conflictWith(existing), null)
        assertEquals(existing.conflictWith(new), null)
    }

    private fun pendingSessionWithDuration(startHour: Int, startMinute: Int, durationMinutes: Int) =
        PendingSession(
            "",
            ImagingSettings(1, AutoStopMode.TIME, durationMinutes),
            OffsetDateTime.of(2000, 1, 1, startHour, startMinute, 0, 0, ZoneOffset.UTC)
        )

    private fun pendingSessionWith(startHour: Int, startMinute: Int) = PendingSession(
        "",
        ImagingSettings(1, AutoStopMode.OFF, 0),
        OffsetDateTime.of(2000, 1, 1, startHour, startMinute, 0, 0, ZoneOffset.UTC)
    )
}
