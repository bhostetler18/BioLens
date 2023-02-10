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

package com.uf.biolens.imaging

import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UnderexposedImageFinder(val sessionID: Long) {

    var onCompleteListener: (() -> Unit)? = null
    var underexposedImageHandler: ((Image) -> Unit)? = null
    var progressListener: ((Int) -> Unit)? = null

    suspend fun getUnderexposedImages() = coroutineScope {
        val session = BioLensRepository.getSession(sessionID) ?: return@coroutineScope
        BioLensRepository.getImagesInSession(sessionID).forEachIndexed { index, image ->
            val file = BioLensRepository.resolve(image, session)
            val underexposed = withContext(Dispatchers.IO) {
                return@withContext loadDownsampledImage(file.absolutePath, 50, 50)?.let { bitmap ->
                    val result = bitmap.isUnderexposed()
                    bitmap.recycle()
                    return@let result
                } ?: false
            }
            if (underexposed) {
                launch(Dispatchers.Main) {
                    ensureActive()
                    underexposedImageHandler?.invoke(image)
                }
            }
            progressListener?.invoke(index)
        }
        onCompleteListener?.invoke()
    }
}
