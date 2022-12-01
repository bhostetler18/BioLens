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

package com.uf.biolens.ui.imaging

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.databinding.DialogIntervalPickerBinding
import com.uf.biolens.ui.common.TimeDurationPicker
import com.uf.biolens.utility.SingletonDialog
import java.util.function.Consumer

class IntervalDialog(
    context: Context,
    currentInterval: Int,
    private val estimatedImageSize: Double,
    consumer: Consumer<Int>
) : SingletonDialog {
    private val binding: DialogIntervalPickerBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = DialogIntervalPickerBinding.inflate(inflater)
    }

    private val durationPicker = binding.durationPicker
    private val dialog: AlertDialog

    init {
        durationPicker.setUnits(TimeDurationPicker.DurationMode.MMSS)
        durationPicker.setUnit1Range(0, 15)
        durationPicker.setUnit2Range(0, 59)
        displayInterval(currentInterval, context)
        durationPicker.setOnTimeDurationChangedListener { minutes, seconds ->
            onPickerChangeValue(context, minutes, seconds)
        }

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.interval_dialog_title)
        dialogBuilder.setView(binding.root)

        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setPositiveButton(R.string.OK) { dialog, _ ->
            consumer.accept(interval)
            dialog.dismiss()
        }
        dialog = dialogBuilder.create()
    }

    private fun showEstimatedSize(ctx: Context) {
        if (interval == 0) {
            binding.sizeEstimate.visibility = View.INVISIBLE
            return
        } else {
            binding.sizeEstimate.visibility = View.VISIBLE
        }
        val bytesPerHour = (estimatedImageSize * 3600.0 / interval).toLong()
        val sizeString = android.text.format.Formatter.formatShortFileSize(ctx, bytesPerHour)
        binding.sizeEstimate.text = "~$sizeString " + ctx.getString(R.string.per_hour)
    }

    private fun displayInterval(i: Int, ctx: Context) {
        durationPicker.setValues(i / 60, i % 60)
        showEstimatedSize(ctx)
    }

    private fun onPickerChangeValue(ctx: Context, minutes: Int, seconds: Int) {
        val isValid = (minutes != 0 || seconds != 0)
        dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = isValid
        showEstimatedSize(ctx)
    }

    private val interval: Int
        get() {
            val minutes: Int = durationPicker.unit1Value
            val seconds: Int = durationPicker.unit2Value
            return 60 * minutes + seconds
        }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener) =
        dialog.setOnDismissListener(listener)

    override fun show() = dialog.show()
}
