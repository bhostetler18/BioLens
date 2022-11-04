package com.uf.automoth.ui.imageGrid

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ActivityImageGridBinding
import com.uf.automoth.network.GoogleDriveUploadWorker
import com.uf.automoth.network.GoogleSignInHelper
import com.uf.automoth.ui.common.EditTextDialog
import com.uf.automoth.ui.common.simpleAlertDialogWithOk
import kotlinx.coroutines.launch

class ImageGridActivity : AppCompatActivity() {
    private lateinit var viewModel: ImageGridViewModel
    private val binding by lazy { ActivityImageGridBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.progressBar.isVisible = false

        val sessionID = intent.extras?.get("SESSION") as? Long ?: run {
            displayError()
            return
        }

        lifecycleScope.launch {
            initialize(sessionID)
        }
    }

    private suspend fun initialize(sessionID: Long) {
        val session = AutoMothRepository.getSession(sessionID) ?: run {
            runOnUiThread { displayError() }
            return@initialize
        }

        viewModel = ViewModelProvider(
            this@ImageGridActivity,
            ImageGridViewModel.ImageGridViewModelFactory(session)
        )[ImageGridViewModel::class.java]

        val adapter = ImageGridAdapter(session)
        binding.imageGrid.adapter = adapter

        viewModel.images.observe(this@ImageGridActivity) { images ->
            images?.let { adapter.submitList(it) }
        }

        viewModel.displayCounts.observe(this@ImageGridActivity) {
            val total = it.first
            val skip = it.second
            val imageString = resources.getQuantityString(R.plurals.unit_images, total)
            var text = "$total $imageString"
            if (skip != 0) {
                text += " ${getString(R.string.showing_every_x, skip)}"
            }
            binding.imgCount.text = text
        }

        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(
            GoogleDriveUploadWorker.uniqueWorkerTag(session)
        ).observe(this) { infoList: List<WorkInfo>? ->
            if (infoList != null && infoList.isNotEmpty()) {
                showUploadProgress(infoList[0])
            } else {
                showUploadProgress(null)
            }
        }

        setSupportActionBar(binding.appBar.toolbar)
        setContentView(binding.root)
        supportActionBar?.title = session.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun displayError() {
        simpleAlertDialogWithOk(
            this,
            R.string.session_not_found,
            R.string.application_data_corrupted
        ) {
            finish()
        }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_grid_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.rename -> {
                renameCurrentSession()
                true
            }
            R.id.upload -> {
                uploadSession()
                true
            }
            R.id.delete -> {
                deleteCurrentSession()
                true
            }
            R.id.show_all_images -> {
                viewModel.skipCount.value = 0
                item.isChecked = true
                true
            }
            R.id.show_every_10 -> {
                viewModel.skipCount.value = 10
                item.isChecked = true
                true
            }
            R.id.show_every_25 -> {
                viewModel.skipCount.value = 25
                item.isChecked = true
                true
            }
            R.id.show_every_100 -> {
                viewModel.skipCount.value = 100
                item.isChecked = true
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun renameCurrentSession() {
        val editDialog = EditTextDialog(
            this,
            layoutInflater,
            title = getString(R.string.rename_session),
            hint = viewModel.session.name,
            positiveText = getString(R.string.rename),
            negativeText = getString(R.string.cancel),
            positiveListener = { text, dialog ->
                AutoMothRepository.renameSession(viewModel.session.sessionID, text)
                supportActionBar?.title = text
                dialog.dismiss()
            },
            textValidator = Session::isValid
        )
        editDialog.show()
    }

    private fun uploadSession() {
        val account = GoogleSignInHelper.getGoogleAccountIfValid(this)?.account
        if (account != null) {
            val workRequest = OneTimeWorkRequestBuilder<GoogleDriveUploadWorker>()
                .setInputData(
                    workDataOf(
                        GoogleDriveUploadWorker.KEY_SESSION_ID to viewModel.session.sessionID,
                        GoogleDriveUploadWorker.KEY_ACCOUNT_EMAIL to account.name,
                        GoogleDriveUploadWorker.KEY_ACCOUNT_TYPE to account.type
                    )
                ).setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(this).enqueueUniqueWork(
                GoogleDriveUploadWorker.uniqueWorkerTag(viewModel.session),
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        } else {
            simpleAlertDialogWithOk(
                this,
                R.string.no_google_account,
                R.string.please_sign_in
            ).show()
        }
    }

    private fun deleteCurrentSession() {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.setTitle(getString(R.string.warn_delete_session))
        dialogBuilder.setMessage(getString(R.string.warn_permanent_action))
        dialogBuilder.setPositiveButton(getString(R.string.delete)) { dialog, _ ->
            AutoMothRepository.delete(viewModel.session)
            dialog.dismiss()
            this.finish()
        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

    private fun showUploadProgress(info: WorkInfo?) {
        if (info == null) {
            binding.progressBar.isVisible = false
            return
        }

        binding.progressBar.isVisible = true

        val maxProgress =
            info.progress.keyValueMap[GoogleDriveUploadWorker.KEY_MAX_PROGRESS] as? Int
        maxProgress?.let {
            binding.progressBar.maxProgress = it
        }

        when (info.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                binding.progressBar.setLabel(getString(R.string.waiting_internet))
                binding.progressBar.configureActionButton(getString(R.string.cancel)) {
                    WorkManager.getInstance(this).cancelUniqueWork(
                        GoogleDriveUploadWorker.uniqueWorkerTag(viewModel.session)
                    )
                }
            }
            WorkInfo.State.RUNNING -> {
                if (info.progress.keyValueMap.containsKey(GoogleDriveUploadWorker.KEY_METADATA)) {
                    binding.progressBar.setLabel(getString(R.string.exporting_metadata))
                } else if (info.progress.keyValueMap.containsKey(GoogleDriveUploadWorker.KEY_PROGRESS)) {
                    val progress =
                        info.progress.keyValueMap[GoogleDriveUploadWorker.KEY_PROGRESS] as? Int
                    binding.progressBar.setLabel(getString(R.string.uploading_images))
                    progress?.let {
                        binding.progressBar.setProgress(it)
                    }
                } else {
                    binding.progressBar.setLabel(getString(R.string.connecting_to_drive))
                }

                binding.progressBar.configureActionButton(getString(R.string.cancel)) {
                    WorkManager.getInstance(this).cancelUniqueWork(
                        GoogleDriveUploadWorker.uniqueWorkerTag(viewModel.session)
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                binding.progressBar.setLabel(getString(R.string.upload_complete))
                // The Result.Success() WorkInfo update may occur before the final update to KEY_PROGRESS,
                // so just force the progress bar to show fully complete
                binding.progressBar.setProgress(binding.progressBar.maxProgress)
                binding.progressBar.configureActionButton(getString(R.string.dismiss)) {
                    dismissAndResetUploadBar()
                }
            }
            WorkInfo.State.CANCELLED -> {
                binding.progressBar.setLabel(getString(R.string.upload_cancelled))
                binding.progressBar.configureActionButton(getString(R.string.dismiss)) {
                    dismissAndResetUploadBar()
                }
            }
            WorkInfo.State.FAILED -> {
                binding.progressBar.setLabel(getString(R.string.upload_failed))
                binding.progressBar.configureActionButton(getString(R.string.retry)) {
                    uploadSession()
                }
            }
        }
    }

    private fun dismissAndResetUploadBar() {
        WorkManager.getInstance(this).pruneWork() // Makes the LiveData update with a null WorkInfo
        binding.progressBar.isVisible = false
        binding.progressBar.reset()
    }
}
