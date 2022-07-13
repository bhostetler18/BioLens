package com.uf.automoth.ui.imaging

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.uf.automoth.R
import com.uf.automoth.databinding.DurationPickerBinding
import java.util.function.Consumer

class IntervalDialog(
    context: Context,
    inflater: LayoutInflater,
    currentInterval: Int,
    private val estimatedImageSize: Double,
    consumer: Consumer<Int>
) {
    private val dialog: AlertDialog
    private val binding: DurationPickerBinding
    init {
        binding = DurationPickerBinding.inflate(inflater)
        setRanges()
        displayInterval(currentInterval)
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle(R.string.choose_interval)
        dialogBuilder.setView(binding.root)

        dialogBuilder.setTitle(R.string.choose_interval)
        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setPositiveButton(R.string.OK) { dialog, _ ->
            consumer.accept(interval)
            dialog.dismiss()
        }
        dialog = dialogBuilder.create()

        setPlurality(context)
        showEstimatedSize(context)

        binding.minutePicker.setOnValueChangedListener { _, _, _ ->
            onPickerChangeValue(context)
        }
        binding.secondPicker.setOnValueChangedListener { _, _, _ ->
            onPickerChangeValue(context)
        }
    }

    private fun setRanges() {
        binding.minutePicker.minValue = 0
        binding.minutePicker.maxValue = 15
        binding.secondPicker.minValue = 0
        binding.secondPicker.maxValue = 59
        binding.minutePicker.wrapSelectorWheel = false
        binding.secondPicker.wrapSelectorWheel = false
    }

    private fun setPlurality(ctx: Context) {
        if (binding.minutePicker.value != 1) {
            binding.minuteText.text = ctx.getString(R.string.unit_minutes_plural)
        } else {
            binding.minuteText.text = ctx.getString(R.string.unit_minutes_singular)
        }
        if (binding.secondPicker.value != 1) {
            binding.secondText.text = ctx.getString(R.string.unit_seconds_plural)
        } else {
            binding.secondText.text = ctx.getString(R.string.unit_seconds_singular)
        }
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

    private fun displayInterval(i: Int) {
        binding.minutePicker.value = i / 60
        binding.secondPicker.value = i % 60
    }

    private fun onPickerChangeValue(ctx: Context) {
        dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = binding.secondPicker.value != 0 ||
            binding.minutePicker.value != 0
        setPlurality(ctx)
        showEstimatedSize(ctx)
    }

    private val interval: Int get() {
        val minutes: Int = binding.minutePicker.value
        val seconds: Int = binding.secondPicker.value
        return 60 * minutes + seconds
    }

    fun show() = dialog.show()
}
