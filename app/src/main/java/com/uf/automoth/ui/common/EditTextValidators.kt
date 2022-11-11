package com.uf.automoth.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText

typealias TextValidator =  (String) -> Boolean

abstract class EditTextValidator(
    private val validate: TextValidator
) : TextWatcher {

    abstract fun onInvalidText()
    abstract fun onValidText()

    override fun afterTextChanged(s: Editable) {
        val text = s.toString()
        if (validate(text)) {
            onValidText()
        } else {
            onInvalidText()
        }
    }

    override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) { }

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) { }
}

class EditTextValidatorWithError(
    validate: TextValidator,
    private val editText: EditText,
    private val errorText: String
) : EditTextValidator(validate) {

    override fun onInvalidText() {
        editText.error = errorText
    }

    override fun onValidText() {
        editText.error = null
    }
}

class EditTextValidatorWithButton(
    validate: TextValidator,
    private val associatedButton: Button
) : EditTextValidator(validate) {

    override fun onInvalidText() {
        associatedButton.isEnabled = false
    }

    override fun onValidText() {
        associatedButton.isEnabled = true
    }
}

fun combineValidators(vararg validators: TextValidator): TextValidator {
    return { string ->
        validators.all { it(string) }
    }
}
