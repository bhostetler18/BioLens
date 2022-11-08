package com.uf.automoth.ui.metadata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.databinding.ActivityMetadataEditorBinding
import kotlinx.coroutines.launch


class MetadataActivity : AppCompatActivity() {

    private var metadata: List<Metadata>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionID = intent.extras?.get("SESSION") as? Long ?: run {
//            displayError()
            return
        }

        lifecycleScope.launch {
            initialize(sessionID)
        }
    }

    private suspend fun initialize(sessionID: Long) {
        val session = AutoMothRepository.getSession(sessionID) ?: run {
//            runOnUiThread { displayError() }
            return@initialize
        }

        val metadata = getMetadata(session, this)
        val binding = ActivityMetadataEditorBinding.inflate(layoutInflater)
        binding.recyclerView.adapter = MetadataAdapter(metadata)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        setContentView(binding.root)
        this.metadata = metadata
    }

    suspend fun saveChanges() {
        metadata?.forEach {
            it.writeValue()
        }
        // TODO: rewrite metadata file
    }
}
