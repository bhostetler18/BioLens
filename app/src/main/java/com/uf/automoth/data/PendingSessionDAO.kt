package com.uf.automoth.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSessionDAO {

    @Insert
    suspend fun insert(session: PendingSession): Long

    @Delete
    suspend fun delete(session: PendingSession)

    @Query("DELETE FROM pending_sessions WHERE requestCode = :requestCode")
    suspend fun deleteByRequestCode(requestCode: Long)

    @Query("SELECT * FROM pending_sessions WHERE requestCode = :requestCode")
    suspend fun getPendingSession(requestCode: Long): PendingSession?

    @Query("SELECT * FROM pending_sessions ORDER BY datetime(scheduledDateTime)")
    suspend fun getAllPendingSessions(): List<PendingSession>

    @Query("SELECT * FROM pending_sessions ORDER BY datetime(scheduledDateTime)")
    fun getAllPendingSessionsFlow(): Flow<List<PendingSession>>

    @Query("SELECT * FROM pending_sessions ORDER BY datetime(scheduledDateTime) limit 1")
    fun getEarliestPendingSession(): Flow<PendingSession?>
}
