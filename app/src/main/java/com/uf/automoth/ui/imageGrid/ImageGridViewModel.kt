package com.uf.automoth.ui.imageGrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class ImageGridViewModel(val session: Session) : ViewModel() {

    val skipCount = MutableStateFlow(0)
    val images =
        skipCount.combine(AutoMothRepository.getImagesInSessionFlow(session.sessionID)) { skip, images ->
            return@combine if (skip == 0) {
                images
            } else {
                val filteredCount = kotlin.math.ceil(images.size / skip.toDouble()).toInt()
                List(filteredCount) { i ->
                    images[i * skip]
                }
            }
        }.flowOn(Dispatchers.IO).asLiveData(viewModelScope.coroutineContext)
    val displayCounts = AutoMothRepository.getNumImagesInSession(session.sessionID)
        .combine(skipCount) { numImages, skip ->
            Pair(numImages, skip)
        }.asLiveData(viewModelScope.coroutineContext)

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
