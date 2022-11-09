package com.uf.automoth.ui.metadata

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.uf.automoth.R
import com.uf.automoth.databinding.MetadataBooleanItemBinding
import com.uf.automoth.databinding.MetadataEditableItemBinding
import com.uf.automoth.databinding.MetadataReadonlyItemBinding

abstract class MetadataRow(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    abstract fun bind(metadata: Metadata)
    abstract fun resetHandlers()

    inline fun <reified T : Metadata> requireType(metadata: Metadata, block: (metadata: T) -> Unit) {
        (metadata as? T)?.let {
            block(it)
        } ?: run {
            resetHandlers() // Don't retain old value handlers and potentially edit the previous metadata object after failing to set a new one
            Log.e(
                "[METADATA]",
                "Metadata row '${metadata.name}' requires type ${T::class.simpleName} but received ${metadata.javaClass.simpleName}"
            )
        }
    }
}

class ReadonlyMetadataRow(context: Context, attrs: AttributeSet?) : MetadataRow(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val binding: MetadataReadonlyItemBinding
    init {
        binding =
            MetadataReadonlyItemBinding.bind(inflate(context, R.layout.metadata_readonly_item, this))
    }

    override fun bind(metadata: Metadata) {
        binding.label.text = metadata.name
        // TODO: gray/italicize value if null
        binding.value.text = metadata.stringRepresentation(context)
    }

    override fun resetHandlers() { }
}

class BooleanMetadataRow(context: Context, attrs: AttributeSet?) : MetadataRow(context, attrs) {
    constructor(context: Context) : this(context, null) private val binding: MetadataBooleanItemBinding
    init {
        binding =
            MetadataBooleanItemBinding.bind(inflate(context, R.layout.metadata_boolean_item, this))
        binding.toggle.setOnCheckedChangeListener { _, isChecked ->
            onChangeValue(isChecked)
        }
    }

    private var onChangeValue: ((Boolean?) -> Unit) = {}

    override fun bind(metadata: Metadata) {
        requireType<Metadata.BooleanMetadata>(metadata) {
            binding.label.text = it.name
            binding.toggle.isChecked = it.value ?: false
            onChangeValue = { newValue -> it.value = newValue }
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
    }

    protected abstract var onChangeValue: ((T?) -> Unit)
    protected abstract var validateValue: (T?) -> Boolean
    protected abstract var defaultValue: T?
    protected abstract fun setup(editText: EditText)
    protected abstract fun convert(value: String): T?

    fun setHandlers(
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

    private fun setupEditText() {
        editText.addTextChangedListener(this)
        editText.onFocusChangeListener = this
        editText.hint = context.getString(R.string.unknown)
        setup(editText)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val new = convert(s?.toString() ?: "")
        if (validateValue(new)) {
            onChangeValue(new)
        }
    }

    override fun afterTextChanged(s: Editable?) { }

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

    override fun bind(metadata: Metadata) {
        binding.label.text = metadata.name
    }

    override fun resetHandlers() {
        onChangeValue = {}
        validateValue = { true }
        defaultValue = null
    }
}

class StringMetadataRow(context: Context) : EditableMetadataRow<String>(context) {
    override fun convert(value: String) = value
    override fun setup(editText: EditText) { }
    override var onChangeValue: (String?) -> Unit = { }
    override var validateValue: (String?) -> Boolean = { true }
    override var defaultValue: String? = null

    override fun bind(metadata: Metadata) {
        super.bind(metadata)
        requireType<Metadata.StringMetadata>(metadata) {
            setHandlers({ newValue -> it.value = newValue }, it.validate, it.value)
        }
    }
}

class IntMetadataRow(context: Context) : EditableMetadataRow<Int>(context) {
    override fun convert(value: String) = value.toIntOrNull()
    override fun setup(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override var onChangeValue: (Int?) -> Unit = { }
    override var validateValue: (Int?) -> Boolean = { true }
    override var defaultValue: Int? = null

    override fun bind(metadata: Metadata) {
        super.bind(metadata)
        requireType<Metadata.IntMetadata>(metadata) {
            setHandlers({ newValue -> it.value = newValue }, it.validate, it.value)
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
    override fun convert(value: String) = value.toDoubleOrNull()
    override fun setup(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    override var onChangeValue: (Double?) -> Unit = { }
    override var validateValue: (Double?) -> Boolean = { true }
    override var defaultValue: Double? = null

    override fun bind(metadata: Metadata) {
        super.bind(metadata)
        requireType<Metadata.DoubleMetadata>(metadata) {
            setHandlers({ newValue -> it.value = newValue }, it.validate, it.value)
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
