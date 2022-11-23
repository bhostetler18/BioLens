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

package com.uf.automoth.ui.imaging.scheduler

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.PendingSession
import com.uf.automoth.imaging.ImagingSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

class ImagingSchedulerViewModel : ViewModel() {
    var estimatedImageSizeBytes: Double = 0.0
    var imagingSettings = ImagingSettings()
    var sessionName: String? = null
    var sessionDate: LocalDate? = null
    var sessionTime: LocalTime? = null
    val allPendingSessions: LiveData<List<PendingSession>> =
        AutoMothRepository.allPendingSessionsFlow.asLiveData()

    val scheduleStartTime: OffsetDateTime?
        get() {
            if (sessionDate == null || sessionTime == null) {
                return null
            }
            return OffsetDateTime.of(sessionDate, sessionTime, OffsetDateTime.now().offset)
        }
}
