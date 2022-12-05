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

package com.uf.biolens.utility

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

fun Activity.hideSoftKeyboard() {
    currentFocus?.let { view ->
        ContextCompat.getSystemService(this, InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun Activity.shareSheet(uri: Uri, mimeType: String, title: String) {
    val intent = Intent()
    intent.action = Intent.ACTION_SEND
    intent.type = mimeType
    // See https://stackoverflow.com/questions/57689792/permission-denial-while-sharing-file-with-fileprovider
    intent.clipData = ClipData.newRawUri("", uri)
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    startActivity(Intent.createChooser(intent, title))
}
