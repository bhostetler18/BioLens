package com.uf.automoth.utility

import android.content.DialogInterface
import android.view.View

interface SingletonDialog {
    fun show()
    fun setOnDismissListener(listener: DialogInterface.OnDismissListener)
}

// Prevents rapid clicks from opening a dialog multiple times
fun launchDialog(dialog: SingletonDialog, source: View) {
    source.isEnabled = false
    dialog.setOnDismissListener {
        source.isEnabled = true
    }
    dialog.show()
}
