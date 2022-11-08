package com.uf.automoth.data

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
    fun getSessionsWithImages(): List<SessionWithImages>

    @Transaction
    @Query("SELECT * FROM images WHERE parentSessionID = :id")
    fun getImagesInSessionFlow(id: Long): Flow<List<Image>>

    @Transaction
    @Query("SELECT * FROM images WHERE parentSessionID = :id")
    suspend fun getImagesInSession(id: Long): List<Image>

    @Transaction
    @Query("SELECT COUNT(imageID) FROM images WHERE parentSessionID = :id")
    fun getNumImagesInSession(id: Long): Flow<Int>

    @Query("SELECT * FROM sessions WHERE sessionID = :id")
    suspend fun getSession(id: Long): Session?

    @Query("UPDATE sessions SET latitude = :latitude, longitude = :longitude WHERE sessionID = :id")
    fun updateSessionLocation(id: Long, latitude: Double?, longitude: Double?)

    @Query("UPDATE sessions SET completed = :time WHERE sessionID = :id")
    fun updateSessionCompletion(id: Long, time: OffsetDateTime)

    @Query("UPDATE sessions SET name = :name WHERE sessionID = :id")
    fun renameSession(id: Long, name: String)
}
