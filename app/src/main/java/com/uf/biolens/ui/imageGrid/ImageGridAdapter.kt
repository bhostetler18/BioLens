/*
 * Copyright (c) 2022-2023 University of Florida
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

package com.uf.biolens.ui.imageGrid

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.databinding.ImageGridItemBinding
import com.uf.biolens.ui.common.GlideApp
import com.uf.biolens.ui.imageView.ImageViewerActivity

class ImageGridAdapter(
    val session: Session,
    private val imageSelector: ImageSelector,
    context: Context
) :
    ListAdapter<Image, ImageGridAdapter.ImageViewHolder>(ImageDiffCallback(imageSelector)) {

    private val checked =
        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_checked_circle_24, null)
    private val unchecked =
        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_unchecked_circle_24, null)

    private fun startEditing(image: Image) {
        if (!imageSelector.isEditing) {
            imageSelector.toggleEditing()
            imageSelector.setSelected(image, true)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshEditingState() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageGridAdapter.ImageViewHolder {
        val binding = ImageGridItemBinding.inflate(LayoutInflater.from(parent.context))
        return this.ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageGridAdapter.ImageViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    inner class ImageViewHolder(private val viewBinding: ImageGridItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(image: Image) {
            val file = BioLensRepository.resolve(image, session)
            val thumbnailRequest = GlideApp.with(viewBinding.root.context)
                .load(file)
                .sizeMultiplier(0.1f)
            GlideApp.with(viewBinding.root.context)
                .load(file)
                .thumbnail(thumbnailRequest)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .apply(RequestOptions().override(viewBinding.root.width, 100))
                .into(viewBinding.image)

            setSelected(imageSelector.isSelected(image), false)

            viewBinding.root.setOnClickListener {
                if (imageSelector.isEditing) {
                    toggle(image)
                } else {
                    openViewer(image)
                }
            }

            viewBinding.root.setOnLongClickListener {
                startEditing(image)
                true
            }
        }

        private fun toggle(image: Image) {
            imageSelector.toggle(image)
            setSelected(imageSelector.isSelected(image), true)
        }

        private fun openViewer(image: Image) {
            val ctx = viewBinding.root.context
            val intent = Intent(ctx, ImageViewerActivity::class.java)
            intent.putExtra("IMAGE", image.imageID)
            intent.putExtra("SESSION", session.sessionID)
            ctx.startActivity(intent)
        }

        private fun setSelected(isSelected: Boolean, animated: Boolean) {
            val ic = if (isSelected) checked else unchecked
            viewBinding.checkIcon.setImageDrawable(ic)
            viewBinding.checkIcon.isVisible = imageSelector.isEditing

//            val scale = if (isSelected) 0.95f else 1.0f
//            if (animated) {
//                viewBinding.image.animate().scaleX(scale).scaleY(scale).setDuration(50)
//            } else {
//                viewBinding.image.scaleX = scale
//                viewBinding.image.scaleY = scale
//            }
        }
    }

    class ImageDiffCallback(
        private val imageSelector: ImageSelector?
    ) : DiffUtil.ItemCallback<Image>() {

        override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem.imageID == newItem.imageID &&
                imageSelector?.isSelected(oldItem) == imageSelector?.isSelected(newItem)
        }
    }
}
