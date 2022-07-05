package com.uf.automoth.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDAO {

    @Insert
    suspend fun insert(image: Image): Long

    @Delete
    suspend fun delete(image: Image)

    @Query("SELECT * FROM images")
    fun getAllImages(): List<Image>
}

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
    fun getImagesInSession(id: Long): LiveData<List<Image>>

    @Transaction
    @Query("SELECT COUNT(imageID) FROM images WHERE parentSessionID = :id")
    fun getNumImagesInSession(id: Long): LiveData<Int>

    @Query("SELECT * FROM sessions WHERE sessionID = :id")
    fun getSession(id: Long): Session
}
