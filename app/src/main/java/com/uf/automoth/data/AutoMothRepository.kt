package com.uf.automoth.data

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.room.Room

object AutoMothRepository {

    private lateinit var imageDatabase: ImageDatabase

    operator fun invoke(context: Context) {
        imageDatabase = Room.databaseBuilder(context,
            ImageDatabase::class.java,
            "image-db").build()
    }

    val allSessionsFlow by lazy {
        imageDatabase.sessionDAO().getAllSessions()
    }

    @WorkerThread
    suspend fun insert(session: Session) {
        val id = imageDatabase.sessionDAO().insert(session)
        session.sessionID = id
        //TODO: create filesystem storage
    }

    @WorkerThread
    suspend fun delete(session: Session) {
        imageDatabase.sessionDAO().delete(session)
        //TODO: delete associated filesystem storage
    }

}