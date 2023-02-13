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
import com.uf.biolens.data.export.BioLensSessionCSVFormatter
import com.uf.biolens.data.export.SessionFilenameProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

interface ImageUploadQueueListener {
    fun onFinishUpload()
    fun onFailUpload()
    fun onCancelUpload()
}

class ImageUploadQueue(
    private val parentScope: CoroutineScope,
    private val uploader: ImageUploader,
    private val maxFailures: Int = 3,
    private val retryDelay: Long = 5000
) : ImageUploadBuffer {

    private var acceptingImages = AtomicBoolean(false)
    private val queue = ConcurrentLinkedQueue<Image>()

    var uploadJob: Job? = null
    var listener: ImageUploadQueueListener? = null

    override fun start(session: Session, filenameProvider: SessionFilenameProvider) {
        if (uploadJob != null) {
            Log.w(TAG, "Upload queue already started")
            return
        }
        acceptingImages.set(true)
        uploadJob = parentScope.launch {
            if (tryUpload({ uploader.initialize(session, filenameProvider) }, "init session")) {
                uploadLoop(session)
            } else {
                listener?.onFailUpload()
            }
        }
    }

    override fun enqueue(image: Image) {
        queue.add(image)
    }

    private suspend fun uploadLoop(session: Session) = coroutineScope {
        while (isActive) {
            yield()
            val image = queue.peek() ?: continue
            if (tryUpload({ uploader.uploadImage(image) }, "upload image ${image.imageID}")) {
                queue.poll()
            } else {
                onFailure()
                break
            }

            if (!acceptingImages.get() && queue.isEmpty()) {
                onCompletion(session)
                break
            }
        }
    }

    private fun onFailure() {
        queue.clear()
        listener?.onFailUpload()
    }

    private suspend fun onCompletion(session: Session) {
        if (tryUpload(
                { uploader.uploadMetadata(BioLensSessionCSVFormatter(session)) },
                "upload metadata"
            )
        ) {
            listener?.onFailUpload()
        } else {
            listener?.onFinishUpload()
        }
    }

    override fun finalize() {
        acceptingImages.set(false)
    }

    override fun cancel() {
        uploadJob?.cancel()
        queue.clear()
        listener?.onCancelUpload()
    }

    private suspend fun tryUpload(uploadBlock: suspend () -> Unit, name: String): Boolean {
        repeat(maxFailures) {
            try {
                withContext(Dispatchers.IO) {
                    uploadBlock()
                }
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to $name: ${e.localizedMessage}")
            }
            delay(retryDelay)
        }
        return false
    }

    companion object {
        private const val TAG = "[UPLOAD_QUEUE]"
    }
}
