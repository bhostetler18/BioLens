package com.uf.automoth.ui.metadata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.databinding.ActivityMetadataEditorBinding
import com.uf.automoth.utility.launchDialog
import kotlinx.coroutines.launch

class MetadataActivity : AppCompatActivity() {

    private lateinit var viewModel: MetadataViewModel

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

        viewModel = ViewModelProvider(
            this@MetadataActivity,
            MetadataViewModel.Factory(session, applicationContext)
        )[MetadataViewModel::class.java]

        val binding = ActivityMetadataEditorBinding.inflate(layoutInflater)
        val adapter = MetadataAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        viewModel.allMetadata.observe(this) {
            adapter.submitList(it)
        }

        binding.fab.setOnClickListener {
            val dialog = AddFieldDialog(this, this::createField)
            launchDialog(dialog, binding.fab)
        }
        setContentView(binding.root)
    }

    private fun createField(name: String, type: UserMetadataType) {
        lifecycleScope.launch {
            // TODO: show warning if already exists
            AutoMothRepository.metadataStore.addMetadataField(name, type)
            // Reload UI and metadata list (maybe have livedata or flow that fetches metadata list
            // automatically and feeds it to recyclerview?
        }
    }

    fun deleteField(metadata: DisplayableMetadata) {
        if (!metadata.deletable) {
            return
        }
        lifecycleScope.launch {
            AutoMothRepository.metadataStore.deleteMetadataField(metadata.name)
        }
    }

    suspend fun saveChanges() {
        viewModel.allMetadata.value?.forEach {
            if (!it.readonly) {
                it.writeValue()
            }
        }
        // TODO: rewrite metadata file
    }
}
