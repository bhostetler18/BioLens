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

package com.uf.biolens.ui.metadata

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uf.biolens.data.metadata.MetadataType
import kotlinx.coroutines.launch

class MetadataViewModel(
    sessionID: Long,
    context: Context
) : ViewModel(), MetadataListObserver {

    private val metadataList = MetadataList(context, viewModelScope, this)

    private val _allMetadata: MutableLiveData<List<MetadataTableDataModel>> =
        MutableLiveData(emptyList())
    val allMetadata: LiveData<List<MetadataTableDataModel>> get() = _allMetadata

    private val _isDirty = MutableLiveData(false)
    val isDirty: LiveData<Boolean> get() = _isDirty

    init {
        viewModelScope.launch {
            metadataList.loadMetadata(context, sessionID)
        }
    }

    override fun onListChanged(newList: List<MetadataTableDataModel>) {
        _allMetadata.postValue(newList)
    }

    override fun onDirtyChanged(isDirty: Boolean) {
        _isDirty.postValue(isDirty)
    }

    suspend fun saveChanges() = metadataList.saveChanges()
    suspend fun addUserField(name: String, type: MetadataType) =
        metadataList.addUserField(name, type)

    suspend fun removeUserField(item: MetadataTableDataModel) = metadataList.removeUserField(item)

    class Factory(private val sessionID: Long, private val context: Context) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MetadataViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MetadataViewModel(sessionID, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
