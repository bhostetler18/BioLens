package com.uf.automoth.ui.metadata

import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.databinding.ActivityMetadataEditorBinding
import com.uf.automoth.ui.common.simpleAlertDialogWithOkAndCancel
import com.uf.automoth.utility.hideSoftKeyboard
import com.uf.automoth.utility.indexOfFirstOrNull
import com.uf.automoth.utility.launchDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MetadataActivity : AppCompatActivity() {

    private lateinit var viewModel: MetadataViewModel
    private val binding by lazy { ActivityMetadataEditorBinding.inflate(layoutInflater) }
    private val adapter = MetadataAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionID = intent.extras?.get("SESSION") as? Long ?: run {
//            displayError()
            return
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        setSupportActionBar(binding.appBar.toolbar)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.metadata)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

        viewModel.allMetadata.observe(this) {
            adapter.submitList(it)
        }

        viewModel.isDirty.observe(this) {
            binding.saveButton.isEnabled = it
        }

        binding.fab.setOnClickListener {
            val dialog = AddFieldDialog(this, this::createField)
            launchDialog(dialog, binding.fab)
        }

        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.saveChanges()
                // Force EditTexts to rebind and get the new default value for use when resetting
                // after an invalid value is entered. Otherwise, they would reset to the value that
                // was set when the metadata editor was first opened, which would be confusing
                ';'
                adapter.rebindAll()
            }
        }
    }

    private fun createField(name: String, type: UserMetadataType) {
        lifecycleScope.launch {
            // TODO: show warning if already exists
            AutoMothRepository.metadataStore.addMetadataField(name, type)
            delay(200) // kind of hacky, but not worth the synchronization currently
            viewModel.allMetadata.value?.indexOfFirstOrNull {
                (it as? EditableMetadataInterface)?.name == name
            }?.let {
                binding.recyclerView.smoothScrollToPosition(it)
            }
        }
    }

    fun deleteField(metadata: EditableMetadataInterface) {
        if (!metadata.deletable) {
            return
        }
        lifecycleScope.launch {
            AutoMothRepository.metadataStore.deleteMetadataField(metadata.name)
        }
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

    override fun onBackPressed() {
        if (viewModel.isDirty.value == true) {
            simpleAlertDialogWithOkAndCancel(
                this,
                R.string.warn_exit_metadata,
                R.string.warn_unsaved_changes,
                onOk = { super.onBackPressed() }
            ).show()
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            (currentFocus as? EditText)?.let {
                val rect = Rect()
                it.getGlobalVisibleRect(rect)
                if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    hideSoftKeyboard()
                    it.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
