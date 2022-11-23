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

package com.uf.automoth.ui.metadata

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.uf.automoth.R
import com.uf.automoth.databinding.MetadataBooleanItemBinding
import com.uf.automoth.databinding.MetadataEditableItemBinding
import com.uf.automoth.databinding.MetadataHeaderRowBinding
import com.uf.automoth.databinding.MetadataReadonlyItemBinding

abstract class MetadataRow(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    abstract fun bind(metadata: MetadataTableDataModel)
    abstract fun resetHandlers()

    protected inline fun <reified T : MetadataTableDataModel> requireType(
        metadata: MetadataTableDataModel,
        block: (metadata: T) -> Unit
    ) {
        (metadata as? T)?.let {
            block(it)
        } ?: run {
            resetHandlers() // Don't retain old value handlers and potentially edit the previous metadata object after failing to set a new one
            Log.e(
                TAG,
                "Metadata row '$metadata' requires type ${T::class.simpleName} but received ${metadata.javaClass.simpleName}"
            )
        }
    }

    companion object {
        protected const val TAG = "[METADATA ROW]"
    }
}

class MetadataHeaderRow(context: Context, attrs: AttributeSet?) : MetadataRow(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val binding: MetadataHeaderRowBinding

    init {
        binding =
            MetadataHeaderRowBinding.bind(
                inflate(
                    context,
                    R.layout.metadata_header_row,
                    this
                )
            )
    }

    override fun bind(metadata: MetadataTableDataModel) {
        requireType<MetadataTableDataModel.Header>(metadata) {
            binding.headerTitle.text = it.name
        }
    }

    override fun resetHandlers() {}
}

class ReadonlyMetadataRow(context: Context, attrs: AttributeSet?) : MetadataRow(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val binding: MetadataReadonlyItemBinding

    init {
        binding =
            MetadataReadonlyItemBinding.bind(
                inflate(
                    context,
                    R.layout.metadata_readonly_item,
                    this
                )
            )
    }

    override fun bind(metadata: MetadataTableDataModel) {
        metadata.editable?.let {
            binding.label.text = it.name
            // TODO: gray/italicize value if null
            binding.value.text = it.stringRepresentation(context)
        }
    }

    override fun resetHandlers() {}
}

class BooleanMetadataRow(context: Context, attrs: AttributeSet?) : MetadataRow(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val binding: MetadataBooleanItemBinding

    init {
        binding =
            MetadataBooleanItemBinding.bind(inflate(context, R.layout.metadata_boolean_item, this))
        binding.toggleGroup.addOnButtonCheckedListener(::onSelected)
    }

    private var onChangeValue: ((Boolean?) -> Unit) = {}

    override fun bind(metadata: MetadataTableDataModel) {
        resetHandlers() // otherwise the old onChangeValue gets called when modifying the toggle state
        requireType<MetadataTableDataModel.BooleanMetadata>(metadata) {
            binding.label.text = it.name
            setToggle(it.currentValue)
            onChangeValue = { newValue -> it.setValue(newValue) }
        }
    }

    private fun onSelected(
        @Suppress("UNUSED_PARAMETER")
        group: MaterialButtonToggleGroup,
        toggleId: Int,
        checked: Boolean
    ) {
        if (!checked) {
            return
        }
        when (toggleId) {
            R.id.undefined_toggle -> onChangeValue(null)
            R.id.no_toggle -> onChangeValue(false)
            R.id.yes_toggle -> onChangeValue(true)
        }
    }

    private fun setToggle(value: Boolean?) {
        when (value) {
            true -> binding.toggleGroup.check(R.id.yes_toggle)
            false -> binding.toggleGroup.check(R.id.no_toggle)
            null -> binding.toggleGroup.check(R.id.undefined_toggle)
        }
    }

    override fun resetHandlers() {
        onChangeValue = {}
    }
}

abstract class EditableMetadataRow<T> constructor(context: Context, attrs: AttributeSet?) :
    MetadataRow(context, attrs), TextWatcher, View.OnFocusChangeListener {

    constructor(context: Context) : this(context, null)

    private var binding: MetadataEditableItemBinding
    protected var editText: EditText

    init {
        binding =
            MetadataEditableItemBinding.inflate(LayoutInflater.from(context), this, true)
        editText = binding.editText
        // It would be nice to use the "next" option, but this causes crashes when rows are displayed
        // in a recyclerview. See https://stackoverflow.com/questions/13614101/fatal-crash-focus-search-returned-a-view-that-wasnt-able-to-take-focus
        editText.imeOptions = EditorInfo.IME_ACTION_DONE
    }

    protected abstract var onChangeValue: ((T?) -> Unit)
    protected abstract var validateValue: (T?) -> Boolean
    protected abstract var defaultValue: T?
    protected abstract fun setup(editText: EditText)
    protected abstract fun convert(value: String?): T?

    private fun setHandlers(
        onChangeValue: ((T?) -> Unit),
        validateValue: (T?) -> Boolean,
        defaultValue: T?
    ) {
        this.onChangeValue = onChangeValue
        this.validateValue = validateValue
        this.defaultValue = defaultValue

        // Prevent onTextChanged from triggering onChangeValue while just setting the initial value
        editText.removeTextChangedListener(this)
        editText.onFocusChangeListener = null
        restoreDefaultValue()

        setupEditText() // this is called here rather than in the primary constructor to avoid leaking
        // `this` before the base class is fully initialized. After all, the text watchers don't
        // need to be called anyway until they have something to do...
    }

    fun bindMetadataHandlers(metadata: MetadataValueInterface<T>) {
        setHandlers(
            metadata::setValue,
            metadata.validate,
            metadata.currentValue
        )
    }

    private fun setupEditText() {
        editText.addTextChangedListener(this)
        editText.onFocusChangeListener = this

        editText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
            }
            false
        }

        editText.hint = context.getString(R.string.unknown)
        setup(editText)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        var text = s?.toString()
        if (text == "") {
            text = null
        }
        val new = convert(text)
        if (validateValue(new)) {
            onChangeValue(new)
            editText.error = null
        } else {
            editText.error = context.getString(R.string.invalid_value)
        }
    }

    override fun afterTextChanged(s: Editable?) {}

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        // Don't show an invalid value when the user stops editing
        if (v == editText && !hasFocus && !validateValue(
                convert(editText.text.toString())
            )
        ) {
            restoreDefaultValue()
        }
    }

    private fun restoreDefaultValue() {
        editText.setText(defaultValue?.toString() ?: "")
    }

    override fun bind(metadata: MetadataTableDataModel) {
        binding.label.text = metadata.editable?.name
    }

    override fun resetHandlers() {
        onChangeValue = {}
        validateValue = { true }
        defaultValue = null
    }
}

class StringMetadataRow(context: Context) : EditableMetadataRow<String>(context) {
    override fun convert(value: String?) = value
    override fun setup(editText: EditText) {}
    override var onChangeValue: (String?) -> Unit = { }
    override var validateValue: (String?) -> Boolean = { true }
    override var defaultValue: String? = null

    override fun bind(metadata: MetadataTableDataModel) {
        super.bind(metadata)
        requireType<MetadataTableDataModel.StringMetadata>(metadata) {
            bindMetadataHandlers(it)
        }
    }
}

class IntMetadataRow(context: Context) : EditableMetadataRow<Int>(context) {
    override fun convert(value: String?) = value?.toIntOrNull()
    override fun setup(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override var onChangeValue: (Int?) -> Unit = { }
    override var validateValue: (Int?) -> Boolean = { true }
    override var defaultValue: Int? = null

    override fun bind(metadata: MetadataTableDataModel) {
        super.bind(metadata)
        requireType<MetadataTableDataModel.IntMetadata>(metadata) {
            bindMetadataHandlers(it)
        }
    }

    fun setSigned(allowed: Boolean) {
        if (allowed) {
            editText.inputType = editText.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED
        } else {
            editText.inputType = editText.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED.inv()
        }
    }
}

class DoubleMetadataRow(context: Context) : EditableMetadataRow<Double>(context) {
    override fun convert(value: String?) = value?.toDoubleOrNull()
    override fun setup(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
    }

    override var onChangeValue: (Double?) -> Unit = { }
    override var validateValue: (Double?) -> Boolean = { true }
    override var defaultValue: Double? = null

    override fun bind(metadata: MetadataTableDataModel) {
        super.bind(metadata)
        requireType<MetadataTableDataModel.DoubleMetadata>(metadata) {
            bindMetadataHandlers(it)
        }
    }
}

fun setNumeric() {
    //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            editText.keyListener = DigitsKeyListener.getInstance(getCurrentLocale(context),false,true)
//        } else {
//            editText.keyListener = DigitsKeyListener.getInstance("")
//        }
}
