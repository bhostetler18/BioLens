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

package com.uf.biolens.ui.imaging

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.await
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Session
import com.uf.biolens.databinding.FragmentImagingBinding
import com.uf.biolens.imaging.ImageCaptureInterface
import com.uf.biolens.imaging.ImagingManager
import com.uf.biolens.imaging.ImagingService
import com.uf.biolens.imaging.ImagingSettings
import com.uf.biolens.network.SingleLocationProvider
import com.uf.biolens.ui.common.EditTextDialog
import com.uf.biolens.ui.common.simpleAlertDialogWithOk
import com.uf.biolens.ui.imaging.scheduler.ImagingSchedulerActivity
import com.uf.biolens.utility.combineWith
import com.uf.biolens.utility.launchDialog
import com.uf.biolens.utility.mutate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImagingFragment : Fragment(), MenuProvider, ImageCaptureInterface {

    private var _binding: FragmentImagingBinding? = null
    private val viewModel: ImagingViewModel by viewModels()

    private var menu: Menu? = null
    private lateinit var locationProvider: SingleLocationProvider
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture = ImageCapture.Builder().build()
    private var preview = Preview.Builder().build()

    override val isCameraStarted: Boolean
        get() = cameraProvider?.isBound(imageCapture) ?: false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        locationProvider = SingleLocationProvider(requireContext())

        BioLensRepository.loadDefaultImagingSettings(requireContext())?.let {
            viewModel.imagingSettingsLiveData.postValue(it)
        }

        _binding = FragmentImagingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val orientation = resources.configuration.orientation
        if (orientation == ORIENTATION_LANDSCAPE) {
            binding.cameraPreview.scaleType = PreviewView.ScaleType.FIT_CENTER
        } else {
            binding.cameraPreview.scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        binding.captureButton.setOnClickListener {
            captureButtonPressed()
        }
        binding.intervalButton.setOnClickListener {
            changeIntervalPressed()
        }
        binding.autoStopButton.setOnClickListener {
            changeAutoStopPressed()
        }

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.STARTED)

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermissionsIfNecessary {
            if (!isSessionInProgress()) {
                launchStartCamera()
            }
        }

        viewModel.imagingSettingsLiveData
            .combineWith(ImagingService.currentImagingSettings) { currentSettings, activeSettings ->
                // show active settings rather than selected settings while a session is running
                return@combineWith activeSettings ?: currentSettings
            }.observe(viewLifecycleOwner) { imagingSettings ->
                showImagingSettings(imagingSettings)
            }

        // Though unlikely, this ensures that the UI will display correctly if the user has it open
        // and is watching while a background service session auto-stops or starts.
        ImagingService.IS_RUNNING.observe(viewLifecycleOwner) { isRunning ->
            if (isRunning) {
                binding.captureButton.text = getString(R.string.stop_session)
                setButtonsEnabled(false)
            } else {
                binding.captureButton.text = getString(R.string.start_session)
                launchStartCamera() // Restart preview
                setButtonsEnabled(true)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.imaging_menu, menu)
        this.menu = menu
        refreshUI() // Check and disable menu items appropriately if session is running
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.imaging_schedule -> {
                val intent = Intent(requireContext(), ImagingSchedulerActivity::class.java)
                intent.putExtra(
                    ImagingSchedulerActivity.KEY_ESTIMATED_IMAGE_SIZE_BYTES,
                    estimatedImageSizeInBytes()
                )
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showImagingSettings(imagingSettings: ImagingSettings) {
        val ctx = requireContext()
        binding.intervalDescription.text =
            "${getString(R.string.interval)}: ${imagingSettings.intervalDescription(ctx, true)}"
        binding.autoStopDescription.text = "${getString(R.string.auto_stop)}: ${
        imagingSettings.autoStopDescription(
            ctx,
            true
        )
        }"
    }

    private fun launchStartCamera() = lifecycleScope.launch {
        startCamera()
    }

    override suspend fun startCamera() {
        cameraProvider?.let {
            bindUseCases(it)
            return@startCamera
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.await()?.let {
            this.cameraProvider = it
            bindUseCases(it)
        }
    }

    private suspend fun bindUseCases(cameraProvider: ProcessCameraProvider) = withContext(
        Dispatchers.Main
    ) {
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

        val useCases = if (USE_SERVICE) {
            arrayOf<UseCase>(preview)
        } else {
            imageCapture = ImageCapture.Builder().build()
            arrayOf(preview, imageCapture)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this@ImagingFragment,
                cameraSelector,
                *useCases
            )
        } catch (exc: Exception) {
            exc.localizedMessage?.let { Log.d(TAG, it) }
        }
    }

    // Could probably take a test image for a better estimate
    private fun estimatedImageSizeInBytes(): Double {
        val resolution = preview.resolutionInfo?.resolution ?: return 0.0
        val pixels = resolution.height * resolution.width
        val bytes = 24.0 * pixels / 8.0 // 8 bits each for RGB channels
        // see https://www.graphicsmill.com/blog/2014/11/06/Compression-ratio-for-different-JPEG-quality-values
        val avgCompression = 9.88
        return bytes / avgCompression
    }

    override suspend fun takePhoto(saveLocation: File): Result<ImageCapture.OutputFileResults> =
        suspendCoroutine { continuation ->
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(saveLocation)
                .build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(Result.success(outputFileResults))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(Result.failure(exception))
                    }
                }
            )
        }

    override suspend fun stopCamera() {
        withContext(Dispatchers.Main) {
            cameraProvider?.unbindAll()
        }
    }

    private fun isSessionInProgress(): Boolean {
        return if (USE_SERVICE) {
            ImagingService.IS_RUNNING.value ?: false
        } else {
            viewModel.imagingManager != null
        }
    }

    private fun captureButtonPressed() {
        if (isSessionInProgress()) {
            finishSession(USE_SERVICE)
        } else {
            val editDialog = EditTextDialog(
                requireContext(),
                hint = getString(R.string.session_name_hint),
                positiveText = getString(R.string.start_session),
                negativeText = getString(R.string.cancel),
                positiveListener = { text, dialog ->
                    startSession(text, USE_SERVICE)
                    dialog.dismiss()
                },
                textValidator = Session::isValid
            )
            editDialog.show()
        }
    }

    private fun changeIntervalPressed() {
        val dialog = IntervalDialog(
            requireContext(),
            viewModel.imagingSettings.interval,
            estimatedImageSizeInBytes()
        ) { interval ->
            viewModel.imagingSettingsLiveData.mutate {
                this.interval = interval
            }
        }
        launchDialog(dialog, binding.intervalButton)
    }

    private fun changeAutoStopPressed() {
        val dialog = AutoStopDialog(
            requireContext(),
            viewModel.imagingSettings,
            estimatedImageSizeInBytes()
        ) { mode, value ->
            viewModel.imagingSettingsLiveData.mutate {
                this.autoStopMode = mode
                value?.let {
                    this.autoStopValue = it
                }
            }
        }
        launchDialog(dialog, binding.autoStopButton)
    }

    private fun startSession(name: String?, service: Boolean = false) {
        binding.captureButton.text = getString(R.string.stop_session)
        setButtonsEnabled(false)
        if (service) {
            val intent = ImagingService.getStartSessionIntent(
                requireContext(),
                viewModel.imagingSettings,
                false,
                name
            )
            ContextCompat.startForegroundService(requireContext().applicationContext, intent)
        } else {
            val manager = ImagingManager(viewModel.imagingSettings, WeakReference(this))
            viewModel.imagingManager = manager
            lifecycleScope.launch {
                startCamera()
                manager.start(
                    name ?: getString(R.string.default_session_name),
                    requireContext(),
                    locationProvider,
                    1
                )
            }
        }
    }

    private fun finishSession(service: Boolean = false) {
        binding.captureButton.text = getString(R.string.start_session)
        if (service) {
            val intent = Intent(requireContext().applicationContext, ImagingService::class.java)
            intent.action = ImagingService.ACTION_STOP_SESSION
            requireContext().applicationContext.startService(intent)
        } else {
            lifecycleScope.launch {
                viewModel.imagingManager?.stop("Manual stop")
                viewModel.imagingManager = null
            }
        }
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean, includeCaptureButton: Boolean = false) {
        if (!USE_SERVICE) {
            val bar: BottomNavigationView = requireActivity().findViewById(R.id.bottom_nav_bar)
            bar.visibility = if (enabled) View.VISIBLE else View.GONE
        }
        menu?.findItem(R.id.imaging_schedule).apply {
            this?.isVisible = enabled
        }
        listOf(binding.intervalButton, binding.autoStopButton).forEach {
            it.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        }
        if (includeCaptureButton) {
            binding.captureButton.isVisible = enabled
        } else {
            binding.captureButton.isVisible = true
        }
    }

    private fun requestPermissionsIfNecessary(onRequiredPermissionsGranted: () -> Unit) {
        if (allRequiredPermissionsGranted()) {
            binding.permissionText.isVisible = false
            onRequiredPermissionsGranted()
        } else {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted ->
                if (REQUIRED_PERMISSIONS.all { isGranted[it] == true }) {
                    onRequiredPermissionsGranted()
                } else {
                    warnRequiredPermissionsDenied()
                }
            }
            permissionLauncher.launch(REQUIRED_PERMISSIONS + OPTIONAL_PERMISSIONS)
        }
    }

    private fun allRequiredPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun warnRequiredPermissionsDenied() {
        simpleAlertDialogWithOk(
            requireContext(),
            R.string.warn_required_permissions_denied
        ) {
            setButtonsEnabled(enabled = false, includeCaptureButton = true)
            binding.permissionText.isVisible = true
        }.show()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onPause() {
        super.onPause()
        BioLensRepository.saveDefaultImagingSettings(viewModel.imagingSettings, requireContext())
    }

    private fun refreshUI() {
        if (isSessionInProgress()) {
            binding.captureButton.text = getString(R.string.stop_session)
            setButtonsEnabled(false)
        } else {
            binding.captureButton.text = getString(R.string.start_session)
            setButtonsEnabled(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        if (!USE_SERVICE) {
            finishSession()
        }
    }

    companion object {
        // Set USE_SERVICE = false if you want to run everything inside the main application
        // for debugging purposes
        private var USE_SERVICE = true
        private const val TAG = "[IMAGING FRAGMENT]"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
        private val OPTIONAL_PERMISSIONS = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
