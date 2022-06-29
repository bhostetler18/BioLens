package com.uf.automoth

import android.app.Application
import com.uf.automoth.data.AutoMothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AutoMothApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        AutoMothRepository(this, applicationScope)
    }
}
