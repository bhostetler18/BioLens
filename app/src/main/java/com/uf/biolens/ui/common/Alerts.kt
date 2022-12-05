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

package com.uf.biolens.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.biolens.R

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
