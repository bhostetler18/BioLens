package com.uf.automoth.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R

fun simpleAlertDialogWithOk(
    context: Context,
    @StringRes title: Int,
    @StringRes message: Int? = null,
    onOk: (() -> Unit)? = null
): AlertDialog {
    val alertBuilder = MaterialAlertDialogBuilder(context)
    alertBuilder.setTitle(title)
    message?.let {
        alertBuilder.setMessage(it)
    }
    alertBuilder.setPositiveButton(R.string.OK) { dialog, _ ->
        onOk?.invoke()
        dialog.dismiss()
    }
    return alertBuilder.create()
}

fun simpleAlertDialogWithOkAndCancel(
    context: Context,
    @StringRes title: Int,
    @StringRes message: Int? = null,
    onCancel: (() -> Unit)? = null,
    onOk: (() -> Unit)? = null
): AlertDialog {
    val alertBuilder = MaterialAlertDialogBuilder(context)
    alertBuilder.setTitle(title)
    message?.let {
        alertBuilder.setMessage(it)
    }
    alertBuilder.setPositiveButton(R.string.OK) { dialog, _ ->
        onOk?.invoke()
        dialog.dismiss()
    }
    alertBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
        onCancel?.invoke()
        dialog.dismiss()
    }
    return alertBuilder.create()
}

