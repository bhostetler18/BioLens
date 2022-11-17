package com.uf.automoth.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uf.automoth.data.metadata.UserMetadataKey
import com.uf.automoth.data.metadata.UserMetadataKeyDAO
import com.uf.automoth.data.metadata.UserMetadataValue
import com.uf.automoth.data.metadata.UserMetadataValueDAO

@Database(
    version = 3,
    entities = [
        Image::class, Session::class, PendingSession::class, UserMetadataKey::class, UserMetadataValue::class
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(AutoMothTypeConverters::class)
abstract class AutoMothDatabase : RoomDatabase() {

    abstract fun imageDAO(): ImageDAO
    abstract fun sessionDAO(): SessionDAO
    abstract fun pendingSessionDAO(): PendingSessionDAO
    abstract fun userMetadataKeyDAO(): UserMetadataKeyDAO
    abstract fun userMetadataValueDAO(): UserMetadataValueDAO
}
