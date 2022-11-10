package com.uf.automoth.ui.metadata

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

object MetadataViewType {
    const val READONLY = 0
    const val STRING = 1
    const val INT = 2
    const val DOUBLE = 3
    const val BOOLEAN = 4
    const val DATE = 5
}

class MetadataAdapter(private val metadata: List<DisplayableMetadata>) :
    RecyclerView.Adapter<MetadataViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetadataViewHolder {
        val ctx = parent.context
        val rowView = when (viewType) {
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
        return MetadataViewHolder(rowView)
    }

    override fun getItemViewType(position: Int): Int {
        val item = metadata[position]
        if (item.readonly) {
            return MetadataViewType.READONLY
        }
        return when (item) {
            is DisplayableMetadata.StringMetadata -> MetadataViewType.STRING
            is DisplayableMetadata.IntMetadata -> MetadataViewType.INT
            is DisplayableMetadata.DoubleMetadata -> MetadataViewType.DOUBLE
            is DisplayableMetadata.BooleanMetadata -> MetadataViewType.BOOLEAN
            is DisplayableMetadata.DateMetadata -> MetadataViewType.DATE
        }
    }

    override fun onBindViewHolder(holder: MetadataViewHolder, position: Int) {
        holder.bind(metadata[position])
    }

    override fun getItemCount(): Int {
        return metadata.size
    }
}

class MetadataViewHolder(private val view: MetadataRow) : RecyclerView.ViewHolder(view) {
    fun bind(metadata: DisplayableMetadata) {
        view.bind(metadata)
    }
}
