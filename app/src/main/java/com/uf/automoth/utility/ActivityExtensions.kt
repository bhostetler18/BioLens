package com.uf.automoth.utility

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
