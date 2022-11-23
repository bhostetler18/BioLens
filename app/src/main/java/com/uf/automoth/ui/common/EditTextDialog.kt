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

package com.uf.automoth.ui.common

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.databinding.DialogEditTextBinding

class EditTextDialog(
    context: Context,
    title: String? = null,
    hint: String? = null,
    positiveText: String,
    negativeText: String,
    positiveListener: (text: String, dialog: DialogInterface) -> Unit,
    negativeListener: (dialog: DialogInterface) -> Unit = { dialog -> dialog.dismiss() },
    textValidator: (String) -> Boolean
) {
    private val dialog: AlertDialog

    init {
        val inflater = LayoutInflater.from(context)
        val binding = DialogEditTextBinding.inflate(inflater)
        val textEntry = binding.editText
        textEntry.hint = hint
        textEntry.setSingleLine()
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setView(binding.root)
        dialogBuilder.setTitle(title)
        dialogBuilder.setPositiveButton(positiveText) { dialog, _ ->
            positiveListener(textEntry.text.toString(), dialog)
        }
        dialogBuilder.setNegativeButton(negativeText) { dialog, _ ->
            negativeListener(dialog)
        }
        dialog = dialogBuilder.create()
        textEntry.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            textEntry.addTextChangedListener(
                EditTextValidatorWithButton(
                    textValidator,
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                )
            )
        }
    }

    fun show() = dialog.show()
}
