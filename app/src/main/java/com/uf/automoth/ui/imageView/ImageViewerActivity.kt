package com.uf.automoth.ui.imageView

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ActivityImageViewerBinding
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
//        GlideApp.with(this).load(file).into(binding.imageview)
//        binding.imageview.setImageBitmap(BitmapFactory.decodeFile(file.path))
//        binding.imageview.setImageURI(file.toUri())
        binding.imageview.setImageURI(file.toUri())
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
