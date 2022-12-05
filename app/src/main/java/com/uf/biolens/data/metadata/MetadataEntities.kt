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

package com.uf.biolens.data.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.uf.biolens.data.Session

@Entity(tableName = "metadata_keys")
data class MetadataKey(
    @PrimaryKey override val name: String,
    override val type: MetadataType,
    override val builtin: Boolean
) : MetadataField

@Entity(
    tableName = "metadata_values",
    primaryKeys = ["name", "sessionID"],
    foreignKeys = [
        ForeignKey(
            entity = MetadataKey::class,
            parentColumns = ["name"],
            childColumns = ["name"],
            onDelete = ForeignKey.CASCADE
//            onUpdate = ForeignKey.CASCADE
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
    val name: String,
    @ColumnInfo(index = true) val sessionID: Long,
    val stringValue: String? = null,
    val intValue: Int? = null,
    val boolValue: Boolean? = null,
    val doubleValue: Double? = null
)
