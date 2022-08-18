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
