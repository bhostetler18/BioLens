package com.uf.automoth.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.uf.automoth.R
import com.uf.automoth.databinding.GenericTimePickerBinding

class TimeDurationPicker(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    enum class DurationMode {
        HHMM, MMSS
    }

    private val binding: GenericTimePickerBinding

    init {
        binding = GenericTimePickerBinding.bind(View.inflate(context, R.layout.generic_time_picker, this))
    }

    private val unit1Picker = binding.unit1Picker
    private val unit2Picker = binding.unit2Picker
    private var unit1Singular = ""
    private var unit1Plural = ""
    private var unit2Singular = ""
    private var unit2Plural = ""
    private var onTimeDurationChangedListener: ((value1: Int, value2: Int) -> Unit)? = null

    init {
        unit1Picker.wrapSelectorWheel = false
        unit2Picker.wrapSelectorWheel = false
        unit1Picker.setOnValueChangedListener { _, _, _ ->
            onTimeDurationChangedListener?.invoke(unit1Picker.value, unit2Picker.value)
            setPlurality()
        }
        unit2Picker.setOnValueChangedListener { _, _, _ ->
            onTimeDurationChangedListener?.invoke(unit1Picker.value, unit2Picker.value)
            setPlurality()
        }
    }

    val unit1Value: Int get() { return unit1Picker.value }
    val unit2Value: Int get() { return unit2Picker.value }

    fun setUnits(
        unit1Singular: String,
        unit1Plural: String,
        unit2Singular: String,
        unit2Plural: String
    ) {
        this.unit1Singular = unit1Singular
        this.unit1Plural = unit1Plural
        this.unit2Singular = unit2Singular
        this.unit2Plural = unit2Plural
        setPlurality()
    }

    fun setUnits(mode: DurationMode) {
        when (mode) {
            DurationMode.HHMM -> {
                setUnits(
                    context.getString(R.string.unit_hours_singular),
                    context.getString(R.string.unit_hours_plural),
                    context.getString(R.string.unit_minutes_singular),
                    context.getString(R.string.unit_minutes_plural)
                )
            }

            DurationMode.MMSS -> {
                setUnits(
                    context.getString(R.string.unit_minutes_singular),
                    context.getString(R.string.unit_minutes_plural),
                    context.getString(R.string.unit_seconds_singular),
                    context.getString(R.string.unit_seconds_plural)
                )
            }
        }
    }

    fun setOnTimeDurationChangedListener(listener: (value1: Int, value2: Int) -> Unit) {
        onTimeDurationChangedListener = listener
    }

    fun setUnit1Range(min: Int, max: Int) {
        unit1Picker.minValue = min
        unit1Picker.maxValue = max
    }

    fun setUnit2Range(min: Int, max: Int) {
        unit2Picker.minValue = min
        unit2Picker.maxValue = max
    }

    fun setValues(value1: Int, value2: Int) {
        unit1Picker.value = value1
        unit2Picker.value = value2
        setPlurality()
    }

    private fun setPlurality() {
        if (unit1Picker.value == 1) {
            binding.unit1Label.text = unit1Singular
        } else {
            binding.unit1Label.text = unit1Plural
        }
        if (unit2Picker.value == 1) {
            binding.unit2Label.text = unit2Singular
        } else {
            binding.unit2Label.text = unit2Plural
        }
    }
}
