package com.uf.automoth.data.export

import com.uf.automoth.BuildConfig
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.data.metadata.UserMetadataStore
import com.uf.automoth.data.metadata.getValue
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

    suspend fun addUserMetadata(metadataStore: UserMetadataStore) {
        metadataStore.getAllFields().forEach {
            val value = metadataStore.getValue(it, session.sessionID)
            addConstantColumn(it.field, value.toStringOrEmpty())
        }
    }

    suspend fun addAutoMothMetadata() {
    }
}

fun Any?.toStringOrEmpty(): String = this?.toString() ?: ""
