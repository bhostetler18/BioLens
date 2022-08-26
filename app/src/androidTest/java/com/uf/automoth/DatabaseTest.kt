package com.uf.automoth

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uf.automoth.data.AutoMothDatabase
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AutoMothDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AutoMothDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testForeignKeyConstraint() = runBlocking {
        val session = Session("TestSession1", "test1/", OffsetDateTime.now(), 50.0, 50.0, 5)
        session.sessionID = db.sessionDAO().insert(session)

        val image = Image("test1.png", OffsetDateTime.now(), session.sessionID)
        image.imageID = db.imageDAO().insert(image)

        var images = db.sessionDAO().getImagesInSessionBlocking(session.sessionID)
        assertThat(images[0], equalTo(image))

        // Foreign key structure should result in all images contained in the session being deleted
        // when their parent session is removed
        db.sessionDAO().delete(session)
        images = db.imageDAO().getAllImages()
        assert(images.isEmpty())

        // Foreign key structure should also prevent insertion into a nonexistent session
        assertFailsWith<SQLiteConstraintException> {
            db.imageDAO().insert(image)
        }
        images = db.imageDAO().getAllImages()
        assert(images.isEmpty())
    }
}
