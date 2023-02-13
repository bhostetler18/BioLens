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

package com.uf.biolens.network.upload

import android.util.Log
import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.SessionFilenameProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

interface ImageUploadQueueListener {
    fun onFinishUpload()
    fun onFailUpload()
}

class ImageUploadQueue(
    private val parentScope: CoroutineScope,
    private val uploader: ImageUploader,
    private val maxFailures: Int = 3
) : ImageUploadBuffer {

    private var acceptingImages = AtomicBoolean(false)
    private val queue = ConcurrentLinkedQueue<Image>()

    var uploadJob: Job? = null
    var listener: ImageUploadQueueListener? = null

    override fun start(session: Session, filenameProvider: SessionFilenameProvider) {
        acceptingImages.set(true)
        uploadJob = parentScope.launch {
            withContext(Dispatchers.IO) {
                uploader.initialize(session, filenameProvider)
            }
            uploadLoop()
        }
    }

    override fun enqueue(image: Image) {
        queue.add(image)
    }

    private suspend fun uploadLoop() = coroutineScope {
        var failedAttempts = 0
        while (isActive) {
            queue.peek()?.let {
                if (tryUpload(it)) {
                    queue.poll()
                } else {
                    failedAttempts += 1
                }
            }
            if (!acceptingImages.get() && queue.isEmpty()) {
                onCompletion()
                break
            }
            if (failedAttempts >= maxFailures) {
                onFailure()
                break
            }
            yield()
        }
    }

    private suspend fun tryUpload(image: Image): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            uploader.uploadImage(image)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload image: ${e.localizedMessage}")
            false
        }
    }

    private fun onFailure() {
        listener?.onFailUpload()
        queue.clear()
    }

    private suspend fun onCompletion() {
//        uploader.uploadMetadata()
        listener?.onFinishUpload()
    }

    override fun finalize() {
        acceptingImages.set(false)
    }

    override fun cancel() {
        uploadJob?.cancel()
        queue.clear()
    }

    companion object {
        private const val TAG = "[UPLOAD_QUEUE]"
    }
}
