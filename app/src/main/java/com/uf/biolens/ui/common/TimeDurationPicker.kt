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

package com.uf.biolens.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.PluralsRes
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
        binding =
            GenericTimePickerBinding.bind(View.inflate(context, R.layout.generic_time_picker, this))
    }

    private val unit1Picker = binding.unit1Picker
    private val unit2Picker = binding.unit2Picker

    @PluralsRes
    private var unit1: Int = R.plurals.unit_minutes

    @PluralsRes
    private var unit2: Int = R.plurals.unit_seconds
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

    val unit1Value: Int
        get() {
            return unit1Picker.value
        }
    val unit2Value: Int
        get() {
            return unit2Picker.value
        }

    fun setUnits(
        @PluralsRes unit1: Int,
        @PluralsRes unit2: Int
    ) {
        this.unit1 = unit1
        this.unit2 = unit2
        setPlurality()
    }

    fun setUnits(mode: DurationMode) {
        when (mode) {
            DurationMode.HHMM -> {
                setUnits(
                    R.plurals.unit_hours,
                    R.plurals.unit_minutes
                )
            }

            DurationMode.MMSS -> {
                setUnits(
                    R.plurals.unit_minutes,
                    R.plurals.unit_seconds
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
        binding.unit1Label.text = resources.getQuantityText(unit1, unit1Picker.value)
        binding.unit2Label.text = resources.getQuantityText(unit2, unit2Picker.value)
    }
}
