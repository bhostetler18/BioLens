package com.uf.automoth.ui.imaging

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.automoth.R
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.FragmentImagingBinding
import com.uf.automoth.imaging.ImageCaptureInterface
import com.uf.automoth.imaging.ImagingManager
import com.uf.automoth.imaging.ImagingService
import com.uf.automoth.imaging.ImagingSettings
import com.uf.automoth.network.SingleLocationProvider
import com.uf.automoth.ui.common.EditTextDialog
import com.uf.automoth.ui.imaging.scheduler.ImagingSchedulerActivity
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class ImagingFragment : Fragment(), MenuProvider, ImageCaptureInterface {

    private var _binding: FragmentImagingBinding? = null
    private val viewModel: ImagingViewModel by viewModels()

    private var menu: Menu? = null
    private lateinit var locationProvider: SingleLocationProvider
    private var imageCapture = ImageCapture.Builder().build()

    override var isRestartingCamera = AtomicBoolean(false)

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        locationProvider = SingleLocationProvider(requireContext())

        ImagingSettings.loadDefaultsFromFile(requireContext())?.let {
            viewModel.imagingSettings = it
        }

        _binding = FragmentImagingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requestPermissionsIfNecessary {
            if (!isSessionInProgress()) {
                startCamera()
            }
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

        // Though unlikely, this ensures that the UI will display correctly if the user has it open
        // and is watching while a background service session auto-stops or starts.
        ImagingService.IS_RUNNING.observe(viewLifecycleOwner) { isRunning ->
            if (isRunning) {
                binding.captureButton.text = getString(R.string.stop_session)
                setButtonsEnabled(false)
            } else {
                binding.captureButton.text = getString(R.string.start_session)
                startCamera() // Restart preview
                setButtonsEnabled(true)
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }
            isRestartingCamera.set(false)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // TODO: this is a placeholder – should probably take a test image for a better estimate
    private fun estimatedImageSizeInBytes(): Double {
        val resolution = imageCapture.resolutionInfo?.resolution ?: return 0.0
        val pixels = resolution.height * resolution.width
        val bytes = 24.0 * pixels / 8.0 // 8 bits each for RGB channels
        // see https://www.graphicsmill.com/blog/2014/11/06/Compression-ratio-for-different-JPEG-quality-values
        val avgCompression = 9.88
        return bytes / avgCompression
    }

    override fun takePhoto(
        saveLocation: File,
        onSaved: ImageCapture.OnImageSavedCallback
    ) {
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLocation)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            onSaved
        )
    }

    override fun restartCamera() {
        isRestartingCamera.set(true)
        startCamera()
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
                layoutInflater,
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
            layoutInflater,
            viewModel.imagingSettings.interval,
            estimatedImageSizeInBytes()
        ) { interval -> viewModel.imagingSettings.interval = interval }
        dialog.show()
    }

    private fun changeAutoStopPressed() {
        val dialog = AutoStopDialog(
            requireContext(),
            layoutInflater,
            viewModel.imagingSettings,
            estimatedImageSizeInBytes()
        ) { mode, value ->
            viewModel.imagingSettings.autoStopMode = mode
            value?.let {
                viewModel.imagingSettings.autoStopValue = it
            }
        }
        dialog.show()
    }

    private fun startSession(name: String?, service: Boolean = false) {
        // TODO: if there are scheduled sessions, warn and ask if the user wants to cancel them
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
                manager.start(
                    name ?: getString(R.string.default_session_name),
                    requireContext(),
                    locationProvider
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
            startCamera() // Restart preview
        } else {
            viewModel.imagingManager?.stop()
            viewModel.imagingManager = null
        }
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
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
    }

    private fun requestPermissionsIfNecessary(onAllPermissionGranted: () -> Unit) {
        if (allPermissionsGranted()) {
            onAllPermissionGranted()
        } else {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted ->
                if (isGranted.values.all { it }) {
                    onAllPermissionGranted()
                } else {
                    warnPermissionsDenied()
                }
            }
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun warnPermissionsDenied() {
        Toast.makeText(
            requireContext(),
            R.string.permissions_denied_toast,
            Toast.LENGTH_SHORT
        ).show()
        // TODO: more persistent warning, disable capture button and maybe mention going to settings
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onPause() {
        super.onPause()
        viewModel.imagingSettings.saveToFile(requireContext())
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
        private var USE_SERVICE = true
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
