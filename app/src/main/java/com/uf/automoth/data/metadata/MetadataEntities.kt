package com.uf.automoth.data.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.uf.automoth.data.Session

@Entity(tableName = "metadata_keys")
data class MetadataKey(
    @PrimaryKey val key: String,
    override val type: MetadataType,
    // see https://stackoverflow.com/questions/70574648/how-to-set-a-default-value-to-existing-rows-for-a-new-column-created-using-room
    @ColumnInfo(defaultValue = "0") override val builtin: Boolean
) : MetadataField {
    @Ignore
    override val field: String = key // match naming convention of UserMetadataField interface
}

@Entity(
    tableName = "metadata_values",
    primaryKeys = ["key", "sessionID"],
    foreignKeys = [
        ForeignKey(
            entity = MetadataKey::class,
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
data class MetadataValue(
    val key: String,
    @ColumnInfo(index = true) val sessionID: Long,
    val stringValue: String? = null,
    val intValue: Int? = null,
    val boolValue: Boolean? = null,
    val doubleValue: Double? = null
)
