package com.uf.automoth.data.metadata

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMetadataKeyDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(key: UserMetadataKey)

    @Delete
    suspend fun delete(key: UserMetadataKey)

    @Query("DELETE FROM metadata_keys WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM metadata_keys WHERE `key` = :key")
    suspend fun get(key: String): UserMetadataKey?

    @Query("SELECT * FROM metadata_keys")
    suspend fun getAllKeys(): List<UserMetadataKey>

    @Query("SELECT * FROM metadata_keys")
    fun getAllKeysFlow(): Flow<List<UserMetadataKey>>

    @Query("SELECT type FROM metadata_keys where `key` = :key")
    suspend fun getType(key: String): UserMetadataType?
}
