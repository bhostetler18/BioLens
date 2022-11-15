package com.uf.automoth.ui.metadata

import android.content.Context
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

    private val defaultMetadata = getDefaultMetadata(session, context)
    private val autoMothMetadata = emptyList<DisplayableMetadata>()
    private var userMetadata = mutableListOf<DisplayableMetadata>()

    val allMetadata = AutoMothRepository.metadataStore.allFieldsFlow.map { fields ->
        updateUserMetadata(fields)
        return@map listOf(DisplayableMetadata.Header(defaultMetadataHeader)) + defaultMetadata +
            listOf(DisplayableMetadata.Header(autoMothMetadataHeader)) + autoMothMetadata +
            listOf(DisplayableMetadata.Header(userMetadataHeader, true)) + userMetadata
    }.asLiveData()

    private suspend fun updateUserMetadata(fields: List<UserMetadataKey>) {
        // Each displayable metadata holds a temporary edited value for its field, so we can't just
        // recreate the list every time a field is added or deleted without resetting these values
        // Instead, insert or delete only the appropriate items
        val previous = userMetadata.map { it.name }.toHashSet()
        val current = fields.map { it.field }.toHashSet()
        val new = current.subtract(previous)

        userMetadata.removeIf { !current.contains(it.name) }

        fields.filter { new.contains(it.field) }.forEach {
            userMetadata.add(
                it.toDisplayableMetadata(
                    AutoMothRepository.metadataStore,
                    session.sessionID,
                    true
                )
            )
        }
        userMetadata.sortBy { it.name }
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
