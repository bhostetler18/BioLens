package com.uf.automoth.ui.metadata

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uf.automoth.data.metadata.UserMetadataType
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
    suspend fun addUserField(name: String, type: UserMetadataType) =
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
