package com.uf.automoth.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uf.automoth.data.metadata.MetadataKey
import com.uf.automoth.data.metadata.MetadataKeyDAO
import com.uf.automoth.data.metadata.MetadataValue
import com.uf.automoth.data.metadata.MetadataValueDAO

@Database(
    version = 4,
    entities = [
        Image::class, Session::class, PendingSession::class,
        MetadataKey::class, MetadataValue::class
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]
)
@TypeConverters(AutoMothTypeConverters::class)
abstract class AutoMothDatabase : RoomDatabase() {

    abstract fun imageDAO(): ImageDAO
    abstract fun sessionDAO(): SessionDAO
    abstract fun pendingSessionDAO(): PendingSessionDAO
    abstract fun userMetadataKeyDAO(): MetadataKeyDAO
    abstract fun userMetadataValueDAO(): MetadataValueDAO
}
