package com.uf.automoth

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uf.automoth.data.AutoMothDatabase
import com.uf.automoth.data.Session
import com.uf.automoth.data.metadata.AutoMothMetadataStore
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.data.metadata.UserMetadataValue
import com.uf.automoth.data.metadata.getValue
import com.uf.automoth.data.metadata.setValue
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.reflect.KSuspendFunction2
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class MetadataTest {

    private lateinit var db: AutoMothDatabase
    private lateinit var md: AutoMothMetadataStore

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AutoMothDatabase::class.java).build()
        md = AutoMothMetadataStore(db)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private suspend fun createTestSession(): Session {
        val s = Session("", "", OffsetDateTime.now(), null, null, 5)
        s.sessionID = db.sessionDAO().insert(s)
        return s
    }

    @Test
    fun testForeignKeyConstraints(): Unit = runBlocking {
        db.clearAllTables()
        var session = createTestSession()
        var sessionID = session.sessionID
        md.addMetadataField("test", UserMetadataType.STRING)

        // nonexistent session, correct key
        assertFailsWith<SQLiteConstraintException> {
            db.userMetadataValueDAO().insert(UserMetadataValue("test", 100, "don't set this"))
        }

        // nonexistent key, correct session
        assertFailsWith<SQLiteConstraintException> {
            db.userMetadataValueDAO().insert(UserMetadataValue("blah", sessionID, "don't set this"))
        }

        // Deleting session should delete all metadata for that session
        val value = "some text"
        md.setValue("test", sessionID, value)
        assertThat(md.getValue("test", sessionID), equalTo(value))
        db.sessionDAO().delete(session)
        assertThat(md.getValue<String>("test", sessionID), equalTo(null))

        // Make a new session to work with
        session = createTestSession()
        sessionID = session.sessionID

        // Deleting metadata field should delete all associated entries
        md.setValue("test", sessionID, value)
        assertThat(db.userMetadataValueDAO().getString("test", sessionID), equalTo(value))
        md.deleteMetadataField("test")
        assertThat(db.userMetadataValueDAO().getString("test", sessionID), equalTo(null))
    }

    @Test
    fun testFields(): Unit = runBlocking {
        db.clearAllTables()
        assertThat(md.getMetadataType("test"), equalTo(null))
        md.addMetadataField("test", UserMetadataType.STRING)
        assertThat(md.getField("test"), notNullValue())
        assertThat(md.getMetadataType("test"), equalTo(UserMetadataType.STRING))
        md.addMetadataField("test", UserMetadataType.BOOLEAN)
        // Existing key should not be overwritten
        assertThat(md.getMetadataType("test"), equalTo(UserMetadataType.STRING))

        md.addMetadataField("int", UserMetadataType.INT)

        val fields = md.getAllFields()
        assertThat(fields.size, equalTo(2))
    }

    private fun correctGetterFor(type: UserMetadataType): KSuspendFunction2<String, Long, Any?> {
        return when (type) {
            UserMetadataType.STRING -> db.userMetadataValueDAO()::getString
            UserMetadataType.INT -> db.userMetadataValueDAO()::getInt
            UserMetadataType.DOUBLE -> db.userMetadataValueDAO()::getDouble
            UserMetadataType.BOOLEAN -> db.userMetadataValueDAO()::getBoolean
        }
    }

    private fun getSampleValue(type: UserMetadataType): Any {
        return when (type) {
            UserMetadataType.STRING -> "something"
            UserMetadataType.INT -> 2
            UserMetadataType.DOUBLE -> 5.8
            UserMetadataType.BOOLEAN -> true
        }
    }

    @Test
    fun testValues(): Unit = runBlocking {
        db.clearAllTables()
        val session = createTestSession()
        val sessionID = session.sessionID

        // Test basic set use case
        md.addMetadataField("test", UserMetadataType.STRING)
        val value = "blah"
        md.setValue("test", sessionID, value)
        assertThat(md.getValue("test", sessionID), equalTo(value))

        // Ensure that writing values has no effect unless of the correct type
        for (type in UserMetadataType.values()) {
            val fieldName = "field_${type.name}"
            md.addMetadataField(fieldName, type)

            md.setString(fieldName, sessionID, getSampleValue(UserMetadataType.STRING) as String)
            md.setInt(fieldName, sessionID, getSampleValue(UserMetadataType.INT) as Int)
            md.setDouble(fieldName, sessionID, getSampleValue(UserMetadataType.DOUBLE) as Double)
            md.setBoolean(fieldName, sessionID, getSampleValue(UserMetadataType.BOOLEAN) as Boolean)

            // Use DAO methods directly since getters on MetadataStore already check for attempts to
            // get the incorrect type from a given metadata field and thus wouldn't reflect if a
            // bogus value got into the actual database table
            val allGetters = setOf(
                db.userMetadataValueDAO()::getBoolean,
                db.userMetadataValueDAO()::getString,
                db.userMetadataValueDAO()::getDouble,
                db.userMetadataValueDAO()::getInt
            )
            val workingGetter = correctGetterFor(type)
            for (get in allGetters.minus(workingGetter)) {
                val x = get(fieldName, sessionID)
                assertThat(x, equalTo(null))
            }
            assertThat(workingGetter(fieldName, sessionID), equalTo(getSampleValue(type)))
        }
    }
}
