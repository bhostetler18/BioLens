/*
 * Copyright (c) 2022-2023 University of Florida
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

package com.uf.biolens.data

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.uf.biolens.R
import com.uf.biolens.data.metadata.BioLensMetadataStore
import com.uf.biolens.data.metadata.prepopulate
import com.uf.biolens.imaging.ImagingSettings
import com.uf.biolens.utility.getRandomString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime

object BioLensRepository {

    private lateinit var database: BioLensDatabase
    lateinit var metadataStore: BioLensMetadataStore

    // Used for database updates that should finish regardless of whether the caller is still around
    private lateinit var coroutineScope: CoroutineScope

    lateinit var storageLocation: File
    lateinit var userID: String

    private const val DEFAULT_IMAGING_SETTINGS_FILENAME = "imaging_settings.json"
    private var JSON = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private const val TAG = "[Repository]"
    private const val KEY_SHARED_PREFERENCE_FILE = "com.uf.biolens.SHARED_PREFERENCES"
    private const val KEY_USER_ID = "com.uf.biolens.preference.USER_ID"

    operator fun invoke(context: Context, storageLocation: File, coroutineScope: CoroutineScope) {
        if (this.isInitialized) {
            return
        }
        this.storageLocation = storageLocation
        this.coroutineScope = coroutineScope
        this.database = Room.databaseBuilder(
            context,
            BioLensDatabase::class.java,
            "biolens-db"
        ).build()
        this.metadataStore = BioLensMetadataStore(database)
        coroutineScope.launch {
            prepopulate(metadataStore)
        }
        Log.d(TAG, "External file path is ${storageLocation.path}")
        userID = createOrGetUserId(context)
        Log.d(TAG, "User ID is $userID")
    }

    // These aren't truly unique, but should work until we decide how to manage unique users
    private fun createOrGetUserId(context: Context): String {
        val prefs = context.getSharedPreferences(KEY_SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE)
        val id = prefs.all[KEY_USER_ID] as? String
        return if (id == null) {
            val newId = getRandomString(5)
            with(prefs.edit()) {
                putString(KEY_USER_ID, newId)
                apply()
            }
            newId
        } else {
            id
        }
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
    val isInitialized: Boolean
        get() {
            return this::database.isInitialized
        }

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

    fun deleteSession(id: Long) = coroutineScope.launch {
        getSession(id)?.let {
            delete(it)
        }
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

    fun deleteImage(id: Long) = coroutineScope.launch {
        getImage(id)?.let {
            delete(it)
        }
    }

    suspend fun getSession(id: Long): Session? = database.sessionDAO().getSession(id)
    fun getSessionFlow(id: Long): Flow<Session?> = database.sessionDAO().getSessionFlow(id)

    suspend fun getPendingSession(requestCode: Long) =
        database.pendingSessionDAO().getPendingSession(requestCode)

    suspend fun getImage(id: Long): Image? = database.imageDAO().getImage(id)
    suspend fun getAllPendingSessions(): List<PendingSession> =
        database.pendingSessionDAO().getAllPendingSessions()

    fun getImagesInSessionFlow(id: Long): Flow<List<Image>> {
        return database.sessionDAO().getImagesInSessionFlow(id)
    }

    suspend fun getImagesInSession(id: Long): List<Image> {
        return database.sessionDAO().getImagesInSession(id)
    }

    fun getNumImagesInSession(id: Long): Flow<Int> {
        return database.sessionDAO().getNumImagesInSession(id)
    }

    fun updateSessionLocation(id: Long, latitude: Double?, longitude: Double?) =
        coroutineScope.launch {
            database.sessionDAO().updateSessionLocation(id, latitude, longitude)
        }

    suspend fun updateSessionCompletion(id: Long): OffsetDateTime {
        val completed = database.sessionDAO().getLastImageTimestampInSession(id)
        database.sessionDAO().updateSessionCompletion(id, completed)
        return completed
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

    fun saveDefaultImagingSettings(settings: ImagingSettings, context: Context) {
        val encoded = JSON.encodeToString(settings)
        context.openFileOutput(DEFAULT_IMAGING_SETTINGS_FILENAME, Context.MODE_PRIVATE).use {
            it.write(encoded.encodeToByteArray())
        }
    }

    fun loadDefaultImagingSettings(context: Context): ImagingSettings? {
        return try {
            context.openFileInput(DEFAULT_IMAGING_SETTINGS_FILENAME)?.use { input ->
                input.bufferedReader().use {
                    val settings: ImagingSettings = Json.decodeFromString(it.readText())
                    settings.shutdownCameraWhenPossible =
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean(context.getString(R.string.PREF_SHUTDOWN_CAMERA), false)
                    settings.automaticUpload =
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean(
                                context.getString(R.string.PREF_UPLOAD_AUTOMATICALLY),
                                false
                            )
                    return settings
                }
            }
        } catch (e: IOException) {
            return null
        } catch (e: SerializationException) {
            return null
        }
    }

    fun getLocationToleranceSeconds(context: Context): Int {
        return 60 * PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(context.getString(R.string.PREF_LOCATION_TOLERANCE), 5)
    }
}
