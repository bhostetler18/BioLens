package com.uf.automoth.ui.sessions.grid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.databinding.ActivityImageGridBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

class ImageGridActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityImageGridBinding.inflate(layoutInflater)

        val sessionID = intent.extras?.get("SESSION") as? Long ?: return
        val session = runBlocking(Dispatchers.IO) {
            AutoMothRepository.getSession(sessionID)
        }

        val sessionDirectory = File(AutoMothRepository.storageLocation, session.directory)
        val adapter = ImageGridAdapter(sessionDirectory)
        binding.imageGrid.adapter = adapter

        AutoMothRepository.getImagesInSession(sessionID).observe(this) { sessions ->
            sessions?.let { adapter.submitList(it) }
        }

        setSupportActionBar(binding.toolbar)
        setContentView(binding.root)
        supportActionBar?.title = session.name
    }
}