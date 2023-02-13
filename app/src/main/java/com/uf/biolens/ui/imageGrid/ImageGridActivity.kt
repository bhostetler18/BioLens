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

package com.uf.biolens.ui.imageGrid

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.databinding.ActivityImageGridBinding
import com.uf.biolens.imaging.UnderexposedImageFinder
import com.uf.biolens.network.GoogleSignInHelper
import com.uf.biolens.network.upload.GoogleDriveUploadWorker
import com.uf.biolens.network.upload.SessionUploadWorker
import com.uf.biolens.ui.common.ExportOptions
import com.uf.biolens.ui.common.ExportOptionsDialog
import com.uf.biolens.ui.common.ExportOptionsHandler
import com.uf.biolens.ui.common.ImageSelectorListener
import com.uf.biolens.ui.common.simpleAlertDialogWithOk
import com.uf.biolens.ui.metadata.MetadataActivity
import com.uf.biolens.utility.launchDialog
import com.uf.biolens.utility.setItemEnabled
import com.uf.biolens.utility.setPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class ImageGridActivity : AppCompatActivity(), ImageSelectorListener {
    private var sessionID by Delegates.notNull<Long>()
    private val binding by lazy { ActivityImageGridBinding.inflate(layoutInflater) }

    private lateinit var viewModel: ImageGridViewModel
    private lateinit var adapter: ImageGridAdapter

    private var selectUnderexposedJob: Job? = null
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.uploadProgressBar.isVisible = false
        binding.progressBar.isVisible = false

        sessionID = intent.extras?.get("SESSION") as? Long ?: run {
            displayError()
            return
        }

        viewModel = ViewModelProvider(
            this@ImageGridActivity,
            ImageGridViewModel.ImageGridViewModelFactory(sessionID)
        )[ImageGridViewModel::class.java]

        lifecycleScope.launch {
            initialize(sessionID)
        }
    }

    private suspend fun initialize(sessionID: Long) {
        val session = BioLensRepository.getSession(sessionID) ?: run {
            runOnUiThread { displayError() }
            return@initialize
        }

        // If the session was stopped by the device running out of battery or crashing (and
        // therefore not shutdown fully in ImagingManager::stop), ensure that the completed date is
        // still set when the user goes to view/export this session
        if (session.completed == null) {
            session.completed = BioLensRepository.updateSessionCompletion(sessionID)
        }

        adapter = ImageGridAdapter(session, viewModel.imageSelector, this)
        binding.imageGrid.adapter = adapter
        setSupportActionBar(binding.appBar.toolbar)
        setContentView(binding.root)
        supportActionBar?.title = session.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.images.observe(this@ImageGridActivity) { images ->
            images?.let { adapter.submitList(it) }
        }

        viewModel.session.observe(this@ImageGridActivity) {
            supportActionBar?.title = it?.name
        }

        viewModel.imageSelector.isEditingLiveData.observe(this@ImageGridActivity) { isEditing ->
            cancelSelectingUnderexposed()
            showSelectionTools(isEditing)
            adapter.refreshEditingState()
            menu?.setItemEnabled(R.id.upload, !isEditing)
        }

        viewModel.displayCounts.observe(this@ImageGridActivity) {
            val imageString = resources.getQuantityString(R.plurals.unit_images, it.numImages)
            var text = "${it.numImages} $imageString"
            if (it.skipCount != 0) {
                text += " ${getString(R.string.showing_every_x, it.skipCount)}"
            }
            binding.imgCount.text = text
        }

        viewModel.imageSelector.numSelected.observe(this@ImageGridActivity) {
            binding.imageSelectorBar.setNumImagesSelected(it)
        }

        binding.imageSelectorBar.listener = this

        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(
            SessionUploadWorker.uniqueWorkerTag(viewModel.sessionID)
        ).observe(this) { infoList: List<WorkInfo>? ->
            if (infoList != null && infoList.isNotEmpty()) {
                showUploadProgress(infoList[0])
            } else {
                showUploadProgress(null)
            }
        }
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
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.edit_metadata -> {
                val intent = Intent(applicationContext, MetadataActivity::class.java)
                intent.putExtra("SESSION", viewModel.sessionID)
                startActivity(intent)
                true
            }
            R.id.edit -> {
                viewModel.imageSelector.setEditing(true)
                true
            }
            R.id.upload -> {
                val optionsDialog =
                    ExportOptionsDialog(
                        this,
                        object : ExportOptionsHandler {
                            override fun onSelectOptions(options: ExportOptions) {
                                viewModel.exportOptions = options
                                uploadSession()
                            }
                        },
                        R.string.upload,
                        true,
                        viewModel.exportOptions
                    )
                launchDialog(optionsDialog, item.actionView)
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

    private fun uploadSession() {
        val account = GoogleSignInHelper.getGoogleAccountIfValid(this)?.account
        if (account != null) {
            val options = viewModel.exportOptions
            val data = GoogleDriveUploadWorker.GoogleDriveWorkData(
                sessionID,
                options.metadataOnly,
                account,
                options.includeAutoMothMetadata,
                options.includeUserMetadata
            )
            val workRequest = OneTimeWorkRequestBuilder<GoogleDriveUploadWorker>()
                .setInputData(
                    data.toWorkData()
                ).setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(this).enqueueUniqueWork(
                SessionUploadWorker.uniqueWorkerTag(viewModel.sessionID),
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
            viewModel.deleteCurrentSession()
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
            binding.uploadProgressBar.isVisible = false
            return
        }

        binding.uploadProgressBar.isVisible = true

        val maxProgress =
            info.progress.keyValueMap[SessionUploadWorker.KEY_MAX_PROGRESS] as? Int
        maxProgress?.let {
            binding.uploadProgressBar.maxProgress = it
        }

        when (info.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                binding.uploadProgressBar.setLabel(getString(R.string.waiting_internet))
                binding.uploadProgressBar.configureActionButton(getString(R.string.cancel)) {
                    WorkManager.getInstance(this).cancelUniqueWork(
                        SessionUploadWorker.uniqueWorkerTag(viewModel.sessionID)
                    )
                }
            }
            WorkInfo.State.RUNNING -> {
                if (info.progress.keyValueMap.containsKey(SessionUploadWorker.KEY_METADATA)) {
                    binding.uploadProgressBar.setLabel(getString(R.string.exporting_metadata))
                } else if (info.progress.keyValueMap.containsKey(SessionUploadWorker.KEY_PROGRESS)) {
                    val progress =
                        info.progress.keyValueMap[SessionUploadWorker.KEY_PROGRESS] as? Int
                    binding.uploadProgressBar.setLabel(getString(R.string.uploading_images))
                    progress?.let {
                        binding.uploadProgressBar.setProgress(it)
                    }
                } else {
                    binding.uploadProgressBar.setLabel(getString(R.string.connecting_to_drive))
                }

                binding.uploadProgressBar.configureActionButton(getString(R.string.cancel)) {
                    WorkManager.getInstance(this).cancelUniqueWork(
                        SessionUploadWorker.uniqueWorkerTag(viewModel.sessionID)
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                binding.uploadProgressBar.setLabel(getString(R.string.upload_complete))
                // The Result.Success() WorkInfo update may occur before the final update to KEY_PROGRESS,
                // so just force the progress bar to show fully complete
                binding.uploadProgressBar.showComplete()
                if (!binding.uploadProgressBar.hasSetMaxProgress) {
                    // The Result.Success() WorkInfo contains no progress information, and when
                    // navigating back to a completed upload the progress bar may not have been
                    // configured with the proper max value and would just show 100/100. In this
                    // case, just hide the numbers
                    binding.uploadProgressBar.showNumericProgress(false)
                }

                binding.uploadProgressBar.configureActionButton(getString(R.string.dismiss)) {
                    dismissAndResetUploadBar()
                }
            }
            WorkInfo.State.CANCELLED -> {
                binding.uploadProgressBar.setLabel(getString(R.string.upload_cancelled))
                binding.uploadProgressBar.configureActionButton(getString(R.string.dismiss)) {
                    dismissAndResetUploadBar()
                }
            }
            WorkInfo.State.FAILED -> {
                binding.uploadProgressBar.setLabel(getString(R.string.upload_failed))
                binding.uploadProgressBar.configureActionButton(getString(R.string.retry)) {
                    uploadSession()
                }
            }
        }
    }

    private fun dismissAndResetUploadBar() {
        WorkManager.getInstance(this).pruneWork() // Makes the LiveData update with a null WorkInfo
        binding.uploadProgressBar.isVisible = false
        binding.uploadProgressBar.reset()
    }

    private fun showSelectionTools(visible: Boolean) {
        val tools = binding.imageSelectorBar

        val startOpacity = if (visible) 0.0f else 1.0f
        val targetOpacity = if (visible) 1.0f else 0.0f
        val startTranslation = if (visible) tools.height.toFloat() else 0.0f
        val endTranslation = if (visible) 0.0f else tools.height.toFloat()

        tools.animate()
            .withStartAction {
                if (visible) tools.visibility = View.VISIBLE
                tools.alpha = startOpacity
                tools.translationY = startTranslation
                tools.translationY = -50.0f
            }
            .withEndAction {
                tools.visibility = if (visible) View.VISIBLE else View.INVISIBLE
                var bottomPad = binding.imgCount.height
                if (visible) {
                    bottomPad += tools.height + tools.marginBottom
                }
                binding.imageGrid.setPadding(bottom = bottomPad)
            }
            .alpha(targetOpacity)
            .translationY(endTranslation)
            .setDuration(300)
            .setStartDelay(10).interpolator = AnticipateOvershootInterpolator()
    }

    override fun onDeletePressed() {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.setTitle(getString(R.string.warn_delete_images))
        dialogBuilder.setMessage(getString(R.string.warn_permanent_action))
        dialogBuilder.setPositiveButton(getString(R.string.delete)) { dialog, _ ->
            viewModel.imageSelector.deleteSelectedImages()
            viewModel.imageSelector.setEditing(false)
            dialog.dismiss()
        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

    override fun onExitPressed() {
        viewModel.imageSelector.setEditing(false)
    }

    override fun onMoreOptionPressed(identifier: Int) {
        when (identifier) {
            R.id.select_underexposed -> startSelectingUnderexposed()
        }
    }

    private fun startSelectingUnderexposed() {
        if (selectUnderexposedJob != null) {
            return
        }
        val finder = UnderexposedImageFinder(sessionID)
        finder.underexposedImageHandler = { image ->
            viewModel.imageSelector.setSelected(image, true)
            adapter.refreshEditingState()
        }
        finder.progressListener = {
            binding.progressBar.progress = it
        }
        finder.onCompleteListener = {
            selectUnderexposedJob = null
            binding.progressBar.isVisible = false
        }
        binding.progressBar.max = viewModel.displayCounts.value?.numImages ?: 0
        binding.progressBar.progress = 0
        binding.progressBar.isVisible = true
        selectUnderexposedJob = lifecycleScope.launch {
            finder.getUnderexposedImages()
        }
    }

    private fun cancelSelectingUnderexposed() {
        selectUnderexposedJob?.cancel()
        selectUnderexposedJob = null
        binding.progressBar.isVisible = false
    }
}
