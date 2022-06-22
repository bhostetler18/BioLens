package com.uf.automoth.data

import androidx.room.*

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
    fun getAllSessions(): List<SessionWithImages>

    @Transaction
    @Query("SELECT * FROM sessions")
    fun getSessionsWithImages(): List<SessionWithImages>

    @Transaction
    @Query("SELECT * FROM images WHERE parentSessionID = :id")
    fun getImagesInSession(id: Long): List<Image>
}
