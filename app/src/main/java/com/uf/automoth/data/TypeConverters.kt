package com.uf.automoth.data

import androidx.room.TypeConverter
import com.uf.automoth.data.metadata.UserMetadataType
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object AutoMothTypeConverters {

    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    @JvmStatic
    fun toOffsetDateTime(value: String?): OffsetDateTime? {
        return value?.let {
            return formatter.parse(value, OffsetDateTime::from)
        }
    }

    @TypeConverter
    @JvmStatic
    fun fromOffsetDateTime(date: OffsetDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    @JvmStatic
    fun toUserMetadataType(value: String?): UserMetadataType? {
        return value?.let { UserMetadataType.from(it) }
    }

    @TypeConverter
    @JvmStatic
    fun fromUserMetadataType(type: UserMetadataType?): String? {
        return type?.raw
    }
}
