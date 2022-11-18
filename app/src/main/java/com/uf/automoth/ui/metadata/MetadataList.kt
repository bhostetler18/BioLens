package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.metadata.MetadataType
import com.uf.automoth.utility.indexOfOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface MetadataListObserver {
    fun onListChanged(newList: List<MetadataTableDataModel>)
    fun onDirtyChanged(isDirty: Boolean)
}

// This class provides an observable list of MetadataTableDataModel objects and concurrency-safe
// mutating operations. It does NOT synchronize access to any of the individual
// MetadataTableDataModel objects contained in the list
class MetadataList(
    context: Context,
    private val scope: CoroutineScope,
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
    private var disableMetadataObserving: Boolean = false

    private val contents
        get() = defaultMetadataHeader + defaultMetadata +
            autoMothMetadataHeader + autoMothMetadata +
            userMetadataHeader + userMetadata

    private suspend fun isDirty(): Boolean = safeAccess {
        return@safeAccess (defaultMetadata + autoMothMetadata + userMetadata).any {
            it.editable?.dirty ?: false
        }
    }

    suspend fun loadMetadata(context: Context, sessionID: Long) {
        this.sessionID = sessionID
        mutate {
            val session = AutoMothRepository.getSession(sessionID) ?: return@mutate
            defaultMetadata = getDefaultMetadata(session, context, ::onMetadataChange)
            autoMothMetadata = getAutoMothMetadata(
                sessionID,
                AutoMothRepository.metadataStore,
                ::onMetadataChange,
                context
            )
            userMetadata = getUserMetadata(
                sessionID,
                AutoMothRepository.metadataStore,
                ::onMetadataChange
            ).toMutableList()
        }
    }

    private fun onMetadataChange() {
        if (disableMetadataObserving) {
            return
        }
        scope.launch {
            observer.onDirtyChanged(isDirty())
        }
    }

    suspend fun addUserField(name: String, type: MetadataType): Int? {
        if (AutoMothRepository.metadataStore.getField(name) != null) {
            return null
        }
        AutoMothRepository.metadataStore.addMetadataField(name, type)
        AutoMothRepository.metadataStore.getField(name)?.let { field ->
            val displayable = field.toDisplayableMetadata(
                sessionID,
                AutoMothRepository.metadataStore,
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
        onMetadataChange() // update isDirty in case the only dirty item was the one that was removed
        return false
    }

    // This is a mutating function because writeValue() modifies EditableMetadataInterface::originalValue,
    // and we want any observers (e.g. the RecyclerView that stores originalValue for use with its
    // EditText) to get a new list and update accordingly
    suspend fun saveChanges() {
        mutate {
            // The metadata value change observer needs to be disabled temporarily since the mutex is
            // non-reentrant and metadata.writeValue() will invoke the onMetadataChange callback,
            // causing deadlock because we're in a scope that has already acquired the lock and
            // isDirty() also acquires a lock inside onMetadataChange.
            // A better solution could probably be implemented at some point, but the current solution is
            // also desirable because we don't want a ton of back-to-back calls to onMetadataChange()
            // while saving anyway
            withMetadataObserverDisabled {
                (defaultMetadata + userMetadata + autoMothMetadata).forEach {
                    it.editable?.let { metadata ->
                        if (!metadata.readonly && metadata.dirty) {
                            metadata.writeValue()
                        }
                    }
                }
            }
        }
        onMetadataChange() // Just call once when done updating
    }

    private suspend fun <T> safeAccess(block: suspend MetadataList.() -> T): T {
        return mutex.withLock(this) { block(this) }
    }

    private suspend fun <T> mutate(block: suspend MetadataList.() -> T): T {
        val ret = safeAccess(block)
        observer.onListChanged(contents)
        return ret
    }

    private suspend fun <T> withMetadataObserverDisabled(block: suspend () -> T): T {
        disableMetadataObserving = true
        val ret = block()
        disableMetadataObserving = false
        return ret
    }
}
