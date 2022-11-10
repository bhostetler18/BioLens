package com.uf.automoth.data.metadata

import android.util.Log

enum class UserMetadataType(val raw: String) {
    STRING("string"), INT("int"), DOUBLE("double"), BOOLEAN("boolean");

    companion object {
        fun from(raw: String): UserMetadataType? {
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

interface UserMetadataField {
    val field: String
    val type: UserMetadataType
}

// Makes it easier to mock/test or potentially change backing storage in the future
interface UserMetadataStore {
    suspend fun addMetadataField(field: String, type: UserMetadataType)
    suspend fun deleteMetadataField(field: String)
    suspend fun getField(field: String): UserMetadataField?
    suspend fun getMetadataType(field: String): UserMetadataType?
    suspend fun getAllFields(): List<UserMetadataField>

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
suspend inline fun <reified T> UserMetadataStore.setValue(field: String, session: Long, value: T) {
    when (value) {
        is String -> setString(field, session, value)
        is Int -> setInt(field, session, value)
        is Double -> setDouble(field, session, value)
        is Boolean -> setBoolean(field, session, value)
        else -> Log.e("", "Tried to set invalid metadata type ${T::class.simpleName}")
    }
}

inline fun <reified T> reifyUserMetadataType(): UserMetadataType? {
    return when (T::class) {
        String::class -> UserMetadataType.STRING
        Int::class -> UserMetadataType.INT
        Double::class -> UserMetadataType.DOUBLE
        Boolean::class -> UserMetadataType.BOOLEAN
        else -> null
    }
}

suspend inline fun <reified T> UserMetadataStore.getValue(field: String, session: Long): T? {
    return when (reifyUserMetadataType<T>()) {
        UserMetadataType.STRING -> getString(field, session) as? T
        UserMetadataType.INT -> getInt(field, session) as? T
        UserMetadataType.DOUBLE -> getDouble(field, session) as? T
        UserMetadataType.BOOLEAN -> getBoolean(field, session) as? T
        else -> {
            Log.e("", "Tried to get invalid metadata type ${T::class.simpleName}")
            null
        }
    }
}
