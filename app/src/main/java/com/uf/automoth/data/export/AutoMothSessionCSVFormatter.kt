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

package com.uf.automoth.data.export

import com.uf.automoth.BuildConfig
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.data.metadata.MetadataStore
import com.uf.automoth.data.metadata.getValue
import com.uf.automoth.ui.common.ExportOptions
import com.uf.automoth.utility.getDeviceType

class AutoMothSessionCSVFormatter(
    private val session: Session
) : BasicSessionCSVFormatter() {

    val uniqueSessionId: String by lazy {
        "${AutoMothRepository.userID}-${session.sessionID}"
    }

    fun getUniqueImageId(image: Image): String {
        return "$uniqueSessionId-${image.index}"
    }

    init {
        this.addColumn("frame_position_in_session") { image ->
            image.index.toString()
        }
        this.addColumn("frame_unique_id") { image ->
            getUniqueImageId(image)
        }
        this.addColumn("frame_local_datetime") { image ->
            image.timestamp.toString()
        }
        this.addColumn("frame_filename") { image ->
            image.filename
        }
        this.addConstantColumn("session_unique_id", uniqueSessionId)
        this.addConstantColumn("session_name", session.name)
        this.addConstantColumn("session_latitude", session.latitude.toStringOrEmpty())
        this.addConstantColumn("session_longitude", session.longitude.toStringOrEmpty())
        this.addConstantColumn("session_local_start_datetime", session.started.toString())
        this.addConstantColumn("session_local_end_datetime", session.completed.toStringOrEmpty())
        this.addConstantColumn("session_frame_interval_seconds", session.interval.toString())
        this.addConstantColumn("device_type", getDeviceType())
        this.addConstantColumn("automoth_app_version", BuildConfig.VERSION_NAME)
    }

    private suspend fun addUserMetadata(metadataStore: MetadataStore) {
        metadataStore.getFields(false).forEach {
            val value = metadataStore.getValue(it, session.sessionID)
            addConstantColumn(it.field, value.toStringOrEmpty())
        }
    }

    private suspend fun addAutoMothMetadata(metadataStore: MetadataStore) {
        metadataStore.getFields(true).forEach {
            val value = metadataStore.getValue(it, session.sessionID)
            addConstantColumn(it.field, value.toStringOrEmpty())
        }
    }

    suspend fun configure(options: ExportOptions, metadataStore: MetadataStore) {
        if (options.includeAutoMothMetadata) {
            addAutoMothMetadata(metadataStore)
        }
        if (options.includeUserMetadata) {
            addUserMetadata(metadataStore)
        }
    }
}

fun Any?.toStringOrEmpty(): String = this?.toString() ?: ""
