package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.data.metadata.UserMetadataField
import com.uf.automoth.data.metadata.UserMetadataStore
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.data.metadata.getValue
import com.uf.automoth.data.metadata.setValue
import com.uf.automoth.ui.imaging.intervalDescription
import com.uf.automoth.utility.getDeviceType

// Metadata inherent to the app and/or stored in the database
fun getDefaultMetadata(session: Session, context: Context): List<DisplayableMetadata> {
    return listOf(
        DisplayableMetadata.StringMetadata(
            context.getString(R.string.name),
            false,
            session.name,
            { name -> name?.let { AutoMothRepository.renameSession(session.sessionID, it) } },
            { name -> name != null && Session.isValid(name) }
        ),
        DisplayableMetadata.DoubleMetadata(
            context.getString(R.string.latitude),
            false,
            session.latitude,
            { latitude ->
                AutoMothRepository.updateSessionLocation(
                    session.sessionID,
                    latitude,
                    session.longitude
                )
            },
            { value -> value == null || (value >= -90 && value <= 90) }
        ),
        DisplayableMetadata.DoubleMetadata(
            context.getString(R.string.longitude),
            false,
            session.longitude,
            { longitude ->
                AutoMothRepository.updateSessionLocation(
                    session.sessionID,
                    session.latitude,
                    longitude
                )
            },
            { value -> value == null || (value >= -180 && value <= 180) }
        ),
        DisplayableMetadata.StringMetadata(
            context.getString(R.string.interval),
            true,
            intervalDescription(session.interval, context, true)
        ),
        DisplayableMetadata.DateMetadata(
            context.getString(R.string.date_started),
            true,
            session.started
        ),
        DisplayableMetadata.DateMetadata(
            context.getString(R.string.date_completed),
            true,
            session.completed
        ),
        DisplayableMetadata.StringMetadata(
            context.getString(R.string.device_type),
            true,
            getDeviceType()
        )
    )
}

fun csvValidator(value: String?): Boolean {
    return value == null || !value.contains(",")
}

suspend fun UserMetadataField.toDisplayableMetadata(
    db: UserMetadataStore,
    sessionID: Long
): DisplayableMetadata {
    return when (type) {
        UserMetadataType.STRING -> {
            DisplayableMetadata.StringMetadata(
                field,
                false,
                db.getValue(field, sessionID),
                { newValue -> db.setValue(field, sessionID, newValue) },
                ::csvValidator
            )
        }
        UserMetadataType.INT -> {
            DisplayableMetadata.IntMetadata(
                field,
                false,
                db.getValue(field, sessionID),
                { newValue -> db.setValue(field, sessionID, newValue) }
            )
        }
        UserMetadataType.DOUBLE -> {
            DisplayableMetadata.DoubleMetadata(
                field,
                false,
                db.getValue(field, sessionID),
                { newValue -> db.setValue(field, sessionID, newValue) }
            )
        }
        UserMetadataType.BOOLEAN -> {
            DisplayableMetadata.BooleanMetadata(
                field,
                false,
                db.getValue(field, sessionID)
            ) { newValue -> db.setValue(field, sessionID, newValue) }
        }
    }
}

// Custom or auxiliary metadata
suspend fun getUserMetadata(session: Session, store: UserMetadataStore): List<DisplayableMetadata> {
//    AutoMothRepository.metadataStore.addMetadataField("test1", UserMetadataType.BOOLEAN)
//    AutoMothRepository.metadataStore.addMetadataField("test2", UserMetadataType.STRING)
    val fields = store.getAllFields().sortedBy { it.field }
    return fields.map { it.toDisplayableMetadata(store, session.sessionID) }
}
