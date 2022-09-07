package com.uf.automoth.data.export

import com.uf.automoth.BuildConfig
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session

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
        this.addColumn("session_unique_id") {
            uniqueSessionId
        }
        this.addColumn("session_name") {
            session.name
        }
        this.addColumn("session_latitude") {
            if (session.hasLocation()) session.latitude!!.toString() else ""
        }
        this.addColumn("session_longitude") {
            if (session.hasLocation()) session.longitude!!.toString() else ""
        }
        this.addColumn("session_local_start_datetime") {
            session.started.toString()
        }
        this.addColumn("session_local_end datetime") {
            session.completed?.toString() ?: ""
        }
        this.addColumn("session_frame_interval_seconds") {
            session.interval.toString()
        }
        this.addColumn("automoth_app_version") {
            BuildConfig.VERSION_NAME
        }
    }
}
