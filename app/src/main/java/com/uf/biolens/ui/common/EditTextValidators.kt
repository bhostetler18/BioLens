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

package com.uf.biolens.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText

typealias TextValidator = (String) -> Boolean

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
