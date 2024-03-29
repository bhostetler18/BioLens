/*
 * Copyright (c) 2023 University of Florida
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

package com.uf.biolens

import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.SessionCSVFormatter
import com.uf.biolens.data.export.SessionFilenameProvider
import com.uf.biolens.network.upload.ImageUploadQueueListener
import com.uf.biolens.network.upload.SessionUploadQueue
import com.uf.biolens.network.upload.SessionUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.OffsetDateTime

class SessionUploadQueueTests {

    @Test
    fun testUpload() = runBlocking {
        val uploader = TestUploader()
        val queue = SessionUploadQueue(this, uploader)
        val session = Session("", "", OffsetDateTime.now(), null, null, 1)
        queue.start(session, DummyFilenameProvider())
        queue.enqueue(testImage(0))
        queue.enqueue(testImage(1))
        queue.finalize()
        queue.uploadJob?.join()
        assert(uploader.uploadedImageIDs == hashSetOf(0L, 1L))
    }

    @Test
    fun testCancel() = runBlocking {
        val uploader = TestUploader()
        val queue = SessionUploadQueue(this, uploader, retryDelay = 0)
        val session = Session("", "", OffsetDateTime.now(), null, null, 1)
        queue.start(session, DummyFilenameProvider())
        queue.enqueue(testImage(0))
        queue.enqueue(testImage(1))
        delay(2000)
        queue.cancel()
        queue.enqueue(testImage(2))
        queue.uploadJob?.join()
        assert(uploader.uploadedImageIDs == hashSetOf(0L, 1L))
    }

    @Test
    fun testCoroutineScoping() = runBlocking {
        val scope = CoroutineScope(coroutineContext + Job())
        val uploader = TestUploader()
        val queue = SessionUploadQueue(scope, uploader, retryDelay = 0)
        val session = Session("", "", OffsetDateTime.now(), null, null, 1)
        queue.start(session, DummyFilenameProvider())
        queue.enqueue(testImage(0))
        queue.enqueue(testImage(1))
        delay(2000)
        scope.cancel()
        queue.enqueue(testImage(2))
        queue.uploadJob?.join()
        assert(uploader.uploadedImageIDs == hashSetOf(0L, 1L))
    }

    @Test
    fun testFailure() = runBlocking {
        var uploader = TestUploader(true, 3)
        var queue = SessionUploadQueue(this, uploader, 4, retryDelay = 0)
        val session = Session("", "", OffsetDateTime.now(), null, null, 1)
        queue.start(session, DummyFilenameProvider())
        queue.enqueue(testImage(0))
        queue.enqueue(testImage(1))
        queue.finalize()
        queue.uploadJob?.join()
        assert(uploader.uploadedImageIDs == hashSetOf(0L, 1L))

        uploader = TestUploader(true, 4)
        queue = SessionUploadQueue(this, uploader, 4)
        var failed = false
        var finished = false
        queue.listener = object : ImageUploadQueueListener {
            override fun onFailUpload(session: Session?) {
                failed = true
            }

            override fun onCancelUpload(session: Session?) {
                TODO("Not yet implemented")
            }

            override fun onFinishUpload(session: Session?) {
                finished = true
            }
        }
        queue.start(session, DummyFilenameProvider())
        queue.enqueue(testImage(0))
        queue.finalize()
        queue.uploadJob?.join()
        assert(uploader.uploadedImageIDs.isEmpty())
        assert(failed)
        assert(!finished)
    }

    private fun testImage(id: Long) = Image(
        id.toInt(),
        "",
        OffsetDateTime.now(),
        0,
        id
    )
}

class TestUploader(
    private val shouldFail: Boolean = false,
    private val numFails: Int = 0
) : SessionUploader {
    var hasUploadedMetadata: Boolean = false
    var uploadedImageIDs = HashSet<Long>()
    private var failCount = 0

    override suspend fun initialize(
        session: Session,
        filenameProvider: SessionFilenameProvider
    ) {
        println("Initializing session ${session.sessionID}")
    }

    override suspend fun uploadImage(image: Image) {
        if (shouldFail && failCount < numFails) {
            failCount += 1
            throw Exception("Failed to upload")
        }
        println("Uploading image ${image.imageID}...")
        delay(500)
        uploadedImageIDs.add(image.imageID)
        println("Image ${image.imageID} uploaded")
    }

    override suspend fun uploadMetadata(formatter: SessionCSVFormatter) {
        println("Uploaded metadata")
        hasUploadedMetadata = true
    }
}

class DummyFilenameProvider : SessionFilenameProvider {
    override val uniqueSessionId: String = "test_session"
    override val sessionDirectory: String = "test_session_folder"

    override fun getUniqueImageId(imageIndex: Int): String {
        return "test_img$imageIndex"
    }
}
