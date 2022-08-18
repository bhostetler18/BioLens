package com.uf.automoth.data

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime

object AutoMothRepository {

    private lateinit var database: AutoMothDatabase
    private lateinit var coroutineScope: CoroutineScope
    lateinit var storageLocation: File

    operator fun invoke(context: Context, coroutineScope: CoroutineScope) {
        if (this::database.isInitialized) {
            return
        }
        database = Room.databaseBuilder(
            context,
            AutoMothDatabase::class.java,
            "automoth-db"
        ).fallbackToDestructiveMigration() // TODO: remove when ready to release
            .build()
        storageLocation = context.getExternalFilesDir(null)!! // TODO: handle ejection?
        this.coroutineScope = coroutineScope
        Log.d("[INFO]", "External file path is ${storageLocation.path}")
    }

    val allSessionsFlow by lazy {
        database.sessionDAO().getAllSessions()
    }
    val allPendingSessionsFlow by lazy {
        database.pendingSessionDAO().getAllPendingSessionsFlow()
    }
    val earliestPendingSessionFlow by lazy {
        database.pendingSessionDAO().getEarliestPendingSession()
    }

    // TODO: indicate filesystem errors in below functions either by exception or return

    suspend fun create(session: Session): Long? {
        val sessionDir = File(storageLocation, session.directory)
        val created = withContext(Dispatchers.IO) {
            return@withContext sessionDir.mkdir()
        }
        if (created) {
            session.sessionID = database.sessionDAO().insert(session)
            return session.sessionID
        }
        return null
    }

    fun delete(session: Session) = coroutineScope.launch {
        val sessionDir = File(storageLocation, session.directory)
        val deleted = withContext(Dispatchers.IO) {
            return@withContext sessionDir.deleteRecursively()
        }
        if (deleted) {
            database.sessionDAO().delete(session)
        }
    }

    suspend fun create(pendingSession: PendingSession): Long {
        pendingSession.requestCode = database.pendingSessionDAO().insert(pendingSession)
        return pendingSession.requestCode
    }

    fun deletePendingSession(requestCode: Long) = coroutineScope.launch {
        database.pendingSessionDAO().deleteByRequestCode(requestCode)
    }

    fun insert(image: Image) = coroutineScope.launch {
        image.imageID = database.imageDAO().insert(image)
    }

    fun delete(image: Image) = coroutineScope.launch {
        val session = database.sessionDAO().getSession(image.parentSessionID) ?: return@launch
        val sessionDir = File(storageLocation, session.directory)
        val imagePath = File(sessionDir, image.filename)

        val deleted = withContext(Dispatchers.IO) {
            return@withContext imagePath.delete()
        }

        if (deleted) {
            database.imageDAO().delete(image)
        }
    }

    suspend fun getSession(id: Long): Session? = database.sessionDAO().getSession(id)
    suspend fun getPendingSession(requestCode: Long) =
        database.pendingSessionDAO().getPendingSession(requestCode)

    suspend fun getImage(id: Long): Image? = database.imageDAO().getImage(id)
    suspend fun getAllPendingSessions(): List<PendingSession> =
        database.pendingSessionDAO().getAllPendingSessions()

    fun getImagesInSession(id: Long): Flow<List<Image>> {
        return database.sessionDAO().getImagesInSession(id)
    }

    fun getImagesInSessionBlocking(id: Long): List<Image> {
        return database.sessionDAO().getImagesInSessionBlocking(id)
    }

    fun getNumImagesInSession(id: Long): Flow<Int> {
        return database.sessionDAO().getNumImagesInSession(id)
    }

    fun updateSessionLocation(id: Long, location: Location) = coroutineScope.launch {
        database.sessionDAO().updateSessionLocation(id, location.latitude, location.longitude)
    }

    fun updateSessionCompletion(id: Long, time: OffsetDateTime) = coroutineScope.launch {
        database.sessionDAO().updateSessionCompletion(id, time)
    }

    fun renameSession(id: Long, name: String) = coroutineScope.launch {
        database.sessionDAO().renameSession(id, name)
    }

    fun resolve(session: Session): File {
        return File(storageLocation, session.directory)
    }

    fun resolve(image: Image, session: Session): File {
        return File(resolve(session), image.filename)
    }
}
