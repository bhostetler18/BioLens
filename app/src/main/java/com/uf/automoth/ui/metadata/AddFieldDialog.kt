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

package com.uf.automoth.ui.metadata

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.data.metadata.MetadataType
import com.uf.automoth.databinding.DialogAddMetadataFieldBinding
import com.uf.automoth.ui.common.EditTextValidatorWithButton
import com.uf.automoth.ui.common.combineValidators
import com.uf.automoth.utility.SingletonDialog

class AddFieldDialog(
    context: Context,
    onCreateField: (String, MetadataType) -> Unit
) : SingletonDialog, AdapterView.OnItemSelectedListener {
    private val binding: DialogAddMetadataFieldBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = DialogAddMetadataFieldBinding.inflate(inflater)
    }

    private val editText = binding.editText
    private val spinner = binding.spinner
    private val dialog: AlertDialog

    private var type = MetadataType.STRING

    init {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.add_custom_metadata_field)
        dialogBuilder.setMessage(R.string.custom_metadata_field_description)
        dialogBuilder.setView(binding.root)

        spinner.onItemSelectedListener = this

        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setPositiveButton(R.string.add) { dialog, _ ->
            onCreateField(editText.text.toString(), type)
            dialog.dismiss()
        }
        dialog = dialogBuilder.create()
        dialog.setOnShowListener {
            val addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            addButton.isEnabled = false
            editText.addTextChangedListener(
                EditTextValidatorWithButton(
                    combineValidators(::csvValidator, ::fieldNameValidator),
                    addButton
                )
            )
        }
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener) =
        dialog.setOnDismissListener(listener)

    override fun show() = dialog.show()

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val options = dialog.context.resources.getStringArray(R.array.metadata_types)
        type = toType(options[position])
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun toType(spinnerOption: String): MetadataType {
        return when (spinnerOption) {
            "Text" -> MetadataType.STRING
            "Integer" -> MetadataType.INT
            "Decimal" -> MetadataType.DOUBLE
            "Boolean" -> MetadataType.BOOLEAN
            else -> MetadataType.STRING
        }
    }
}
