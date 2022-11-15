package com.uf.automoth.ui.common

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.databinding.DialogExportOptionsBinding
import com.uf.automoth.utility.SingletonDialog

data class ExportOptions(
    val includeAutoMothMetadata: Boolean,
    val includeUserMetadata: Boolean,
    val metadataOnly: Boolean
) {
    companion object {
        val default = ExportOptions(
            includeAutoMothMetadata = true,
            includeUserMetadata = true,
            metadataOnly = false
        )
    }
}

interface ExportOptionsHandler {
    fun onSelectOptions(options: ExportOptions)
}

class ExportOptionsDialog(
    context: Context,
    private val handler: ExportOptionsHandler,
    @StringRes confirmationText: Int,
    forUpload: Boolean,
    initialOptions: ExportOptions
) : SingletonDialog {
    private val binding: DialogExportOptionsBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = DialogExportOptionsBinding.inflate(inflater)
    }

    private val userMetadataCheckbox = binding.userMetadataCheckbox
    private val automothMetadataCheckbox = binding.automothMetadataCheckbox
    private val metadataOnlyCheckbox = binding.metadataOnlyCheckbox
    private val dialog: AlertDialog

    init {
        binding.metadataOnlyContainer.isVisible = forUpload
        setOptions(initialOptions)

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.export_options)
        dialogBuilder.setView(binding.root)

        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setPositiveButton(confirmationText) { dialog, _ ->
            handler.onSelectOptions(
                ExportOptions(
                    automothMetadataCheckbox.isChecked,
                    userMetadataCheckbox.isChecked,
                    metadataOnlyCheckbox.isChecked
                )
            )
            dialog.dismiss()
        }
        dialog = dialogBuilder.create()
    }

    fun setOptions(options: ExportOptions) {
        automothMetadataCheckbox.isChecked = options.includeAutoMothMetadata
        userMetadataCheckbox.isChecked = options.includeUserMetadata
        metadataOnlyCheckbox.isChecked = options.metadataOnly
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener) =
        dialog.setOnDismissListener(listener)

    override fun show() = dialog.show()
}
