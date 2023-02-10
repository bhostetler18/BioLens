/*
 * Copyright (c) 2022-2023 University of Florida
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

package com.uf.biolens.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.uf.biolens.imaging.AutoStopMode
import com.uf.biolens.imaging.ImagingSettings
import java.time.OffsetDateTime

@Entity(tableName = "sessions")
data class Session(
    val name: String,
    val directory: String,
    val started: OffsetDateTime,
    val latitude: Double?,
    val longitude: Double?,
    val interval: Int,
    var completed: OffsetDateTime? = null,
    @PrimaryKey(autoGenerate = true) var sessionID: Long = 0
) {
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }

    companion object {
        fun isValid(sessionName: String): Boolean {
            val trimmed = sessionName.trim()
            return trimmed.isNotEmpty() && !trimmed.contains(",")
        }
    }
}

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["sessionID"],
            childColumns = ["parentSessionID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Image(
    val index: Int,
    val filename: String,
    val timestamp: OffsetDateTime,
    @ColumnInfo(index = true) val parentSessionID: Long,
    @PrimaryKey(autoGenerate = true) var imageID: Long = 0
)

@Entity
data class SessionWithImages(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "sessionID",
        entityColumn = "parentSessionID"
    )
    val images: List<Image>
)

@Entity(tableName = "pending_sessions")
data class PendingSession(
    val name: String,
    val interval: Int,
    val autoStopMode: AutoStopMode,
    val autoStopValue: Int = 0,
    val scheduledDateTime: OffsetDateTime,
    @PrimaryKey(autoGenerate = true) var requestCode: Long = 0
) {
    constructor(name: String, imagingSettings: ImagingSettings, startTime: OffsetDateTime) : this(
        name,
        imagingSettings.interval,
        imagingSettings.autoStopMode,
        imagingSettings.autoStopValue,
        startTime
    )

    fun getStopTime(): OffsetDateTime? {
        return when (autoStopMode) {
            AutoStopMode.OFF -> null
            AutoStopMode.TIME -> scheduledDateTime.plusMinutes(autoStopValue.toLong())
            AutoStopMode.IMAGE_COUNT -> scheduledDateTime.plusSeconds(autoStopValue.toLong() * interval)
        }
    }
}
