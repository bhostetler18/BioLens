package com.uf.automoth.ui.metadata

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.data.metadata.UserMetadataKey
import kotlinx.coroutines.flow.map

class MetadataViewModel(private val session: Session, context: Context) : ViewModel() {
    private val defaultMetadataHeader = context.getString(R.string.default_metadata_header)
    private val autoMothMetadataHeader = context.getString(R.string.automoth_metadata_header)
    private val userMetadataHeader = context.getString(R.string.user_metadata_header)

    private val defaultMetadata = getDefaultMetadata(session, context, ::onMetadataChange)
    private val autoMothMetadata = emptyList<MetadataTableDataModel>()
    private var userMetadata = mutableListOf<MetadataTableDataModel>()

    val allMetadata: LiveData<List<MetadataTableDataModel>> =
        AutoMothRepository.metadataStore.allFieldsFlow.map { fields ->
            updateUserMetadata(fields)
            return@map listOf(MetadataTableDataModel.Header(defaultMetadataHeader)) + defaultMetadata +
                listOf(MetadataTableDataModel.Header(autoMothMetadataHeader)) + autoMothMetadata +
                listOf(MetadataTableDataModel.Header(userMetadataHeader)) + userMetadata
        }.asLiveData()

    private fun isDirty(): Boolean {
        return (defaultMetadata + autoMothMetadata + userMetadata).any {
            (it as? EditableMetadataInterface)?.dirty ?: false
        }
    }

    val isDirty = MutableLiveData(false)

    private fun onMetadataChange() {
        this.isDirty.postValue(isDirty())
    }

    suspend fun saveChanges() {
        (defaultMetadata + userMetadata + autoMothMetadata).forEach {
            (it as? EditableMetadataInterface)?.let { metadata ->
                if (!metadata.readonly && metadata.dirty) {
                    metadata.writeValue()
                }
            }
        }
    }

    private suspend fun updateUserMetadata(fields: List<UserMetadataKey>) {
        // Each displayable metadata holds a temporary edited value for its field, so we can't just
        // recreate the list every time a field is added or deleted without resetting these values
        // Instead, insert or delete only the appropriate items
        val previous = userMetadata.mapNotNull { (it as? EditableMetadataInterface)?.name }.toHashSet()
        val current = fields.map { it.field }.toHashSet()
        val new = current.subtract(previous)

        userMetadata.removeIf {
            (it as? EditableMetadataInterface)?.let { metadata ->
                return@removeIf !current.contains(metadata.name)
            }
            return@removeIf false
        }

        fields.filter { new.contains(it.field) }.forEach {
            userMetadata.add(
                it.toDisplayableMetadata(
                    AutoMothRepository.metadataStore,
                    session.sessionID,
                    true,
                    observer = ::onMetadataChange
                )
            )
        }
        userMetadata.sortBy { (it as? EditableMetadataInterface)?.name }
    }

    class Factory(private val session: Session, private val context: Context) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MetadataViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MetadataViewModel(session, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
