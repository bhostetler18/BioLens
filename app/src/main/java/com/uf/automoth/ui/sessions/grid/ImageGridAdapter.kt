package com.uf.automoth.ui.sessions.grid

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ImageGridItemBinding
import com.uf.automoth.ui.common.GlideApp
import com.uf.automoth.ui.sessions.ImageViewerActivity

class ImageGridAdapter(val session: Session) :
    ListAdapter<Image, ImageGridAdapter.ImageViewHolder>(DiffCallback) {

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
            val file = AutoMothRepository.resolve(image, session)
            GlideApp.with(viewBinding.root.context)
                .load(file)
                .thumbnail(0.5f)
                .into(viewBinding.image)
            viewBinding.image.setOnClickListener {
                val ctx = viewBinding.root.context
                val intent = Intent(ctx, ImageViewerActivity::class.java)
                intent.putExtra("IMAGE", image.imageID)
                intent.putExtra("SESSION", session.sessionID)
                ctx.startActivity(intent)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Image>() {
        override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem.imageID == newItem.imageID
        }
    }
}
