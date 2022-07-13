package com.uf.automoth.ui.common

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.uf.automoth.databinding.DialogEditTextBinding

class EditTextDialog(
    context: Context,
    inflater: LayoutInflater,
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
        val binding = DialogEditTextBinding.inflate(inflater)
        val textEntry = binding.editText
        textEntry.hint = hint
        textEntry.setSingleLine()
        val dialogBuilder = AlertDialog.Builder(context)
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
        }
        textEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = textValidator(s.toString())
            }

            override fun afterTextChanged(s: Editable?) { }
        })
    }

    fun show() = dialog.show()
}
