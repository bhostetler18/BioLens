package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.ui.imaging.intervalDescription
import com.uf.automoth.utility.SHORT_DATE_TIME_FORMATTER
import java.time.OffsetDateTime

// Allows creating a List<Metadata> with heterogeneous contents in a type-safe manner
// This looks unnecessary, but it also allows polymorphism as opposed to template types that would
// be erased at runtime. Since there are relatively few types used, it seems like an okay compromise
// especially since it allows limiting the Metadata types to those that can actually be displayed
sealed class Metadata : MetadataInterface {

    class StringMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var value: String?,
        override val setValue: suspend (String?) -> Unit = {},
        override val validate: (String?) -> Boolean = { true }
    ) : Metadata(), MetadataInterface, MetadataValueInterface<String> {
        override suspend fun writeValue() {
            setValue(value)
        }

        override fun stringRepresentation(context: Context) = value
    }

    class IntMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var value: Int?,
        override val setValue: suspend (Int?) -> Unit = {},
        override val validate: (Int?) -> Boolean = { true }
    ) : Metadata(), MetadataInterface, MetadataValueInterface<Int> {
        override suspend fun writeValue() {
            setValue(value)
        }

        override fun stringRepresentation(context: Context): String? = value?.toString()
    }

    class DoubleMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var value: Double?,
        override val setValue: suspend (Double?) -> Unit = {},
        override val validate: (Double?) -> Boolean = { true }
    ) : Metadata(), MetadataInterface, MetadataValueInterface<Double> {
        override suspend fun writeValue() {
            setValue(value)
        }

        override fun stringRepresentation(context: Context): String? = value?.toString()
    }

    class BooleanMetadata(
        override val name: String,
        override val readonly: Boolean,
        var value: Boolean?,
        val setValue: suspend (Boolean?) -> Unit = {}
    ) : Metadata(), MetadataInterface {
        override suspend fun writeValue() {
            setValue(value)
        }

        override fun stringRepresentation(context: Context): String {
            return if (value == true) context.getString(R.string.yes) else context.getString(R.string.no)
        }
    }

    class DateMetadata(
        override val name: String,
        override val readonly: Boolean,
        override var value: OffsetDateTime?,
        override val setValue: suspend (OffsetDateTime?) -> Unit = {},
        override val validate: (OffsetDateTime?) -> Boolean = { true }
    ) : Metadata(), MetadataInterface, MetadataValueInterface<OffsetDateTime> {
        override suspend fun writeValue() {
            setValue(value)
        }

        override fun stringRepresentation(context: Context) = value?.format(SHORT_DATE_TIME_FORMATTER)
    }
}

interface MetadataValueInterface<T> {
    var value: T?
    val setValue: suspend (T?) -> Unit
    val validate: (T?) -> Boolean
}

interface MetadataInterface {
    val name: String
    val readonly: Boolean
    suspend fun writeValue()

    // Should return null when the absence of a value is significant and should be displayed
    fun stringRepresentation(context: Context): String?
}

fun getMetadata(session: Session, context: Context): List<Metadata> {
    return listOf(
        Metadata.StringMetadata(
            context.getString(R.string.name),
            false,
            session.name,
            { name -> name?.let { AutoMothRepository.renameSession(session.sessionID, it) } },
            { name -> name != null && Session.isValid(name) }
        ),
        Metadata.DoubleMetadata(
            context.getString(R.string.latitude),
            false,
            session.latitude,
            { latitude ->
                AutoMothRepository.updateSessionLocation(session.sessionID, latitude, session.longitude)
            },
            { value -> value == null || (value >= -90 && value <= 90) }
        ),
        Metadata.DoubleMetadata(
            context.getString(R.string.longitude),
            false,
            session.longitude,
            { longitude ->
                AutoMothRepository.updateSessionLocation(session.sessionID, session.latitude, longitude)
            },
            { value -> value == null || (value >= -180 && value <= 180) }
        ),
        Metadata.DateMetadata(
            context.getString(R.string.date_started),
            true,
            session.started
        ),
        Metadata.DateMetadata(
            context.getString(R.string.date_completed),
            true,
            session.completed
        ),
        Metadata.StringMetadata(
            context.getString(R.string.interval),
            true,
            intervalDescription(session.interval, context, true)
        )
    )
}
