/*
 * Copyright (c) 2022-2023 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.biolens.ui.imageGrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.ui.common.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class ImageGridViewModel(val sessionID: Long) : ViewModel() {

    var exportOptions = ExportOptions.default
    val imageSelector = BioLensImageSelector()

    val skipCount = MutableStateFlow(0)
    val images =
        skipCount.combine(BioLensRepository.getImagesInSessionFlow(sessionID)) { skip, images ->
            return@combine if (skip == 0) {
                images
            } else {
                val filteredCount = kotlin.math.ceil(images.size / skip.toDouble()).toInt()
                List(filteredCount) { i ->
                    images[i * skip]
                }
            }
        }.flowOn(Dispatchers.IO).asLiveData(viewModelScope.coroutineContext)

    val displayCounts = combine(
        BioLensRepository.getNumImagesInSession(sessionID),
        skipCount,
        imageSelector.numSelected.asFlow()
    ) { numImages, skipCount, numSelected ->
        ImageDisplayData(numImages, skipCount, numSelected)
    }.asLiveData(viewModelScope.coroutineContext)

    val session = BioLensRepository.getSessionFlow(sessionID).asLiveData()

    fun deleteCurrentSession() {
        BioLensRepository.deleteSession(sessionID)
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
