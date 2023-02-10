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

package com.uf.biolens.ui.metadata

import android.content.Context
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Session
import com.uf.biolens.data.metadata.AUTOMOTH_METADATA
import com.uf.biolens.data.metadata.AUTOMOTH_METADATA_VALIDATORS
import com.uf.biolens.data.metadata.MetadataField
import com.uf.biolens.data.metadata.MetadataKey
import com.uf.biolens.data.metadata.MetadataStore
import com.uf.biolens.data.metadata.MetadataType
import com.uf.biolens.data.metadata.getValue
import com.uf.biolens.data.metadata.setValue
import com.uf.biolens.ui.imaging.intervalDescription
import com.uf.biolens.utility.getDeviceType

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
            { name -> name?.let { BioLensRepository.renameSession(session.sessionID, it) } },
            { name -> name != null && Session.isValid(name) }
        ),
        MetadataTableDataModel.DoubleMetadata(
            context.getString(R.string.latitude),
            false,
            session.latitude,
            { latitude ->
                BioLensRepository.updateSessionLocation(
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
                BioLensRepository.updateSessionLocation(
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

suspend fun MetadataField.toDisplayableMetadata(
    sessionID: Long,
    db: MetadataStore,
    nameOverride: String? = null,
    observer: MetadataChangeObserver = null,
    validator: ((Any?) -> Boolean) = { true }
): MetadataTableDataModel {
    return when (type) {
        MetadataType.STRING -> {
            val combined = { value: String? ->
                csvValidator(value) && validator(value)
            }
            MetadataTableDataModel.StringMetadata(
                nameOverride ?: name,
                false,
                db.getValue(name, sessionID),
                { newValue -> db.setValue(name, sessionID, newValue) },
                combined
            )
        }
        MetadataType.INT -> {
            MetadataTableDataModel.IntMetadata(
                nameOverride ?: name,
                false,
                db.getValue(name, sessionID),
                { newValue -> db.setValue(name, sessionID, newValue) },
                validator
            )
        }
        MetadataType.DOUBLE -> {
            MetadataTableDataModel.DoubleMetadata(
                nameOverride ?: name,
                false,
                db.getValue(name, sessionID),
                { newValue -> db.setValue(name, sessionID, newValue) },
                validator
            )
        }
        MetadataType.BOOLEAN -> {
            MetadataTableDataModel.BooleanMetadata(
                nameOverride ?: name,
                false,
                db.getValue(name, sessionID)
            ) { newValue -> db.setValue(name, sessionID, newValue) }
        }
    }.also {
        it.observer = observer
        if (!this.builtin) {
            it.userField = this
        }
    }
}

suspend fun getUserMetadata(
    sessionID: Long,
    store: MetadataStore,
    observer: MetadataChangeObserver
): List<MetadataTableDataModel> {
    return store.getFields(false).map {
        it.toDisplayableMetadata(sessionID, store, observer = observer)
    }
}

// Pre-populated metadata specific to AutoMoth
suspend fun getAutoMothMetadata(
    sessionID: Long,
    store: MetadataStore,
    observer: MetadataChangeObserver,
    context: Context
): List<MetadataTableDataModel> {
    return AUTOMOTH_METADATA.map { metadata ->
        val displayName = context.getString(metadata.translation)
        val key = MetadataKey(metadata.name, metadata.type, true)
        AUTOMOTH_METADATA_VALIDATORS[metadata.name]?.let { validator ->
            return@map key.toDisplayableMetadata(sessionID, store, displayName, observer, validator)
        }
        return@map key.toDisplayableMetadata(sessionID, store, displayName, observer)
    }
}
