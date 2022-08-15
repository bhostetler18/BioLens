package com.uf.automoth.ui.sessions

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.databinding.ActivityImageViewerBinding
import com.uf.automoth.ui.common.GlideApp
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val imageID = intent.extras?.get("IMAGE") as? Long ?: return
        val binding = ActivityImageViewerBinding.inflate(layoutInflater)

        val file = File(AutoMothRepository.storageLocation, "session_2022_08_05_12_38_37_6710/img1.jpg")
//        GlideApp.with(this).load(file).into(binding.imageview)
//        binding.imageview.setImageBitmap(BitmapFactory.decodeFile(file.path))
//        binding.imageview.setImageURI(file.toUri())
        binding.imageview.setImageResource(R.drawable.ic_launcher_foreground)

        setContentView(binding.root)


    }
}
