package com.uf.automoth.data.metadata

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.uf.automoth.data.Session

@Entity(tableName = "metadata_keys")
data class UserMetadataKey(
    @PrimaryKey val key: String,
    override val type: UserMetadataType
) : UserMetadataField {
    @Ignore
    override val field: String = key // match naming convention of UserMetadataField interface
}

@Entity(
    tableName = "metadata_values",
    primaryKeys = ["key", "sessionID"],
    foreignKeys = [
        ForeignKey(
            entity = UserMetadataKey::class,
            parentColumns = ["key"],
            childColumns = ["key"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Session::class,
            parentColumns = ["sessionID"],
            childColumns = ["sessionID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserMetadataValue(
    val key: String,
    val sessionID: Long,
    val stringValue: String? = null,
    val intValue: Int? = null,
    val boolValue: Boolean? = null,
    val doubleValue: Double? = null
)
