package com.uf.automoth.ui.imaging

import android.Manifest
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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import com.uf.automoth.ui.common.simpleAlertDialogWithOk
import com.uf.automoth.ui.imaging.scheduler.ImagingSchedulerActivity
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

class ImagingFragment : Fragment(), MenuProvider, ImageCaptureInterface {

    private var _binding: FragmentImagingBinding? = null
    private val viewModel: ImagingViewModel by viewModels()

    private var menu: Menu? = null
    private lateinit var locationProvider: SingleLocationProvider
    private var imageCapture = ImageCapture.Builder().build()

    override val isCameraStarted: Boolean
        get() = TODO("")

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        locationProvider = SingleLocationProvider(requireContext())

        ImagingSettings.loadDefaults(requireContext())?.let {
            viewModel.imagingSettings = it
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermissionsIfNecessary {
            if (!isSessionInProgress()) {
                startCamera(null)
            }
        }

        // Though unlikely, this ensures that the UI will display correctly if the user has it open
        // and is watching while a background service session auto-stops or starts.
        ImagingService.IS_RUNNING.observe(viewLifecycleOwner) { isRunning ->
            if (isRunning) {
                binding.captureButton.text = getString(R.string.stop_session)
                setButtonsEnabled(false)
            } else {
                binding.captureButton.text = getString(R.string.start_session)
                startCamera(null) // Restart preview
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

    override fun startCamera(onStart: (() -> Unit)?) {
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
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }
            onStart?.invoke()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Could probably take a test image for a better estimate
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

    override fun stopCamera() {
        TODO("Not yet implemented")
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
                    locationProvider,
                    1000
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
            viewModel.imagingManager?.stop()
            viewModel.imagingManager = null
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
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
        private val OPTIONAL_PERMISSIONS = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
