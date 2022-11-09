package com.uf.automoth.ui.metadata

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R

class MetadataDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }.create()
    }
}
