package com.uf.automoth.data.metadata

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
    val field: String
    val type: MetadataType
    val builtin: Boolean
}

// Makes it easier to mock/test or potentially change backing storage in the future
interface MetadataStore {
    suspend fun addMetadataField(field: String, type: MetadataType, builtin: Boolean = false)
    suspend fun deleteMetadataField(field: String)
    suspend fun getField(field: String): MetadataField?
    suspend fun getMetadataType(field: String): MetadataType?
    suspend fun getFields(builtin: Boolean): List<MetadataField>
    suspend fun getAllFields(): List<MetadataField>

    // It would be nice to do this with generics, but type erasure and issues with virtual inline/reified
    // functions make that more trouble than it's worth. At least this parallels the "put/get"
    // methods of Android SharedPreferences and Bundles, and the convenience extension methods below
    // mean they don't need to be used often
    suspend fun getString(field: String, session: Long): String?
    suspend fun setString(field: String, session: Long, value: String?)
    suspend fun getInt(field: String, session: Long): Int?
    suspend fun setInt(field: String, session: Long, value: Int?)
    suspend fun getDouble(field: String, session: Long): Double?
    suspend fun setDouble(field: String, session: Long, value: Double?)
    suspend fun getBoolean(field: String, session: Long): Boolean?
    suspend fun setBoolean(field: String, session: Long, value: Boolean?)
}

// Convenience generic functions
suspend inline fun <reified T> MetadataStore.setValue(field: String, session: Long, value: T) {
    when (value) {
        is String -> setString(field, session, value)
        is Int -> setInt(field, session, value)
        is Double -> setDouble(field, session, value)
        is Boolean -> setBoolean(field, session, value)
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

suspend inline fun <reified T> MetadataStore.getValue(field: String, session: Long): T? {
    val type = reifyMetadataType<T>()
    return if (type != null) {
        getValue(field, type, session) as? T?
    } else {
        Log.e("", "Tried to get invalid metadata type ${T::class.simpleName}")
        null
    }
}

suspend fun MetadataStore.getValue(field: String, type: MetadataType, session: Long): Any? {
    return when (type) {
        MetadataType.STRING -> getString(field, session)
        MetadataType.INT -> getInt(field, session)
        MetadataType.DOUBLE -> getDouble(field, session)
        MetadataType.BOOLEAN -> getBoolean(field, session)
    }
}

suspend fun MetadataStore.getValue(key: MetadataField, session: Long): Any? {
    return getValue(key.field, key.type, session)
}
