package com.uf.automoth.ui.metadata

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.ui.imaging.intervalDescription
import com.uf.automoth.utility.getDeviceType

// Metadata inherent to the app and/or stored in the database
fun getDefaultMetadata(session: Session, context: Context): List<Metadata> {
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
        Metadata.StringMetadata(
            context.getString(R.string.interval),
            true,
            intervalDescription(session.interval, context, true)
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
            context.getString(R.string.device_type),
            true,
            getDeviceType()
        ),
    )
}

// Custom or auxiliary metadata
fun getUserMetadata(session: Session, context: Context): List<Metadata> {
    return listOf(
        Metadata.IntMetadata(
            "test",
            false,
            1
        ),
        Metadata.BooleanMetadata(
            "test",
            false,
            true
        ),
        Metadata.BooleanMetadata(
            "test",
            true,
            false
        )
    )
}
