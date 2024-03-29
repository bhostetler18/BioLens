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

package com.uf.biolens.data.metadata

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataKeyDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(key: MetadataKey)

    @Delete
    suspend fun delete(key: MetadataKey)

    @Query("DELETE FROM metadata_keys WHERE name = :name AND builtin = :builtin")
    suspend fun delete(name: String, builtin: Boolean)

    @Query("SELECT * FROM metadata_keys WHERE name = :name")
    suspend fun get(name: String): MetadataKey?

    @Query("SELECT * FROM metadata_keys WHERE builtin=:builtin ORDER BY name")
    suspend fun getKeys(builtin: Boolean): List<MetadataKey>

    @Query("SELECT * FROM metadata_keys ORDER BY name")
    suspend fun getAllKeys(): List<MetadataKey>

    @Query("SELECT * FROM metadata_keys ORDER BY name")
    fun getAllKeysFlow(): Flow<List<MetadataKey>>

    @Query("SELECT type FROM metadata_keys where name = :name")
    suspend fun getType(name: String): MetadataType?

    @Query("UPDATE metadata_values SET name = :newName where name = :originalName")
    suspend fun rename(originalName: String, newName: String)
}
