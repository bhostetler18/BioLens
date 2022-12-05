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

package com.uf.biolens.data.metadata

import android.util.Log

enum class MetadataType(val raw: String) {
    STRING("string"), INT("int"), DOUBLE("double"), BOOLEAN("boolean");

    companion object {
        fun from(raw: String): MetadataType? {
            return when (raw) {
                "string" -> STRING
                "int" -> INT
                "double" -> DOUBLE
                "boolean" -> BOOLEAN
                else -> null
            }
        }
    }
}

interface MetadataField {
    val name: String
    val type: MetadataType
    val builtin: Boolean
}

// Makes it easier to mock/test or potentially change backing storage in the future
interface MetadataStore {
    suspend fun addMetadataField(name: String, type: MetadataType, builtin: Boolean)
    suspend fun deleteMetadataField(name: String, builtin: Boolean)
    suspend fun renameField(originalName: String, newName: String, builtin: Boolean)
    suspend fun getField(name: String): MetadataField?
    suspend fun getMetadataType(name: String): MetadataType?
    suspend fun getFields(builtin: Boolean): List<MetadataField>
    suspend fun getAllFields(): List<MetadataField>

    // It would be nice to do this with generics, but type erasure and issues with virtual inline/reified
    // functions make that more trouble than it's worth. At least this parallels the "put/get"
    // methods of Android SharedPreferences and Bundles, and the convenience extension methods below
    // mean they don't need to be used often
    suspend fun getString(name: String, session: Long): String?
    suspend fun setString(name: String, session: Long, value: String?)
    suspend fun getInt(name: String, session: Long): Int?
    suspend fun setInt(name: String, session: Long, value: Int?)
    suspend fun getDouble(name: String, session: Long): Double?
    suspend fun setDouble(name: String, session: Long, value: Double?)
    suspend fun getBoolean(name: String, session: Long): Boolean?
    suspend fun setBoolean(name: String, session: Long, value: Boolean?)
}

// Convenience generic functions
suspend inline fun <reified T> MetadataStore.setValue(name: String, session: Long, value: T) {
    when (value) {
        is String -> setString(name, session, value)
        is Int -> setInt(name, session, value)
        is Double -> setDouble(name, session, value)
        is Boolean -> setBoolean(name, session, value)
        else -> Log.e("", "Tried to set invalid metadata type ${T::class.simpleName}")
    }
}

inline fun <reified T> reifyMetadataType(): MetadataType? {
    return when (T::class) {
        String::class -> MetadataType.STRING
        Int::class -> MetadataType.INT
        Double::class -> MetadataType.DOUBLE
        Boolean::class -> MetadataType.BOOLEAN
        else -> null
    }
}

suspend inline fun <reified T> MetadataStore.getValue(name: String, session: Long): T? {
    val type = reifyMetadataType<T>()
    return if (type != null) {
        getValue(name, type, session) as? T?
    } else {
        Log.e("", "Tried to get invalid metadata type ${T::class.simpleName}")
        null
    }
}

suspend fun MetadataStore.getValue(name: String, type: MetadataType, session: Long): Any? {
    return when (type) {
        MetadataType.STRING -> getString(name, session)
        MetadataType.INT -> getInt(name, session)
        MetadataType.DOUBLE -> getDouble(name, session)
        MetadataType.BOOLEAN -> getBoolean(name, session)
    }
}

suspend fun MetadataStore.getValue(field: MetadataField, session: Long): Any? {
    return getValue(field.name, field.type, session)
}
