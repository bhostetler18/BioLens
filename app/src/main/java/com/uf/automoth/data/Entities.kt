package com.uf.automoth.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.uf.automoth.imaging.AutoStopMode
import com.uf.automoth.imaging.ImagingSettings
import java.time.OffsetDateTime

@Entity(tableName = "sessions")
data class Session(
    val name: String,
    val directory: String,
    val started: OffsetDateTime,
    val latitude: Double,
    val longitude: Double,
    val interval: Int,
    var completed: OffsetDateTime? = null,
    @PrimaryKey(autoGenerate = true) var sessionID: Long = 0
) {
    companion object {
        fun isValid(sessionName: String): Boolean {
            return sessionName.trim().isNotEmpty()
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
