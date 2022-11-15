package com.uf.automoth.ui.metadata

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

class MetadataAdapter :
    ListAdapter<DisplayableMetadata, MetadataViewHolder>(METADATA_COMPARATOR) {

    private var _isInDeleteMode = false
    var allowDeletion: Boolean
        set(value) {
            _isInDeleteMode = value
            notifyDataSetChanged()
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
        if (item.readonly && item !is DisplayableMetadata.Header) {
            return MetadataViewType.READONLY
        }
        return when (item) {
            is DisplayableMetadata.Header -> MetadataViewType.HEADER
            is DisplayableMetadata.StringMetadata -> MetadataViewType.STRING
            is DisplayableMetadata.IntMetadata -> MetadataViewType.INT
            is DisplayableMetadata.DoubleMetadata -> MetadataViewType.DOUBLE
            is DisplayableMetadata.BooleanMetadata -> MetadataViewType.BOOLEAN
            is DisplayableMetadata.DateMetadata -> MetadataViewType.DATE
        }
    }

    override fun onBindViewHolder(holder: MetadataViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.showRemoveButton(_isInDeleteMode && item.deletable)
        holder.onRemovePressed = {
        }
    }

    companion object {
        private val METADATA_COMPARATOR = object : DiffUtil.ItemCallback<DisplayableMetadata>() {
            override fun areItemsTheSame(
                oldItem: DisplayableMetadata,
                newItem: DisplayableMetadata
            ): Boolean {
                return oldItem.name == newItem.name &&
                    oldItem.readonly == newItem.readonly &&
                    oldItem.javaClass == newItem.javaClass
            }

            override fun areContentsTheSame(
                oldItem: DisplayableMetadata,
                newItem: DisplayableMetadata
            ): Boolean {
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

    fun bind(metadata: DisplayableMetadata) {
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
