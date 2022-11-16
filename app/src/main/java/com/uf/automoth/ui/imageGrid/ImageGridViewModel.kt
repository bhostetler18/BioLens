package com.uf.automoth.ui.imageGrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.ui.common.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class ImageGridViewModel(val sessionID: Long) : ViewModel() {

    var exportOptions = ExportOptions.default
    val skipCount = MutableStateFlow(0)
    val images =
        skipCount.combine(AutoMothRepository.getImagesInSessionFlow(sessionID)) { skip, images ->
            return@combine if (skip == 0) {
                images
            } else {
                val filteredCount = kotlin.math.ceil(images.size / skip.toDouble()).toInt()
                List(filteredCount) { i ->
                    images[i * skip]
                }
            }
        }.flowOn(Dispatchers.IO).asLiveData(viewModelScope.coroutineContext)
    val displayCounts = AutoMothRepository.getNumImagesInSession(sessionID)
        .combine(skipCount) { numImages, skip ->
            Pair(numImages, skip)
        }.asLiveData(viewModelScope.coroutineContext)

    val session = AutoMothRepository.getSessionFlow(sessionID).asLiveData()

    fun deleteCurrentSession() {
        AutoMothRepository.deleteSession(sessionID)
    }

    class ImageGridViewModelFactory(private val sessionID: Long) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ImageGridViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ImageGridViewModel(sessionID) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
