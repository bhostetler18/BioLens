package com.uf.automoth.ui.imaging

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.databinding.DialogAutoStopBinding
import com.uf.automoth.databinding.GenericNumberEntryBinding
import com.uf.automoth.ui.common.TimeDurationPicker

class AutoStopDialog(
    context: Context,
    inflater: LayoutInflater,
    private val currentSettings: ImagingSettings,
    private val estimatedImageSize: Double,
    onSetHandler: (AutoStopMode, Int?) -> Unit
) {
    private val binding: DialogAutoStopBinding

    init {
        binding = DialogAutoStopBinding.inflate(inflater)
    }

    private val dialog: AlertDialog
    private val toggleGroup = binding.toggleGroup
    private val optionsContainer = binding.optionsContainer
    private val imageCountBinding: GenericNumberEntryBinding
    private val timePicker: TimeDurationPicker
    private val offText: TextView

    init {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.autostop_dialog_title)
        dialogBuilder.setMessage("Automatically stop the session after...")
        dialogBuilder.setView(binding.root)

        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setPositiveButton(R.string.OK) { dialog, _ ->
            onSetHandler(getAutoStopMode(), getAutoStopValue())
            dialog.dismiss()
        }
        dialog = dialogBuilder.create()
        dialog.setOnShowListener {
            /* This prevents a bug in which opening this dialog when the auto-stop mode is "OFF"
            would cause the software keyboard not to show upon switching to a different mode and
            trying to edit the value. See the link below for more:
            https://stackoverflow.com/questions/9102074/android-edittext-in-dialog-doesnt-pull-up-soft-keyboard
             */
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        imageCountBinding = GenericNumberEntryBinding.inflate(inflater)
        imageCountBinding.root.gravity = Gravity.CENTER
        timePicker = TimeDurationPicker(dialog.context)
        timePicker.setUnits(TimeDurationPicker.DurationMode.HHMM)
        timePicker.setUnit1Range(0, 48)
        timePicker.setUnit2Range(0, 59)
        offText = TextView(dialog.context)
        offText.gravity = Gravity.CENTER
        offText.text = context.getString(R.string.auto_stop_off_description)

        imageCountBinding.numberEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateImageCount()
                setImageCountPlurality()
                showEstimatedSize()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        timePicker.setOnTimeDurationChangedListener { _, _ ->
            validateTimeSelection()
            showEstimatedSize()
        }

        toggleGroup.addOnButtonCheckedListener(this::toggleButtonSelected)
        setAutoStopMode(currentSettings.autoStopMode)
        setAutoStopValue(currentSettings.autoStopValue)
        showEstimatedSize()
    }

    private fun setAutoStopMode(mode: AutoStopMode) {
        when (mode) {
            AutoStopMode.IMAGE_COUNT -> toggleGroup.check(binding.imageToggle.id)
            AutoStopMode.TIME -> toggleGroup.check(binding.timeToggle.id)
            AutoStopMode.OFF -> toggleGroup.check(binding.offToggle.id)
        }
    }

    private fun getAutoStopMode(): AutoStopMode {
        return when (toggleGroup.checkedButtonId) {
            binding.imageToggle.id -> AutoStopMode.IMAGE_COUNT
            binding.timeToggle.id -> AutoStopMode.TIME
            binding.offToggle.id -> AutoStopMode.OFF
            else -> AutoStopMode.OFF
        }
    }

    private fun setAutoStopValue(value: Int) {
        when (getAutoStopMode()) {
            AutoStopMode.IMAGE_COUNT -> {
                imageCountBinding.numberEntry.setText(value.toString())
            }
            AutoStopMode.TIME -> {
                val hours = value / 60
                val minutes = value % 60
                timePicker.setValues(hours, minutes)
            }
            AutoStopMode.OFF -> {}
        }
    }

    private fun getAutoStopValue(): Int? {
        return when (getAutoStopMode()) {
            AutoStopMode.IMAGE_COUNT -> imageCountBinding.numberEntry.text.toString().toIntOrNull()
            AutoStopMode.TIME -> 60 * timePicker.unit1Value + timePicker.unit2Value
            else -> null
        }
    }

    private fun showEstimatedSize() {
        when (getAutoStopMode()) {
            AutoStopMode.IMAGE_COUNT -> {
                val count = getAutoStopValue()
                if (count != null) {
                    binding.sizeEstimate.visibility = View.VISIBLE
                    displaySize(count)
                } else {
                    binding.sizeEstimate.visibility = View.INVISIBLE
                }
            }
            AutoStopMode.TIME -> {
                val minutes = getAutoStopValue()!!
                val numImages = 60 * minutes / currentSettings.interval
                displaySize(numImages)
            }
            AutoStopMode.OFF -> binding.sizeEstimate.visibility = View.INVISIBLE
        }
    }

    private fun displaySize(numImages: Int) {
        if (numImages == 0) {
            binding.sizeEstimate.visibility = View.INVISIBLE
        } else {
            binding.sizeEstimate.visibility = View.VISIBLE
            val ctx = binding.root.context
            val bytes = (numImages * estimatedImageSize).toLong()
            val sizeString = android.text.format.Formatter.formatShortFileSize(ctx, bytes)
            binding.sizeEstimate.text = "~$sizeString " + ctx.getString(R.string.per_session)
        }
    }

    private fun toggleButtonSelected(
        group: MaterialButtonToggleGroup,
        index: Int,
        checked: Boolean
    ) {
        if (!checked) {
            return
        }
        when (index) {
            binding.imageToggle.id -> {
                optionsContainer.removeAllViews()
                optionsContainer.addView(imageCountBinding.root)
                validateImageCount()
                setImageCountPlurality()
            }
            binding.timeToggle.id -> {
                optionsContainer.removeAllViews()
                optionsContainer.addView(timePicker)
                validateTimeSelection()
            }
            binding.offToggle.id -> {
                optionsContainer.removeAllViews()
                optionsContainer.addView(offText)
                enableOK()
            }
        }
        showEstimatedSize()
    }

    private fun validateTimeSelection() {
        val isValid = timePicker.unit1Value != 0 || timePicker.unit2Value != 0
        dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = isValid
    }

    private fun validateImageCount() {
        val value = imageCountBinding.numberEntry.text.toString().toIntOrNull()
        val isValid = (value != null) && value > 0
        dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = isValid
    }

    private fun enableOK() {
        dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = true
    }

    // TODO: refactor this (and other generic number picker methods) into a custom view
    private fun setImageCountPlurality() {
        val ctx = imageCountBinding.root.context
        val count = imageCountBinding.numberEntry.text.toString().toIntOrNull()
        if (count == 1) {
            imageCountBinding.unitLabel.text = ctx.getString(R.string.image_singular)
        } else {
            imageCountBinding.unitLabel.text = ctx.getString(R.string.image_plural)
        }
    }

    fun show() = dialog.show()
}
