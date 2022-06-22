package com.uf.automoth

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.services.storage.internal.TestStorageUtil
import com.uf.automoth.data.Image
import com.uf.automoth.data.ImageDatabase
import com.uf.automoth.data.Session
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.IOException
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: ImageDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ImageDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testSession() = runBlocking {
        val session = Session("TestSession1", "test1/", OffsetDateTime.now(), 50.0, 50.0)
        session.sessionID = db.sessionDAO().insert(session)

        val image = Image("test1.png", OffsetDateTime.now(), session.sessionID)
        image.imageID = db.imageDAO().insert(image)

        var images = db.sessionDAO().getImagesInSession(session.sessionID)
        assertThat(images[0], equalTo(image))

        // Foreign key structure should result in all images contained in the session being deleted
        // when their parent session is removed
        db.sessionDAO().delete(session)
        images = db.imageDAO().getAllImages()
        assert(images.isEmpty())
    }
}