package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.utility.SHORT_DATE_TIME_FORMATTER
import java.time.OffsetDateTime

interface DisplayableMetadataInterface {
    val name: String
    val readonly: Boolean
    val dirty: Boolean
    var deletable: Boolean
    suspend fun writeValue()

    // Should return null when the absence of a value is significant and should be displayed as "unknown"
    fun stringRepresentation(context: Context): String?
}

interface MetadataValueInterface<T> {
    var originalValue: T?
    var currentValue: T?
    val saveValue: suspend (T?) -> Unit
    val validate: (T?) -> Boolean
}

// Allows creating a List<Metadata> with heterogeneous contents in a type-safe manner
// This looks unnecessary, but it also allows polymorphism as opposed to template types that would
// be erased at runtime. Since there are relatively few types used, it seems like an okay compromise
// especially since it allows limiting the Metadata types to those that can actually be displayed
sealed class DisplayableMetadata : DisplayableMetadataInterface {
    override var deletable = false
    override val dirty = false

    class StringMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: String?,
        override val saveValue: suspend (String?) -> Unit = {},
        override val validate: (String?) -> Boolean = { true }
    ) : DisplayableMetadata(), DisplayableMetadataInterface, MetadataValueInterface<String> {
        override var currentValue = originalValue
        override suspend fun writeValue() {
            saveValue(currentValue)
            originalValue = currentValue
        }

        override fun stringRepresentation(context: Context) = currentValue
        override val dirty: Boolean
            get() {
                return currentValue != originalValue
            }
    }

    class IntMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Int?,
        override val saveValue: suspend (Int?) -> Unit = {},
        override val validate: (Int?) -> Boolean = { true }
    ) : DisplayableMetadata(), DisplayableMetadataInterface, MetadataValueInterface<Int> {
        override var currentValue = originalValue
        override suspend fun writeValue() {
            saveValue(currentValue)
            originalValue = currentValue
        }

        override fun stringRepresentation(context: Context): String? = currentValue?.toString()
        override val dirty: Boolean
            get() {
                return currentValue != originalValue
            }
    }

    class DoubleMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Double?,
        override val saveValue: suspend (Double?) -> Unit = {},
        override val validate: (Double?) -> Boolean = { true }
    ) : DisplayableMetadata(), DisplayableMetadataInterface, MetadataValueInterface<Double> {
        override var currentValue = originalValue
        override suspend fun writeValue() {
            saveValue(currentValue)
            originalValue = currentValue
        }

        override fun stringRepresentation(context: Context): String? = currentValue?.toString()
        override val dirty: Boolean
            get() {
                return currentValue != originalValue
            }
    }

    class BooleanMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Boolean?,
        override val saveValue: suspend (Boolean?) -> Unit = {}
    ) : DisplayableMetadata(), DisplayableMetadataInterface, MetadataValueInterface<Boolean> {
        override var currentValue = originalValue
        override suspend fun writeValue() {
            saveValue(currentValue)
            originalValue = currentValue
        }

        override fun stringRepresentation(context: Context): String? {
            return when (currentValue) {
                true -> context.getString(R.string.yes)
                false -> context.getString(R.string.no)
                null -> null
            }
        }

        override val validate: (Boolean?) -> Boolean = { true }
        override val dirty: Boolean
            get() {
                return currentValue != originalValue
            }
    }

    class DateMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: OffsetDateTime?,
        override val saveValue: suspend (OffsetDateTime?) -> Unit = {},
        override val validate: (OffsetDateTime?) -> Boolean = { true }
    ) : DisplayableMetadata(),
        DisplayableMetadataInterface,
        MetadataValueInterface<OffsetDateTime> {
        override var currentValue = originalValue
        override suspend fun writeValue() {
            saveValue(currentValue)
            originalValue = currentValue
        }

        override fun stringRepresentation(context: Context) =
            currentValue?.format(SHORT_DATE_TIME_FORMATTER)

        override val dirty: Boolean
            get() {
                return currentValue != originalValue
            }
    }

    class Header(override val name: String) : DisplayableMetadata() {
        override val readonly = true
        override suspend fun writeValue() {}
        override fun stringRepresentation(context: Context): String = ""
    }
}
