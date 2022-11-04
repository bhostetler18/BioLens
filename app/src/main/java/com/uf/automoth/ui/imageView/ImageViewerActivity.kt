package com.uf.automoth.ui.imageView

import android.content.ClipData
import android.content.Intent
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
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ActivityImageViewerBinding
import com.uf.automoth.network.MimeTypes
import com.uf.automoth.ui.common.GlideApp
import com.uf.automoth.ui.common.simpleAlertDialogWithOk
import com.uf.automoth.utility.SHORT_DATE_TIME_FORMATTER
import com.uf.automoth.utility.copyTo
import com.uf.automoth.utility.saveImageToMediaStore
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
            val image = AutoMothRepository.getImage(imageID)
            val session = AutoMothRepository.getSession(sessionID)
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
        this.file = AutoMothRepository.resolve(image, session)
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
                val uri = FileProvider.getUriForFile(this, "com.uf.automoth.fileprovider", tmp)
                shareImageUri(uri)
                true
            }
            R.id.share_crop -> {
                setCropping(true)
                true
            }

            else -> false
        }
    }

    private fun shareImageUri(uri: Uri?) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.type = MimeTypes.JPEG
        // See https://stackoverflow.com/questions/57689792/permission-denial-while-sharing-file-with-fileprovider
        intent.clipData = ClipData.newRawUri("", uri)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
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
            shareImageUri(uri)
        }
        val tmp = File(applicationContext.cacheDir, "export.jpg")
        val uri = FileProvider.getUriForFile(this, "com.uf.automoth.fileprovider", tmp)
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
