package com.uf.automoth

import android.app.Application
import androidx.room.Room
import com.uf.automoth.data.ImageDatabase

class AutoMothApplication : Application() {

    companion object {
        var database: ImageDatabase? = null
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this,
                                        ImageDatabase::class.java,
                                        "image-db").build()
    }
}