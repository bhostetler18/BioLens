/*
 * Copyright (c) 2022 University of Florida
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

package com.uf.automoth

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uf.automoth.data.AutoMothDatabase
import com.uf.automoth.data.Session
import com.uf.automoth.data.metadata.AutoMothMetadataStore
import com.uf.automoth.data.metadata.MetadataType
import com.uf.automoth.data.metadata.MetadataValue
import com.uf.automoth.data.metadata.getValue
import com.uf.automoth.data.metadata.setValue
import com.uf.automoth.ui.metadata.AUTOMOTH_METADATA
import com.uf.automoth.ui.metadata.prepopulate
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
        md.addMetadataField("test", MetadataType.STRING)

        // nonexistent session, correct key
        assertFailsWith<SQLiteConstraintException> {
            db.userMetadataValueDAO().insert(MetadataValue("test", 100, "don't set this"))
        }

        // nonexistent key, correct session
        assertFailsWith<SQLiteConstraintException> {
            db.userMetadataValueDAO().insert(MetadataValue("blah", sessionID, "don't set this"))
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
        md.addMetadataField("test", MetadataType.STRING)
        assertThat(md.getField("test"), notNullValue())
        assertThat(md.getMetadataType("test"), equalTo(MetadataType.STRING))
        md.addMetadataField("test", MetadataType.BOOLEAN)
        // Existing key should not be overwritten
        assertThat(md.getMetadataType("test"), equalTo(MetadataType.STRING))

        md.addMetadataField("int", MetadataType.INT)

        val fields = md.getAllFields()
        assertThat(fields.size, equalTo(2))
    }

    private fun correctGetterFor(type: MetadataType): KSuspendFunction2<String, Long, Any?> {
        return when (type) {
            MetadataType.STRING -> db.userMetadataValueDAO()::getString
            MetadataType.INT -> db.userMetadataValueDAO()::getInt
            MetadataType.DOUBLE -> db.userMetadataValueDAO()::getDouble
            MetadataType.BOOLEAN -> db.userMetadataValueDAO()::getBoolean
        }
    }

    private fun getSampleValue(type: MetadataType): Any {
        return when (type) {
            MetadataType.STRING -> "something"
            MetadataType.INT -> 2
            MetadataType.DOUBLE -> 5.8
            MetadataType.BOOLEAN -> true
        }
    }

    @Test
    fun testValues(): Unit = runBlocking {
        db.clearAllTables()
        val session = createTestSession()
        val sessionID = session.sessionID

        // Test basic set use case
        md.addMetadataField("test", MetadataType.STRING)
        val value = "blah"
        md.setValue("test", sessionID, value)
        assertThat(md.getValue("test", sessionID), equalTo(value))

        // Ensure that writing values has no effect unless of the correct type
        for (type in MetadataType.values()) {
            val fieldName = "field_${type.name}"
            md.addMetadataField(fieldName, type)

            md.setString(fieldName, sessionID, getSampleValue(MetadataType.STRING) as String)
            md.setInt(fieldName, sessionID, getSampleValue(MetadataType.INT) as Int)
            md.setDouble(fieldName, sessionID, getSampleValue(MetadataType.DOUBLE) as Double)
            md.setBoolean(fieldName, sessionID, getSampleValue(MetadataType.BOOLEAN) as Boolean)

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

    @Test
    fun testBuiltIn() = runBlocking {
        db.clearAllTables()
        prepopulate(md)
        assertThat(md.getFields(true).size, equalTo(AUTOMOTH_METADATA.size))
        md.addMetadataField("test", MetadataType.BOOLEAN, false)
        assertThat(md.getFields(true).size, equalTo(AUTOMOTH_METADATA.size))
        assertThat(md.getFields(false).size, equalTo(1))

        val session = createTestSession()
        val sessionID = session.sessionID

        md.addMetadataField("builtin", MetadataType.STRING, true)
        md.setString("builtin", sessionID, "something")

        // Attempted overwrite should not succeed, nor should it affect existing values
        md.addMetadataField("builtin", MetadataType.BOOLEAN, false)
        assertThat(md.getField("builtin")?.builtin, equalTo(true))
        assertThat(md.getString("builtin", sessionID), equalTo("something"))
    }
}
