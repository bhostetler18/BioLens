package com.uf.automoth.ui.sessions.grid

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.ActivityImageGridBinding
import com.uf.automoth.ui.common.EditTextDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

class ImageGridActivity : AppCompatActivity() {
    private lateinit var session: Session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityImageGridBinding.inflate(layoutInflater)

        val sessionID = intent.extras?.get("SESSION") as? Long ?: return
        session = runBlocking(Dispatchers.IO) {
            AutoMothRepository.getSession(sessionID)
        }

        val sessionDirectory = File(AutoMothRepository.storageLocation, session.directory)
        val adapter = ImageGridAdapter(sessionDirectory)
        binding.imageGrid.adapter = adapter

        AutoMothRepository.getImagesInSession(sessionID).observe(this) { sessions ->
            sessions?.let { adapter.submitList(it) }
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
            hint = session.name,
            positiveText = getString(R.string.rename),
            negativeText = getString(R.string.cancel),
            positiveListener = { text, dialog ->
                AutoMothRepository.renameSession(session.sessionID, text)
                supportActionBar?.title = text
                dialog.dismiss()
            },
            textValidator = Session::isValid
        )
        editDialog.show()
    }

    private fun deleteCurrentSession() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(getString(R.string.warn_delete_session))
        dialogBuilder.setMessage(getString(R.string.warn_permanent_action))
        dialogBuilder.setPositiveButton(getString(R.string.delete)) { dialog, _ ->
            AutoMothRepository.delete(session)
            dialog.dismiss()
            this.finish()
        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }
}
