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

package com.uf.biolens.ui.imageView

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.databinding.ActivityImageViewerBinding
import com.uf.biolens.network.MimeTypes
import com.uf.biolens.ui.common.GlideApp
import com.uf.biolens.ui.common.simpleAlertDialogWithOk
import com.uf.biolens.utility.SHORT_DATE_TIME_FORMATTER
import com.uf.biolens.utility.copyTo
import com.uf.biolens.utility.saveImageToMediaStore
import com.uf.biolens.utility.shareSheet
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var session: Session
    private lateinit var image: Image
    private lateinit var file: File
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        binding.photoView.setScaleLevels(1f, 2f, 10f)
        binding.cropButton.setOnClickListener {
            exportCurrentRegion()
        }
        binding.cancelCropButton.setOnClickListener {
            setCropping(false)
        }
        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        setContentView(binding.root)

        lifecycleScope.launch {
            val imageID = intent.extras?.get("IMAGE") as? Long ?: -1
            val sessionID = intent.extras?.get("SESSION") as? Long ?: -1
            val image = BioLensRepository.getImage(imageID)
            val session = BioLensRepository.getSession(sessionID)
            if (image != null && session != null) runOnUiThread {
                setData(image, session)
            } else runOnUiThread {
                displayNoImage()
            }
        }
    }

    private fun setData(image: Image, session: Session) {
        this.image = image
        this.session = session
        this.file = BioLensRepository.resolve(image, session)
        setShareMenuVisible(true)
        GlideApp.with(this)
            .load(file)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.photoView.setImageDrawable(resource)
                    return true
                }
            })
            .into(binding.photoView)

        supportActionBar?.title = image.timestamp.format(SHORT_DATE_TIME_FORMATTER)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_share_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.save -> {
                lifecycleScope.launch {
                    val name = session.name + "_" + image.filename
                    // TODO: do we want gallery images to have location EXIF as well?
                    val success = saveImageToMediaStore(file, name, MimeTypes.JPEG, contentResolver)
                    runOnUiThread {
                        val text =
                            if (success) getString(R.string.image_saved_success) else getString(R.string.image_saved_failure)
                        Toast.makeText(this@ImageViewerActivity, text, Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            R.id.share -> {
                val tmp = File(applicationContext.cacheDir, "export.jpg")
                file.copyTo(tmp)
                setExif(tmp.toUri())
                val uri = FileProvider.getUriForFile(this, "com.uf.biolens.fileprovider", tmp)
                shareSheet(uri, MimeTypes.JPEG, getString(R.string.share))
                true
            }
            R.id.share_crop -> {
                setCropping(true)
                true
            }

            else -> false
        }
    }

    private fun setShareMenuVisible(enabled: Boolean) {
        menu?.setGroupVisible(R.id.share_group, enabled)
    }

    private fun setCropping(enabled: Boolean) {
        if (enabled && binding.cropImageView.imageUri == null) {
            binding.cropImageView.setImageUriAsync(file.toUri())
        }
        binding.cropContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.photoView.visibility = if (enabled) View.GONE else View.VISIBLE
        setShareMenuVisible(!enabled)
    }

    private fun exportCurrentRegion() {
        binding.cropImageView.setOnCropImageCompleteListener { _, result ->
            val uri = result.uriContent ?: run {
                simpleAlertDialogWithOk(this, R.string.image_crop_failure).show()
                return@setOnCropImageCompleteListener
            }

            setExif(uri)
            shareSheet(uri, MimeTypes.JPEG, getString(R.string.share))
        }
        val tmp = File(applicationContext.cacheDir, "export.jpg")
        val uri = FileProvider.getUriForFile(this, "com.uf.biolens.fileprovider", tmp)
        binding.cropImageView.croppedImageAsync(customOutputUri = uri)
    }

    private fun setExif(uri: Uri) {
        if (session.hasLocation()) {
            contentResolver.openFileDescriptor(uri, "rw")?.use {
                try {
                    val exif = ExifInterface(it.fileDescriptor)
                    exif.setLatLong(session.latitude!!, session.longitude!!)
                    exif.saveAttributes()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to write EXIF tags: {${e.localizedMessage}")
                }
            }
        }
    }

    private fun displayNoImage() {
        simpleAlertDialogWithOk(
            this,
            R.string.image_not_found,
            R.string.application_data_corrupted
        ) {
            finish()
        }.show()
        setShareMenuVisible(false)
    }

    companion object {
        private const val TAG = "[IMAGE VIEWER]"
    }
}
