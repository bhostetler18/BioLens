package com.uf.automoth.data

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime

object AutoMothRepository {

    private lateinit var imageDatabase: ImageDatabase
    private lateinit var coroutineScope: CoroutineScope
    lateinit var storageLocation: File

    operator fun invoke(context: Context, coroutineScope: CoroutineScope) {
        imageDatabase = Room.databaseBuilder(
            context,
            ImageDatabase::class.java,
            "image-db"
        ).fallbackToDestructiveMigration() // TODO: remove when ready to release
            .build()
        storageLocation = context.getExternalFilesDir(null)!! // TODO: handle ejection?
        this.coroutineScope = coroutineScope
        Log.d("[INFO]", "External file path is ${storageLocation.path}")
    }

    val allSessionsFlow by lazy {
        imageDatabase.sessionDAO().getAllSessions()
    }

    // TODO: indicate filesystem errors in below functions either by exception or return

    suspend fun create(session: Session) {
        val sessionDir = File(storageLocation, session.directory)
        val created = withContext(Dispatchers.IO) {
            return@withContext sessionDir.mkdir()
        }
        if (created) {
            session.sessionID = imageDatabase.sessionDAO().insert(session)
        }
    }

    fun delete(session: Session) = coroutineScope.launch {
        val sessionDir = File(storageLocation, session.directory)
        val deleted = withContext(Dispatchers.IO) {
            return@withContext sessionDir.deleteRecursively()
        }
        if (deleted) {
            imageDatabase.sessionDAO().delete(session)
        }
    }

    fun insert(image: Image) = coroutineScope.launch {
        image.imageID = imageDatabase.imageDAO().insert(image)
    }

    fun delete(image: Image) = coroutineScope.launch {
        val session = imageDatabase.sessionDAO().getSession(image.parentSessionID)
        val sessionDir = File(storageLocation, session.directory)
        val imagePath = File(sessionDir, image.filename)

        val deleted = withContext(Dispatchers.IO) {
            return@withContext imagePath.delete()
        }

        if (deleted) {
            imageDatabase.imageDAO().delete(image)
        }
    }

    fun getSession(id: Long): Session = imageDatabase.sessionDAO().getSession(id)

    fun getImagesInSession(id: Long): LiveData<List<Image>> {
        return imageDatabase.sessionDAO().getImagesInSession(id)
    }

    fun updateSessionLocation(id: Long, location: Location) = coroutineScope.launch {
        imageDatabase.sessionDAO().updateSessionLocation(id, location.latitude, location.longitude)
    }

    fun updateSessionCompletion(id: Long, time: OffsetDateTime) = coroutineScope.launch {
        imageDatabase.sessionDAO().updateSessionCompletion(id, time)
    }
}
