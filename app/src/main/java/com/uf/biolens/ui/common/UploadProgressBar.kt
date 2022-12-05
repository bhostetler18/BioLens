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
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.uf.biolens.R
import com.uf.biolens.databinding.UploadProgressBarBinding

class UploadProgressBar(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    private val binding: UploadProgressBarBinding

    init {
        this.orientation = VERTICAL
        binding =
            UploadProgressBarBinding.bind(View.inflate(context, R.layout.upload_progress_bar, this))
    }

    val progressLabel = binding.progressLabel
    val button = binding.progressActionButton
    val progressBar = binding.progressBar
    val progressNumeric = binding.progressNumeric

    var hasSetMaxProgress = false
    var maxProgress: Int
        get() = progressBar.max
        set(max) {
            hasSetMaxProgress = true
            if (max != progressBar.max) {
                progressBar.max = max
                setProgress(0)
            }
        }

    fun setProgress(progress: Int) {
        progressBar.progress = progress
        progressNumeric.text = "$progress/${progressBar.max}"
    }

    fun showComplete() {
        setProgress(progressBar.max)
    }

    fun setLabel(text: String) {
        progressLabel.text = text
    }

    fun showNumericProgress(enabled: Boolean) {
        progressNumeric.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    fun showActionButton(enabled: Boolean) {
        button.isVisible = enabled
    }

    fun configureActionButton(text: String, action: () -> Unit) {
        showActionButton(true)
        button.text = text
        button.setOnClickListener {
            action()
        }
    }

    fun reset() {
        maxProgress = 0
        setProgress(0)
        showActionButton(false)
        setLabel("")
    }
}
