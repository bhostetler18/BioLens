package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.utility.indexOfOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface MetadataListObserver {
    fun onListChanged(newList: List<MetadataTableDataModel>)
    fun onMetadataValueChanged()
}

// This class provides an observable list of MetadataTableDataModel objects and concurrency-safe
// mutating operations. It does NOT synchronize access to any of the individual
// MetadataTableDataModel objects contained in the list
class MetadataList(
    context: Context,
    private val observer: MetadataListObserver
) {
    private var sessionID: Long = -1
    private val defaultMetadataHeader =
        listOf(MetadataTableDataModel.Header(context.getString(R.string.default_metadata_header)))
    private val autoMothMetadataHeader =
        listOf(MetadataTableDataModel.Header(context.getString(R.string.automoth_metadata_header)))
    private val userMetadataHeader =
        listOf(MetadataTableDataModel.Header(context.getString(R.string.user_metadata_header)))

    private var defaultMetadata = listOf<MetadataTableDataModel>()
    private var autoMothMetadata = listOf<MetadataTableDataModel>()
    private var userMetadata = mutableListOf<MetadataTableDataModel>()
    private val mutex = Mutex()

    private val contents
        get() = defaultMetadataHeader + defaultMetadata +
            autoMothMetadataHeader + autoMothMetadata +
            userMetadataHeader + userMetadata

    suspend fun isDirty(): Boolean = safeAccess {
        return@safeAccess (defaultMetadata + autoMothMetadata + userMetadata).any {
            it.editable?.dirty ?: false
        }
    }

    suspend fun loadMetadata(context: Context, sessionID: Long) {
        this.sessionID = sessionID
        mutate {
            val session = AutoMothRepository.getSession(sessionID) ?: return@mutate
            defaultMetadata = getDefaultMetadata(session, context, ::onMetadataChange)
//            autoMothMetadata = // TODO
            userMetadata = getUserMetadata(
                sessionID,
                AutoMothRepository.metadataStore,
                ::onMetadataChange
            ).toMutableList()
        }
    }

    private fun onMetadataChange() {
        observer.onMetadataValueChanged()
    }

    suspend fun addUserField(name: String, type: UserMetadataType): Int? {
        if (AutoMothRepository.metadataStore.getField(name) != null) {
            return null
        }
        AutoMothRepository.metadataStore.addMetadataField(name, type)
        AutoMothRepository.metadataStore.getField(name)?.let { field ->
            val displayable = field.toDisplayableMetadata(
                AutoMothRepository.metadataStore,
                sessionID,
                observer = ::onMetadataChange
            )
            return@addUserField mutate {
                userMetadata.add(displayable)
                userMetadata.sortBy { it.editable?.name }
                return@mutate contents.indexOfOrNull(displayable)
            }
        }
        return null
    }

    suspend fun removeUserField(item: MetadataTableDataModel): Boolean {
        val metadata = item.editable?.userField ?: return false
        if (mutate { return@mutate userMetadata.remove(item) }) {
            AutoMothRepository.metadataStore.deleteMetadataField(metadata.field)
        }
        return false
    }

    // This is a mutating function because writeValue() modifies EditableMetadataInterface::originalValue,
    // and we want any observers (e.g. a RecyclerView) to update accordingly
    suspend fun saveChanges() = mutate {
        (defaultMetadata + userMetadata + autoMothMetadata).forEach {
            it.editable?.let { metadata ->
                if (!metadata.readonly && metadata.dirty) {
                    metadata.writeValue()
                }
            }
        }
    }

    private suspend fun <T> safeAccess(block: suspend MetadataList.() -> T): T {
        return mutex.withLock(this) { block(this) }
    }

    private suspend fun <T> mutate(block: suspend MetadataList.() -> T): T {
        val ret = safeAccess(block)
        observer.onListChanged(contents)
        onMetadataChange()
        return ret
    }
}
