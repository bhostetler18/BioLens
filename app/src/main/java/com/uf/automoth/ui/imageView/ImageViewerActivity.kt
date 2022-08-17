package com.uf.automoth.ui.imageView

import android.content.ClipData
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
import com.uf.automoth.utility.saveImageToMediaStore
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var session: Session? = null
    private var image: Image? = null
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        setContentView(binding.root)

        lifecycleScope.launch {
            // TODO: show loading indicator while this runs?
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
        setShareVisible(true)
        val file = AutoMothRepository.resolve(image, session)
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

        supportActionBar?.title = dateFormatter.format(image.timestamp)
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
                val file = AutoMothRepository.resolve(image!!, session!!)
                lifecycleScope.launch {
                    val name = session!!.name + "_" + image!!.filename
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
                val file = AutoMothRepository.resolve(image!!, session!!)
                val uri = FileProvider.getUriForFile(this, "com.uf.automoth.fileprovider", file)
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.type = MimeTypes.JPEG
                // See https://stackoverflow.com/questions/57689792/permission-denial-while-sharing-file-with-fileprovider
                intent.clipData = ClipData.newRawUri("", uri)
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(Intent.createChooser(intent, getString(R.string.share)))
                true
            }

            else -> false
        }
    }

    private fun setShareVisible(enabled: Boolean) {
        menu?.setGroupVisible(R.id.share_group, enabled)
    }

    private fun displayNoImage() {
        // TODO: Implement image/session not found error screen
        setShareVisible(false)
    }

    companion object {
        val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
}
