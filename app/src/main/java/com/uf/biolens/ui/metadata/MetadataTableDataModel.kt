/*
 * Copyright (c) 2022 University of Florida
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
import com.uf.automoth.R
import com.uf.biolens.data.metadata.MetadataField
import com.uf.biolens.utility.SHORT_DATE_TIME_FORMATTER
import java.time.OffsetDateTime

typealias MetadataChangeObserver = (() -> Unit)?

interface EditableMetadataInterface {
    val name: String
    val readonly: Boolean
    val dirty: Boolean
    var userField: MetadataField?
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

fun EditableMetadataInterface.isDeletable(): Boolean {
    return userField != null && userField?.builtin == false
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
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context) = currentValue
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
        override var userField: MetadataField? = null
    }

    class IntMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Int?,
        override val saveValue: suspend (Int?) -> Unit = {},
        override val validate: (Int?) -> Boolean = { true }
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<Int> {
        override var currentValue = originalValue
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context): String? = currentValue?.toString()
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
        override var userField: MetadataField? = null
    }

    class DoubleMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Double?,
        override val saveValue: suspend (Double?) -> Unit = {},
        override val validate: (Double?) -> Boolean = { true }
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<Double> {
        override var currentValue = originalValue
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context): String? = currentValue?.toString()
        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
        override var userField: MetadataField? = null
    }

    class BooleanMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var originalValue: Boolean?,
        override val saveValue: suspend (Boolean?) -> Unit = {}
    ) : MetadataTableDataModel(), EditableMetadataInterface, MetadataValueInterface<Boolean> {
        override var currentValue = originalValue
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
        override var userField: MetadataField? = null
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
        override val dirty get() = isDirty()
        override fun stringRepresentation(context: Context) =
            currentValue?.format(SHORT_DATE_TIME_FORMATTER)

        override suspend fun writeValue() = saveValue()
        override var observer: MetadataChangeObserver = null
        override var userField: MetadataField? = null
    }

    class Header(val name: String) : MetadataTableDataModel()
}

val MetadataTableDataModel.editable: EditableMetadataInterface?
    get() = this as? EditableMetadataInterface
