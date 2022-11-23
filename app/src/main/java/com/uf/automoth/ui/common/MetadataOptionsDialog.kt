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
    val metadataOnly: Boolean = false
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
