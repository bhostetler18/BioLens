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

package com.uf.automoth.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ImageDAO {

    @Insert
    suspend fun insert(image: Image): Long

    @Delete
    suspend fun delete(image: Image)

    @Query("SELECT * FROM images WHERE imageID = :id")
    suspend fun getImage(id: Long): Image?

    @Query("SELECT * FROM images")
    fun getAllImages(): List<Image>
}
