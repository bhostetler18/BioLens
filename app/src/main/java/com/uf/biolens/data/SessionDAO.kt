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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime

@Dao
interface SessionDAO {

    @Insert
    suspend fun insert(session: Session): Long

    @Delete
    suspend fun delete(session: Session)

    // See https://medium.com/androiddevelopers/room-time-2b4cf9672b98
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY datetime(started)")
    fun getAllSessions(): Flow<List<Session>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY datetime(started)")
    suspend fun getSessionsWithImages(): List<SessionWithImages>

    @Transaction
    @Query("SELECT * FROM images WHERE parentSessionID = :id")
    fun getImagesInSessionFlow(id: Long): Flow<List<Image>>

    @Transaction
    @Query("SELECT * FROM images WHERE parentSessionID = :id ORDER BY `index`")
    suspend fun getImagesInSession(id: Long): List<Image>

    @Transaction
    @Query("SELECT COUNT(imageID) FROM images WHERE parentSessionID = :id")
    fun getNumImagesInSession(id: Long): Flow<Int>

    // It would be nice to just use "SELECT MAX(datetime(timestamp)) FROM images WHERE parentSessionID = :id"
    // but the resulting sqlite datetime is not converted into an OffsetDateTime...
    @Query(
        "SELECT timestamp FROM images WHERE parentSessionID = :id and datetime(timestamp) = " +
            "(SELECT MAX(datetime(timestamp)) FROM images WHERE parentSessionID = :id) LIMIT 1;"
    )
    suspend fun getLastImageTimestampInSession(id: Long): OffsetDateTime?

    @Query("SELECT * FROM sessions WHERE sessionID = :id")
    suspend fun getSession(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE sessionID = :id")
    fun getSessionFlow(id: Long): Flow<Session?>

    @Query("UPDATE sessions SET latitude = :latitude, longitude = :longitude WHERE sessionID = :id")
    suspend fun updateSessionLocation(id: Long, latitude: Double?, longitude: Double?)

    @Query("UPDATE sessions SET completed = :time WHERE sessionID = :id")
    suspend fun updateSessionCompletion(id: Long, time: OffsetDateTime)

    @Query("UPDATE sessions SET name = :name WHERE sessionID = :id")
    suspend fun renameSession(id: Long, name: String)
}
