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

package com.uf.biolens.imaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

fun Bitmap.isUnderexposed(
    minBrightness: Float = 0.25f,
    thresh: Float = 0.9f
): Boolean {
    var dimCount = 0
    var brightCount = 0
    val total = (width * height).toFloat()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = getPixel(x, y)
            val hsv = FloatArray(3)
            Color.colorToHSV(pixel, hsv)
            if (hsv[2] < minBrightness) {
                dimCount += 1
            } else {
                brightCount += 1
            }

            // Return early if possible
            if (brightCount / total > 1 - thresh) {
                return false
            }
            if (dimCount / total > thresh) {
                return true
            }
        }
    }
    return dimCount / total > thresh
}

// See https://developer.android.com/topic/performance/graphics/load-bitmap
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun loadDownsampledImage(path: String, w: Int, h: Int): Bitmap? {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, this)
        inSampleSize = calculateInSampleSize(this, w, h)
        inJustDecodeBounds = false
        BitmapFactory.decodeFile(path, this)
    }
}
