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

package com.uf.biolens.data.metadata

import android.util.Log
import com.uf.biolens.data.BioLensDatabase

class BioLensMetadataStore(private val db: BioLensDatabase) : MetadataStore {

    private suspend inline fun <reified T> safeSet(name: String, set: () -> Unit) {
        val entry = db.metadataKeyDAO().get(name)
        if (entry != null) {
            val requiredType = entry.type
            val actualType = reifyMetadataType<T>()
            if (actualType == requiredType) {
                set()
            } else {
                Log.e(
                    TAG,
                    "Tried to write value of type $actualType to field '$name' of type $requiredType"
                )
            }
        } else {
            Log.e(TAG, "Cannot write to nonexistent field '$name'")
        }
    }

    private suspend inline fun <reified T> safeGet(
        name: String,
        get: () -> T?
    ): T? {
        val entry = db.metadataKeyDAO().get(name)
        return if (entry != null) {
            val desiredType = reifyMetadataType<T>()
            if (entry.type == desiredType) {
                get()
            } else {
                Log.e(
                    TAG,
                    "Tried to get $desiredType from metadata field '$name' of type ${entry.type}"
                )
                null
            }
        } else {
            Log.e(TAG, "Tried to get nonexistent metadata field $name")
            null
        }
    }

    override suspend fun addMetadataField(name: String, type: MetadataType, builtin: Boolean) {
        db.metadataKeyDAO().insert(MetadataKey(name, type, builtin))
    }

    override suspend fun deleteMetadataField(name: String, builtin: Boolean) {
        db.metadataKeyDAO().delete(name, builtin)
    }

    override suspend fun renameField(originalName: String, newName: String, builtin: Boolean) {
        val existingOriginal = db.metadataKeyDAO().get(originalName) ?: return
        if (existingOriginal.builtin != builtin) {
            Log.d(
                TAG,
                "Attempting to rename field '$originalName' with builtin='$builtin', " +
                    "but the existing field has builtin='${existingOriginal.builtin}'"
            )
            return
        }
        if (db.metadataKeyDAO().get(newName) != null) {
            Log.d(
                TAG,
                "Attempting to rename '$originalName' to '$newName', but '$newName' already exists"
            )
            return
        }
        // Insert first to prevent foreign key constraint conflicts (and avoid losing values that
        // would be wiped if the original key was deleted first)
        db.metadataKeyDAO().insert(MetadataKey(newName, existingOriginal.type, builtin))
        db.metadataValueDAO().rename(originalName, newName)
        // Delete now that values have been updated
        db.metadataKeyDAO().delete(existingOriginal)
    }

    override suspend fun getField(name: String): MetadataField? {
        return db.metadataKeyDAO().get(name)
    }

    override suspend fun getMetadataType(name: String): MetadataType? {
        return db.metadataKeyDAO().getType(name)
    }

    override suspend fun getFields(builtin: Boolean): List<MetadataField> {
        return db.metadataKeyDAO().getKeys(builtin)
    }

    override suspend fun getString(name: String, session: Long): String? = safeGet(name) {
        db.metadataValueDAO().getString(name, session)
    }

    override suspend fun setString(name: String, session: Long, value: String?) =
        safeSet<String>(name) {
            db.metadataValueDAO().insert(MetadataValue(name, session, stringValue = value))
        }

    override suspend fun getInt(name: String, session: Long): Int? = safeGet(name) {
        return db.metadataValueDAO().getInt(name, session)
    }

    override suspend fun setInt(name: String, session: Long, value: Int?) = safeSet<Int>(name) {
        db.metadataValueDAO().insert(MetadataValue(name, session, intValue = value))
    }

    override suspend fun getDouble(name: String, session: Long): Double? = safeGet(name) {
        return db.metadataValueDAO().getDouble(name, session)
    }

    override suspend fun setDouble(name: String, session: Long, value: Double?) =
        safeSet<Double>(name) {
            db.metadataValueDAO().insert(MetadataValue(name, session, doubleValue = value))
        }

    override suspend fun getBoolean(name: String, session: Long): Boolean? = safeGet(name) {
        return db.metadataValueDAO().getBoolean(name, session)
    }

    override suspend fun setBoolean(name: String, session: Long, value: Boolean?) =
        safeSet<Boolean>(name) {
            db.metadataValueDAO().insert(MetadataValue(name, session, boolValue = value))
        }

    override suspend fun getAllFields(): List<MetadataField> {
        return db.metadataKeyDAO().getAllKeys()
    }

    companion object {
        private const val TAG = "[METADATA_STORE]"
    }
}
