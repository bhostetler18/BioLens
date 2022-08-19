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
