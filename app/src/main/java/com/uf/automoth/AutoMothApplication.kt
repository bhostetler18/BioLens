package com.uf.automoth

import android.app.Application
import androidx.room.Room
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.ImageDatabase

class AutoMothApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AutoMothRepository(this)
    }
}