/*
 * Copyright (c) 2022 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
