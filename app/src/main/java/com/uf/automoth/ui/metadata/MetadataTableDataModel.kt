package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.utility.SHORT_DATE_TIME_FORMATTER
import java.time.OffsetDateTime

typealias MetadataChangeObserver = (() -> Unit)?

interface EditableMetadataInterface {
    val name: String
    val readonly: Boolean
    var deletable: Boolean
    val dirty: Boolean
    suspend fun writeValue()

    // Should return null when the absence of a value is significant and should be displayed as "unknown"
    fun stringRepresentation(context: Context): String?
}

interface MetadataValueInterface<T> {
    var originalValue: T?
    var currentValue: T?
    val saveValue: suspend (T?) -> Unit
    val validate: (T?) -> Boolean
    var observer: MetadataChangeObserver
}

fun <T> MetadataValueInterface<T>.isDirty(): Boolean {
    return currentValue != originalValue
}

fun <T> MetadataValueInterface<T>.setValue(newValue: T?) {
    if (newValue != currentValue) {
        this.currentValue = newValue
        observer?.invoke()
    }
}

suspend fun <T> MetadataValueInterface<T>.saveValue() {
    saveValue(currentValue)
    originalValue = currentValue // so no longer dirty
    observer?.invoke()
}

// Allows creating a List<Metadata> with heterogeneous contents in a type-safe manner
// This looks unnecessary, but it also allows polymorphism as opposed to template types that would
// be erased at runtime. Since there are relatively few types used, it seems like an okay compromise
// especially since it allows limiting the Metadata types to those that can actually be displayed
sealed class MetadataTableDataModel {
    class StringMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: String?,
        override val saveValue: suspend (String?) -> Unit = {},
        override val validate: (String?) -> Boolean = { true }
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<String> {
        override var currentValue = originalValue
        override var deletable: Boolean = false
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context) = currentValue
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
    }

    class IntMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Int?,
        override val saveValue: suspend (Int?) -> Unit = {},
        override val validate: (Int?) -> Boolean = { true }
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<Int> {
        override var currentValue = originalValue
        override var deletable: Boolean = false
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context): String? = currentValue?.toString()
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
    }

    class DoubleMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Double?,
        override val saveValue: suspend (Double?) -> Unit = {},
        override val validate: (Double?) -> Boolean = { true }
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<Double> {
        override var currentValue = originalValue
        override var deletable: Boolean = false
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context): String? = currentValue?.toString()
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
    }

    class BooleanMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Boolean?,
        override val saveValue: suspend (Boolean?) -> Unit = {}
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<Boolean> {
        override var currentValue = originalValue
        override var deletable: Boolean = false
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context): String? {
            return when (currentValue) {
                true -> context.getString(R.string.yes)
                false -> context.getString(R.string.no)
                null -> null
            }
        }
        override suspend fun writeValue() = saveValue()
        override val validate: (Boolean?) -> Boolean = { true }
        override var observer: MetadataChangeObserver = null
    }

    class DateMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: OffsetDateTime?,
        override val saveValue: suspend (OffsetDateTime?) -> Unit = {},
        override val validate: (OffsetDateTime?) -> Boolean = { true }
    ) : MetadataTableDataModel(),
        EditableMetadataInterface,
        MetadataValueInterface<OffsetDateTime> {
        override var currentValue = originalValue
        override var deletable: Boolean = false
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context) =
            currentValue?.format(SHORT_DATE_TIME_FORMATTER)
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
    }

    class Header(val name: String) : MetadataTableDataModel()
}
