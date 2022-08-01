package com.uf.automoth.ui.sessions.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session

class ImageGridViewModel(val session: Session) : ViewModel() {

    val allImages = AutoMothRepository.getImagesInSession(session.sessionID).asLiveData()

    class ImageGridViewModelFactory(private val session: Session) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ImageGridViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ImageGridViewModel(session) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
