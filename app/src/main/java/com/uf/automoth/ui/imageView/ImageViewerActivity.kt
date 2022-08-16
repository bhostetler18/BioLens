package com.uf.automoth.ui.imageView

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ActivityImageViewerBinding
import com.uf.automoth.ui.common.GlideApp
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var session: Session? = null
    private var image: Image? = null

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
                displayError()
            }
        }
    }

    private fun setData(image: Image, session: Session) {
        this.image = image
        this.session = session
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }

    private fun displayError() {
        TODO("Implement image/session not found error screen")
    }

    companion object {
        val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
}
