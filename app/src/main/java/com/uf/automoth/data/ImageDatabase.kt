package com.uf.automoth.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Image::class, Session::class], version = 1)
@TypeConverters(AutoMothTypeConverters::class)
abstract class ImageDatabase : RoomDatabase() {

    abstract fun imageDAO(): ImageDAO
    abstract fun sessionDAO(): SessionDAO


}