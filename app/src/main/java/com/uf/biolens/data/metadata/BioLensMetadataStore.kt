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
import com.uf.biolens.data.BioLensDatabase

class BioLensMetadataStore(private val db: BioLensDatabase) : MetadataStore {

    private suspend inline fun <reified T> safeSet(field: String, set: () -> Unit) {
        val entry = db.userMetadataKeyDAO().get(field)
        if (entry != null) {
            val requiredType = entry.type
            val actualType = reifyMetadataType<T>()
            if (actualType == requiredType) {
                set()
            } else {
                Log.e(
                    TAG,
                    "Tried to write value of type $actualType to field '$field' of type $requiredType"
                )
            }
        } else {
            Log.e(TAG, "Cannot write to nonexistent field '$field'")
        }
    }

    private suspend inline fun <reified T> safeGet(
        field: String,
        get: () -> T?
    ): T? {
        val entry = db.userMetadataKeyDAO().get(field)
        return if (entry != null) {
            val desiredType = reifyMetadataType<T>()
            if (entry.type == desiredType) {
                get()
            } else {
                Log.e(
                    TAG,
                    "Tried to get $desiredType from metadata field '$field' of type ${entry.type}"
                )
                null
            }
        } else {
            Log.e(TAG, "Tried to get nonexistent metadata field $field")
            null
        }
    }

    override suspend fun addMetadataField(field: String, type: MetadataType, builtin: Boolean) {
        db.userMetadataKeyDAO().insert(MetadataKey(field, type, builtin))
    }

    override suspend fun deleteMetadataField(field: String, builtin: Boolean) {
        db.userMetadataKeyDAO().delete(field, builtin)
    }

    override suspend fun renameField(originalName: String, newName: String, builtin: Boolean) {
        val existingOriginal = db.userMetadataKeyDAO().get(originalName) ?: return
        if (existingOriginal.builtin != builtin) {
            Log.d(
                TAG,
                "Attempting to rename field '$originalName' with builtin='$builtin', " +
                    "but the existing field has builtin='${existingOriginal.builtin}'"
            )
            return
        }
        if (db.userMetadataKeyDAO().get(newName) != null) {
            Log.d(
                TAG,
                "Attempting to rename '$originalName' to '$newName', but '$newName' already exists"
            )
            return
        }
        // Insert first to prevent foreign key constraint conflicts (and avoid losing values that
        // would be wiped if the original key was deleted first)
        db.userMetadataKeyDAO().insert(MetadataKey(newName, existingOriginal.type, builtin))
        db.userMetadataValueDAO().rename(originalName, newName)
        // Delete now that values have been updated
        db.userMetadataKeyDAO().delete(existingOriginal)
    }

    override suspend fun getField(field: String): MetadataField? {
        return db.userMetadataKeyDAO().get(field)
    }

    override suspend fun getMetadataType(field: String): MetadataType? {
        return db.userMetadataKeyDAO().getType(field)
    }

    override suspend fun getFields(builtin: Boolean): List<MetadataField> {
        return db.userMetadataKeyDAO().getKeys(builtin)
    }

    override suspend fun getString(field: String, session: Long): String? = safeGet(field) {
        db.userMetadataValueDAO().getString(field, session)
    }

    override suspend fun setString(field: String, session: Long, value: String?) =
        safeSet<String>(field) {
            db.userMetadataValueDAO().insert(MetadataValue(field, session, stringValue = value))
        }

    override suspend fun getInt(field: String, session: Long): Int? = safeGet(field) {
        return db.userMetadataValueDAO().getInt(field, session)
    }

    override suspend fun setInt(field: String, session: Long, value: Int?) = safeSet<Int>(field) {
        db.userMetadataValueDAO().insert(MetadataValue(field, session, intValue = value))
    }

    override suspend fun getDouble(field: String, session: Long): Double? = safeGet(field) {
        return db.userMetadataValueDAO().getDouble(field, session)
    }

    override suspend fun setDouble(field: String, session: Long, value: Double?) =
        safeSet<Double>(field) {
            db.userMetadataValueDAO().insert(MetadataValue(field, session, doubleValue = value))
        }

    override suspend fun getBoolean(field: String, session: Long): Boolean? = safeGet(field) {
        return db.userMetadataValueDAO().getBoolean(field, session)
    }

    override suspend fun setBoolean(field: String, session: Long, value: Boolean?) =
        safeSet<Boolean>(field) {
            db.userMetadataValueDAO().insert(MetadataValue(field, session, boolValue = value))
        }

    override suspend fun getAllFields(): List<MetadataField> {
        return db.userMetadataKeyDAO().getAllKeys()
    }

    companion object {
        private const val TAG = "[METADATA_STORE]"
    }
}
