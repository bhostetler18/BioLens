package com.uf.automoth

import android.app.Application
import com.uf.automoth.data.AutoMothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AutoMothApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        mount()
    }

    fun mount(): Boolean {
        this.getExternalFilesDir(null)?.let {
            AutoMothRepository(this, it, applicationScope)
            return true
        }
        return false
    }
}
