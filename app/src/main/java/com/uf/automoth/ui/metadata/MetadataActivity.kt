package com.uf.automoth.ui.metadata

import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.export.AutoMothSessionCSVFormatter
import com.uf.automoth.data.export.SessionCSVExporter
import com.uf.automoth.data.metadata.UserMetadataType
import com.uf.automoth.databinding.ActivityMetadataEditorBinding
import com.uf.automoth.network.MimeTypes
import com.uf.automoth.ui.common.ExportOptions
import com.uf.automoth.ui.common.ExportOptionsDialog
import com.uf.automoth.ui.common.ExportOptionsHandler
import com.uf.automoth.ui.common.simpleAlertDialogWithOk
import com.uf.automoth.ui.common.simpleAlertDialogWithOkAndCancel
import com.uf.automoth.utility.hideSoftKeyboard
import com.uf.automoth.utility.launchDialog
import com.uf.automoth.utility.shareSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MetadataActivity : AppCompatActivity() {

    private lateinit var viewModel: MetadataViewModel
    private val binding by lazy { ActivityMetadataEditorBinding.inflate(layoutInflater) }
    private val adapter = MetadataAdapter(onDelete = ::deleteField)
    private var menu: Menu? = null
    private var sessionID: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionID = intent.extras?.get("SESSION") as? Long ?: run {
//            displayError()
            return
        }

        viewModel = ViewModelProvider(
            this@MetadataActivity,
            MetadataViewModel.Factory(sessionID, applicationContext)
        )[MetadataViewModel::class.java]

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
            }
        }
    }

    private fun createField(name: String, type: UserMetadataType) {
        lifecycleScope.launch {
            val index = viewModel.addUserField(name, type)
            delay(100)
            runOnUiThread {
                if (index != null) {
                    binding.recyclerView.smoothScrollToPosition(index)
                } else {
                    simpleAlertDialogWithOk(
                        this@MetadataActivity,
                        R.string.warn_fail_to_add_metadata
                    ).show()
                }
            }
        }
    }

    private fun deleteField(item: MetadataTableDataModel) {
        simpleAlertDialogWithOkAndCancel(
            this,
            R.string.delete_metadata_dialog_title,
            R.string.warn_delete_metadata,
            onOk = {
                lifecycleScope.launch {
                    viewModel.removeUserField(item)
                }
            }
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.metadata_menu, menu)
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
                val dialog = ExportOptionsDialog(
                    this,
                    object : ExportOptionsHandler {
                        override fun onSelectOptions(options: ExportOptions) {
                            lifecycleScope.launch {
                                saveMetadata(options)
                            }
                        }
                    },
                    R.string.save,
                    false,
                    ExportOptions.default
                )
                launchDialog(dialog, item.actionView)
                true
            }
            R.id.share -> {
                val dialog = ExportOptionsDialog(
                    this,
                    object : ExportOptionsHandler {
                        override fun onSelectOptions(options: ExportOptions) {
                            lifecycleScope.launch {
                                shareMetadata(options)
                            }
                        }
                    },
                    R.string.share,
                    false,
                    ExportOptions.default
                )
                launchDialog(dialog, item.actionView)
                true
            }
            else -> false
        }
    }

    private suspend fun saveIfNecessary() {
        if (viewModel.isDirty.value == true) {
            viewModel.saveChanges()
        }
    }

    private suspend fun shareMetadata(options: ExportOptions) {
        saveIfNecessary() // should we ask user if they want to save changed values before exporting?
        val tmp = File(applicationContext.cacheDir, "metadata.csv")
        writeMetadata(tmp, sessionID, options)
        val uri = FileProvider.getUriForFile(this, "com.uf.automoth.fileprovider", tmp)
        shareSheet(uri, MimeTypes.CSV, getString(R.string.share))
    }

    private suspend fun saveMetadata(options: ExportOptions) {
        saveIfNecessary() // should we ask user if they want to save changed values before exporting?
        val session = AutoMothRepository.getSession(sessionID) ?: return
        val folder = AutoMothRepository.resolve(session)
        val exportLocation = File(folder, "metadata.csv")
        if (writeMetadata(exportLocation, sessionID, options)) {
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.metadata_saved)
                .setMessage(getString(R.string.metadata_filesystem_explanation, session.directory))
                .setPositiveButton(R.string.OK) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            runOnUiThread {
                dialog.show()
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, R.string.failed_to_save_metadata, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        warnIfDirty(R.string.warn_exit_metadata, R.string.warn_unsaved_changes) {
            super.onBackPressed()
        }
    }

    private fun warnIfDirty(
        @StringRes title: Int,
        @StringRes message: Int,
        onContinue: () -> Unit
    ) = runOnUiThread {
        if (viewModel.isDirty.value == true) {
            simpleAlertDialogWithOkAndCancel(
                this,
                title,
                message,
                onOk = { onContinue() }
            ).show()
        } else {
            onContinue()
        }
    }

    private suspend fun writeMetadata(
        file: File,
        sessionID: Long,
        options: ExportOptions
    ): Boolean {
        val session = AutoMothRepository.getSession(sessionID) ?: return false
        val formatter = AutoMothSessionCSVFormatter(session).apply {
            configure(options, AutoMothRepository.metadataStore)
        }
        val exporter = SessionCSVExporter(formatter)
        runCatching {
            exporter.export(session, file)
        }.onSuccess {
            return true
        }.onFailure {
            return false
        }
        return false
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

    companion object {
        private const val TAG = "[METADATA_ACTIVITY]"
    }
}
