package com.uf.automoth

import android.app.Application
import com.uf.automoth.data.AutoMothRepository

class AutoMothApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AutoMothRepository(this)
    }
}
