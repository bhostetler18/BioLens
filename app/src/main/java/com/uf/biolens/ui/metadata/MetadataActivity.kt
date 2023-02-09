/*
 * Copyright (c) 2022-2023 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.biolens.ui.metadata

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
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.export.BioLensSessionCSVFormatter
import com.uf.biolens.data.export.SessionCSVExporter
import com.uf.biolens.data.metadata.MetadataType
import com.uf.biolens.databinding.ActivityMetadataEditorBinding
import com.uf.biolens.network.MimeTypes
import com.uf.biolens.ui.common.ExportOptions
import com.uf.biolens.ui.common.ExportOptionsDialog
import com.uf.biolens.ui.common.ExportOptionsHandler
import com.uf.biolens.ui.common.simpleAlertDialogWithOk
import com.uf.biolens.ui.common.simpleAlertDialogWithOkAndCancel
import com.uf.biolens.utility.hideSoftKeyboard
import com.uf.biolens.utility.launchDialog
import com.uf.biolens.utility.shareSheet
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

    private fun createField(name: String, type: MetadataType) {
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
        val uri = FileProvider.getUriForFile(this, "com.uf.biolens.fileprovider", tmp)
        shareSheet(uri, MimeTypes.CSV, getString(R.string.share))
    }

    private suspend fun saveMetadata(options: ExportOptions) {
        saveIfNecessary() // should we ask user if they want to save changed values before exporting?
        val session = BioLensRepository.getSession(sessionID) ?: return
        val folder = BioLensRepository.resolve(session)
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
        val session = BioLensRepository.getSession(sessionID) ?: return false
        val formatter = BioLensSessionCSVFormatter(session).apply {
            configure(options, BioLensRepository.metadataStore)
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
