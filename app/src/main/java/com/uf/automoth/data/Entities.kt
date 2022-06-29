package com.uf.automoth.data

import androidx.room.*
import java.time.OffsetDateTime

@Entity(tableName = "sessions")
data class Session(
    val name: String,
    val directory: String,
    val started: OffsetDateTime,
    val latitude: Double,
    val longitude: Double,
    val interval: Int
) {
    @PrimaryKey(autoGenerate = true) var sessionID: Long = 0
    var completed: OffsetDateTime? = null
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
    @ColumnInfo(index = true) val parentSessionID: Long
) {
    @PrimaryKey(autoGenerate = true) var imageID: Long = 0
    var containsMoths: Boolean? = null
}

@Entity
data class SessionWithImages(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "sessionID",
        entityColumn = "parentSessionID"
    )
    val images: List<Image>
)
