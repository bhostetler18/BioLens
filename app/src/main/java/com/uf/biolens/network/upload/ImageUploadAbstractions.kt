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

import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.SessionCSVFormatter
import com.uf.biolens.data.export.SessionFilenameProvider

interface ImageUploader {
    suspend fun initialize(session: Session, filenameProvider: SessionFilenameProvider)
    suspend fun uploadImage(image: Image)
    suspend fun uploadMetadata(formatter: SessionCSVFormatter)
}

// TODO: make implementation which can upload images simultaneously
interface ImageUploadBuffer {
    fun start(session: Session, filenameProvider: SessionFilenameProvider)
    fun enqueue(image: Image)
    fun finalize()
    fun cancel()
}

class UploadException(message: String) : Exception(message)
