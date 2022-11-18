package com.uf.automoth.ui.metadata

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uf.automoth.databinding.MetadataDeletableRowBinding

object MetadataViewType {
    const val HEADER = -1
    const val READONLY = 0
    const val STRING = 1
    const val INT = 2
    const val DOUBLE = 3
    const val BOOLEAN = 4
    const val DATE = 5
}

class MetadataAdapter(private val onDelete: (MetadataTableDataModel) -> Unit) :
    ListAdapter<MetadataTableDataModel, MetadataViewHolder>(METADATA_COMPARATOR) {

    private var _isInDeleteMode = true
    var allowDeletion: Boolean
        set(value) {
            _isInDeleteMode = value
            rebindAll()
        }
        get() {
            return _isInDeleteMode
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetadataViewHolder {
        val ctx = parent.context
        val rowView = when (viewType) {
            MetadataViewType.HEADER -> MetadataHeaderRow(ctx)
            MetadataViewType.READONLY -> ReadonlyMetadataRow(ctx)
            MetadataViewType.STRING -> StringMetadataRow(ctx)
            MetadataViewType.INT -> IntMetadataRow(ctx)
            MetadataViewType.DOUBLE -> DoubleMetadataRow(ctx)
            MetadataViewType.BOOLEAN -> BooleanMetadataRow(ctx)
            MetadataViewType.DATE -> ReadonlyMetadataRow(ctx) // will make a date picker
            else -> ReadonlyMetadataRow(ctx)
        }
        rowView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return MetadataViewHolder.newInstance(parent, rowView)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        if (item is EditableMetadataInterface && item.readonly) {
            return MetadataViewType.READONLY
        }
        return when (item) {
            is MetadataTableDataModel.Header -> MetadataViewType.HEADER
            is MetadataTableDataModel.StringMetadata -> MetadataViewType.STRING
            is MetadataTableDataModel.IntMetadata -> MetadataViewType.INT
            is MetadataTableDataModel.DoubleMetadata -> MetadataViewType.DOUBLE
            is MetadataTableDataModel.BooleanMetadata -> MetadataViewType.BOOLEAN
            is MetadataTableDataModel.DateMetadata -> MetadataViewType.DATE
        }
    }

    override fun onBindViewHolder(holder: MetadataViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        if (item is EditableMetadataInterface) {
            holder.showRemoveButton(_isInDeleteMode && item.editable?.isDeletable() == true)
            holder.onRemovePressed = {
                onDelete(item)
            }
        } else {
            holder.showRemoveButton(false)
            holder.onRemovePressed = null
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun rebindAll() {
        notifyDataSetChanged()
    }

    companion object {
        private val METADATA_COMPARATOR = object : DiffUtil.ItemCallback<MetadataTableDataModel>() {
            override fun areItemsTheSame(
                oldItem: MetadataTableDataModel,
                newItem: MetadataTableDataModel
            ): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(
                oldItem: MetadataTableDataModel,
                newItem: MetadataTableDataModel
            ): Boolean {
                // It would be nice to compare actual values, but type-erasure makes that difficult
                return false
            }
        }
    }
}

class MetadataViewHolder private constructor(
    private val container: MetadataDeletableRowBinding,
    private val row: MetadataRow
) : RecyclerView.ViewHolder(container.root) {
    var onRemovePressed: (() -> Unit)? = null

    init {
        container.removeButton.setOnClickListener {
            onRemovePressed?.invoke()
        }
    }

    fun bind(metadata: MetadataTableDataModel) {
        row.bind(metadata)
    }

    fun showRemoveButton(visible: Boolean) {
        container.removeButton.isVisible = visible
    }

    companion object {
        fun newInstance(parent: ViewGroup, row: MetadataRow): MetadataViewHolder {
            val binding = MetadataDeletableRowBinding.inflate(LayoutInflater.from(parent.context))
            binding.removeButton.isVisible = false
            binding.contentWrapper.addView(row)
            binding.root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return MetadataViewHolder(binding, row)
        }
    }
}
