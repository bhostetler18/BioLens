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

package com.uf.biolens.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.annotation.IntegerRes
import com.uf.biolens.R
import com.uf.biolens.databinding.ImageSelectorBarBinding

interface ImageSelectorListener {
    fun onDeletePressed()
    fun onExitPressed()
    fun onMoreOptionPressed(@IntegerRes identifier: Int)
}

class ImageSelectorBar(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs), PopupMenu.OnMenuItemClickListener {

    constructor(context: Context) : this(context, null)

    private val binding: ImageSelectorBarBinding
    var listener: ImageSelectorListener? = null

    init {
        binding =
            ImageSelectorBarBinding.bind(inflate(context, R.layout.image_selector_bar, null))
        addView(binding.root)
        binding.deleteButton.setOnClickListener {
            listener?.onDeletePressed()
        }
        binding.exitButton.setOnClickListener {
            listener?.onExitPressed()
        }
        binding.moreButton.setOnClickListener {
            val popup = PopupMenu(context, binding.moreButton)
            popup.menuInflater.inflate(R.menu.image_select_menu, popup.menu)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return listener?.let {
            it.onMoreOptionPressed(item.itemId)
            true
        } ?: false
    }

    fun setNumImagesSelected(num: Int) {
        binding.imageSelectionText.text = resources.getString(R.string.n_images_selected, num)
    }
}
