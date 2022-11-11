package com.uf.automoth.ui.metadata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.databinding.ActivityMetadataEditorBinding
import com.uf.automoth.utility.launchDialog
import kotlinx.coroutines.launch

class MetadataActivity : AppCompatActivity() {

    // TODO: viewmodel
    private var metadata: List<DisplayableMetadata>? = null

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

        val metadata = getDefaultMetadata(session, this) + getUserMetadata(
            session,
            AutoMothRepository.metadataStore
        )
        val binding = ActivityMetadataEditorBinding.inflate(layoutInflater)
        val adapter = MetadataAdapter(metadata)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        binding.fab.setOnClickListener {
            val dialog = AddFieldDialog(this, layoutInflater, this::createField)
            launchDialog(dialog, binding.fab)
        }
        setContentView(binding.root)
        this.metadata = metadata
    }

    private fun createField(name: String, type: UserMetadataType) {
        lifecycleScope.launch {
            // TODO: show warning if already exists
            AutoMothRepository.metadataStore.addMetadataField(name, type)
            // Reload UI and metadata list (maybe have livedata or flow that fetches metadata list
            // automatically and feeds it to recyclerview?
        }
    }

    suspend fun saveChanges() {
        metadata?.forEach {
            if (!it.readonly) {
                it.writeValue()
            }
        }
        // TODO: rewrite metadata file
    }
}
