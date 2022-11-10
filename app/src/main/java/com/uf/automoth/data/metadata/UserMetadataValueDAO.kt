package com.uf.automoth.data.metadata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserMetadataValueDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: UserMetadataValue)

    @Query("SELECT stringValue FROM metadata_values WHERE `key` = :key AND sessionID = :session")
    suspend fun getString(key: String, session: Long): String?

    @Query("SELECT intValue FROM metadata_values WHERE `key` = :key AND sessionID = :session")
    suspend fun getInt(key: String, session: Long): Int?

    @Query("SELECT doubleValue FROM metadata_values WHERE `key` = :key AND sessionID = :session")
    suspend fun getDouble(key: String, session: Long): Double?

    @Query("SELECT boolValue FROM metadata_values WHERE `key` = :key AND sessionID = :session")
    suspend fun getBoolean(key: String, session: Long): Boolean?
}
