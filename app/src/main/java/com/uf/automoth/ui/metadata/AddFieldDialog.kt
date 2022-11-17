package com.uf.automoth.ui.metadata

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.databinding.DialogAddMetadataFieldBinding
import com.uf.automoth.ui.common.EditTextValidatorWithButton
import com.uf.automoth.ui.common.combineValidators
import com.uf.automoth.utility.SingletonDialog

class AddFieldDialog(
    context: Context,
    onCreateField: (String, UserMetadataType) -> Unit
) : SingletonDialog, AdapterView.OnItemSelectedListener {
    private val binding: DialogAddMetadataFieldBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = DialogAddMetadataFieldBinding.inflate(inflater)
    }

    private val editText = binding.editText
    private val spinner = binding.spinner
    private val dialog: AlertDialog

    private var type = UserMetadataType.STRING

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

    private fun toType(spinnerOption: String): UserMetadataType {
        return when (spinnerOption) {
            "Text" -> UserMetadataType.STRING
            "Integer" -> UserMetadataType.INT
            "Decimal" -> UserMetadataType.DOUBLE
            "Boolean" -> UserMetadataType.BOOLEAN
            else -> UserMetadataType.STRING
        }
    }
}
