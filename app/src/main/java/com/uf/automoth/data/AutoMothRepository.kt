package com.uf.automoth.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object AutoMothRepository {

    private lateinit var imageDatabase: ImageDatabase
    private lateinit var coroutineScope: CoroutineScope
    private var filesystemPath: File? = null

    operator fun invoke(context: Context, coroutineScope: CoroutineScope) {
        imageDatabase = Room.databaseBuilder(
            context,
            ImageDatabase::class.java,
            "image-db"
        ).fallbackToDestructiveMigration() // TODO: remove when ready to release
            .build()
        filesystemPath = context.getExternalFilesDir(null) // TODO: handle ejection?
        this.coroutineScope = coroutineScope
        Log.d("[INFO]", "External file path is ${filesystemPath?.path}")
    }

    val allSessionsFlow by lazy {
        imageDatabase.sessionDAO().getAllSessions()
    }

    // TODO: indicate filesystem errors in below functions either by exception or return

    fun insert(session: Session) = coroutineScope.launch {
        val parent = filesystemPath ?: return@launch
        val sessionDir = File(parent, session.directory)
        val created = withContext(Dispatchers.IO) {
            return@withContext sessionDir.mkdir()
        }
        if (created) {
            session.sessionID = imageDatabase.sessionDAO().insert(session)
        }
    }

    fun delete(session: Session) = coroutineScope.launch {
        val parent = filesystemPath ?: return@launch
        val sessionDir = File(parent, session.directory)
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
        val parent = filesystemPath ?: return@launch
        val session = imageDatabase.sessionDAO().getSession(image.parentSessionID)
        val sessionDir = File(parent, session.directory)
        val imagePath = File(sessionDir, image.filename)

        val deleted = withContext(Dispatchers.IO) {
            return@withContext imagePath.delete()
        }

        if (deleted) {
            imageDatabase.imageDAO().delete(image)
        }
    }
}
