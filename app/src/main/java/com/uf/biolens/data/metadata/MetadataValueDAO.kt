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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MetadataValueDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: MetadataValue)

    @Query("SELECT stringValue FROM metadata_values WHERE name = :name AND sessionID = :session")
    suspend fun getString(name: String, session: Long): String?

    @Query("SELECT intValue FROM metadata_values WHERE name = :name AND sessionID = :session")
    suspend fun getInt(name: String, session: Long): Int?

    @Query("SELECT doubleValue FROM metadata_values WHERE name = :name AND sessionID = :session")
    suspend fun getDouble(name: String, session: Long): Double?

    @Query("SELECT boolValue FROM metadata_values WHERE name = :name AND sessionID = :session")
    suspend fun getBoolean(name: String, session: Long): Boolean?

    // This is dangerous and should only be called if you're sure the new key won't produce a foreign
    // key constraint conflict
    @Query("UPDATE metadata_values SET name = :newName where name = :originalName")
    suspend fun rename(originalName: String, newName: String)
}
