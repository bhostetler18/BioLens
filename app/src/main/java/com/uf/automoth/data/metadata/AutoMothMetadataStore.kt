package com.uf.automoth.data.metadata

import android.util.Log
import com.uf.automoth.data.AutoMothDatabase

class AutoMothMetadataStore(private val db: AutoMothDatabase) : MetadataStore {

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

    override suspend fun deleteMetadataField(field: String) {
        db.userMetadataKeyDAO().delete(field)
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
