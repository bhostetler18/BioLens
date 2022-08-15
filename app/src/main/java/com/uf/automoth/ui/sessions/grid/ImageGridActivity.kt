package com.uf.automoth.ui.sessions.grid

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ActivityImageGridBinding
import com.uf.automoth.network.GoogleDriveLoginManager
import com.uf.automoth.network.GoogleDriveSignInActivity
import com.uf.automoth.network.GoogleDriveUploadWorker
import com.uf.automoth.ui.common.EditTextDialog
import kotlinx.coroutines.*

class ImageGridActivity : AppCompatActivity(), GoogleDriveSignInActivity {
    private lateinit var viewModel: ImageGridViewModel

    private val driveManager = GoogleDriveLoginManager(this)
    override val appContext: Context get() = applicationContext
    override val applicationName: String get() = getString(R.string.app_name)
    override val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        driveManager.handleSignInResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityImageGridBinding.inflate(layoutInflater)

        val sessionID = intent.extras?.get("SESSION") as? Long ?: return
        val session = runBlocking(Dispatchers.IO) {
            AutoMothRepository.getSession(sessionID)
        } ?: return // TODO: better handling of nonexistent session

        viewModel = ViewModelProvider(this, ImageGridViewModel.ImageGridViewModelFactory(session))[ImageGridViewModel::class.java]

        val adapter = ImageGridAdapter(session)
        binding.imageGrid.adapter = adapter

        viewModel.allImages.observe(this) { images ->
            images?.let { adapter.submitList(it) }
        }

        AutoMothRepository.getNumImagesInSession(sessionID).asLiveData().observe(this) { count ->
            val imageString = if (count != 1) getString(R.string.image_plural) else getString(R.string.image_singular)

            binding.imgCount.text = "$count $imageString"
        }

        setSupportActionBar(binding.toolbar)
        setContentView(binding.root)
        supportActionBar?.title = session.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.session_menu, menu)
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
                driveManager.signInIfNecessary()
                val account = driveManager.currentAccount?.account
                if (account != null) {
                    val workRequest = OneTimeWorkRequestBuilder<GoogleDriveUploadWorker>()
                        .setInputData(
                            workDataOf(
                                GoogleDriveUploadWorker.KEY_SESSION_ID to viewModel.session.sessionID,
                                GoogleDriveUploadWorker.KEY_ACCOUNT_NAME to account.name,
                                GoogleDriveUploadWorker.KEY_ACCOUNT_TYPE to account.type
                            )
                        )
                        .build()
                    WorkManager.getInstance(this).beginWith(workRequest).enqueue()
                } else {
                    // TODO: handle sign in failure
                }

                true
            }
            R.id.delete -> {
                deleteCurrentSession()
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
}
