package com.uf.automoth.data.metadata

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

    @Query("DELETE FROM metadata_keys WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM metadata_keys WHERE `key` = :key")
    suspend fun get(key: String): MetadataKey?

    @Query("SELECT * FROM metadata_keys WHERE builtin=:builtin ORDER BY `key`")
    suspend fun getKeys(builtin: Boolean): List<MetadataKey>

    @Query("SELECT * FROM metadata_keys ORDER BY `key`")
    suspend fun getAllKeys(): List<MetadataKey>

    @Query("SELECT * FROM metadata_keys ORDER BY `key`")
    fun getAllKeysFlow(): Flow<List<MetadataKey>>

    @Query("SELECT type FROM metadata_keys where `key` = :key")
    suspend fun getType(key: String): MetadataType?
}
