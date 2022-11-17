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

// data class PrepopulatedMetadata(
//    val key: UserMetadataKey,
//    @StringRes val translation: Int? // allow localization even with hardcoded keys
// )
//
// val AUTOMOTH_METADATA = listOf(
//    PrepopulatedMetadata(
//        UserMetadataKey("Sheet width (cm)", UserMetadataType.DOUBLE),
//        null
//    ),
//    PrepopulatedMetadata(
//        UserMetadataKey("Sheet height (cm)", UserMetadataType.DOUBLE),
//        null
//    )
// )
// val RESERVED_KEYS = AUTOMOTH_METADATA.map { it.key.field }.toHashSet()

// suspend fun prepopulate(store: UserMetadataStore) {
//    AUTOMOTH_METADATA.forEach {
//        if (store.getField(it.key.field) == null) {
//            store.addMetadataField(it.key.field, it.key.type)
//        }
//    }
// }

// Basic metadata inherent to the app
fun getDefaultMetadata(
    session: Session,
    context: Context,
    observer: MetadataChangeObserver = null
): List<MetadataTableDataModel> {
    return listOf(
        MetadataTableDataModel.StringMetadata(
            context.getString(R.string.name),
            false,
            session.name,
            { name -> name?.let { AutoMothRepository.renameSession(session.sessionID, it) } },
            { name -> name != null && Session.isValid(name) }
        ),
        MetadataTableDataModel.DoubleMetadata(
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
        MetadataTableDataModel.DoubleMetadata(
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
        MetadataTableDataModel.StringMetadata(
            context.getString(R.string.interval),
            true,
            intervalDescription(session.interval, context, true)
        ),
        MetadataTableDataModel.DateMetadata(
            context.getString(R.string.date_started),
            true,
            session.started
        ),
        MetadataTableDataModel.DateMetadata(
            context.getString(R.string.date_completed),
            true,
            session.completed
        ),
        MetadataTableDataModel.StringMetadata(
            context.getString(R.string.device_type),
            true,
            getDeviceType()
        )
    ).onEach { it.observer = observer }
}

fun csvValidator(value: String?): Boolean {
    return value == null || !value.contains(",")
}

fun fieldNameValidator(name: String): Boolean {
    return name.trim().isNotEmpty() // && !RESERVED_KEYS.contains(name)
}

suspend fun UserMetadataField.toDisplayableMetadata(
    db: UserMetadataStore,
    sessionID: Long,
    deletable: Boolean = false,
    nameOverride: String? = null,
    observer: MetadataChangeObserver = null
): MetadataTableDataModel {
    return when (type) {
        UserMetadataType.STRING -> {
            MetadataTableDataModel.StringMetadata(
                nameOverride ?: field,
                false,
                db.getValue(field, sessionID),
                { newValue -> db.setValue(field, sessionID, newValue) },
                ::csvValidator
            )
        }
        UserMetadataType.INT -> {
            MetadataTableDataModel.IntMetadata(
                nameOverride ?: field,
                false,
                db.getValue(field, sessionID),
                { newValue -> db.setValue(field, sessionID, newValue) }
            )
        }
        UserMetadataType.DOUBLE -> {
            MetadataTableDataModel.DoubleMetadata(
                nameOverride ?: field,
                false,
                db.getValue(field, sessionID),
                { newValue -> db.setValue(field, sessionID, newValue) }
            )
        }
        UserMetadataType.BOOLEAN -> {
            MetadataTableDataModel.BooleanMetadata(
                nameOverride ?: field,
                false,
                db.getValue(field, sessionID)
            ) { newValue -> db.setValue(field, sessionID, newValue) }
        }
    }.also {
        it.deletable = deletable
        it.observer = observer
        it.userField = this
    }
}

suspend fun getUserMetadata(
    sessionID: Long,
    store: UserMetadataStore,
    observer: MetadataChangeObserver
): List<MetadataTableDataModel> {
    return store.getAllFields().map {
        it.toDisplayableMetadata(store, sessionID, true, observer = observer)
    }
}

// Prepopulated metadata specific to AutoMoth
// suspend fun getAutoMothMetadata(session: Session, store: UserMetadataStore, context: Context): List<DisplayableMetadata> {
//    return AUTOMOTH_METADATA.map {
//        val translatedName = it.translation?.let { resourceId ->
//            context.getString(resourceId)
//        }
//        it.key.toDisplayableMetadata(store, session.sessionID, translatedName)
//    }
// }
