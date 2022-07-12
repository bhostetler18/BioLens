package com.uf.automoth.ui.imaging

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.FragmentImagingBinding
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ImagingFragment : Fragment() {

    private var _binding: FragmentImagingBinding? = null
    private lateinit var viewModel: ImagingViewModel

    private var menu: Menu? = null
    private lateinit var locationProvider: SingleLocationProvider
    private var imageCapture: ImageCapture? = null
    private var timer: Timer? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[ImagingViewModel::class.java]
        locationProvider = SingleLocationProvider(requireContext())

        ImagingSettings.loadFromFile(requireContext())?.let {
            viewModel.imagingSettings = it
        }

        _binding = FragmentImagingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requestPermissionsIfNecessary()
        binding.startButton.setOnClickListener {
            startSessionPressed()
        }
        binding.intervalButton.setOnClickListener {
            changeIntervalPressed()
        }
//        binding.cameraPreview.scaleType = PreviewView.ScaleType.FIT_CENTER
//        binding.cameraPreview.rotation = 0F
        return root
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
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // TODO: this is a placeholder – should probably take a test image for a better estimate
    private fun estimatedImageSizeInBytes(): Double {
        val resolution = imageCapture?.attachedSurfaceResolution ?: return 0.0
        val pixels = resolution.height * resolution.width
        val bytes = 24.0 * pixels / 8.0 // 8 bits each for RGB channels
        val avgCompression = 9.88 // see https://www.graphicsmill.com/blog/2014/11/06/Compression-ratio-for-different-JPEG-quality-values
        return bytes / avgCompression
    }

    private fun takePhoto(saveLocation: File) {
        val imageCapture = imageCapture ?: return
        val manager = viewModel.manager ?: return
        val context = context ?: return

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLocation)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            manager
        )
    }

    private fun startSessionPressed() {
        if (viewModel.manager == null) {
            binding.startButton.text = getString(R.string.stop_session)
            startSession()
        } else {
            binding.startButton.text = getString(R.string.start_session)
            finishSession()
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

    private fun startSession() {
        setButtonsEnabled(false)

        val start = OffsetDateTime.now()
        val session = Session(
            "Untitled session",
            ImagingManager.getUniqueDirectory(start),
            start,
            0.0,
            0.0,
            viewModel.imagingSettings.interval
        )
        // Need to get session primary key before continuing, so block for this call
        runBlocking {
            AutoMothRepository.create(session)
        }

        locationProvider.getCurrentLocation {
            AutoMothRepository.updateSessionLocation(session.sessionID, it)
        }

        val manager = ImagingManager(session, viewModel.imagingSettings)
        viewModel.manager = manager
        val milliseconds: Long = viewModel.imagingSettings.interval * 1000L
        timer = fixedRateTimer("imaging", false, 0, milliseconds) {
            if (manager.shouldStop()) {
                finishSession()
            } else {
                takePhoto(manager.getUniqueFile())
            }
        }
    }

    private fun finishSession() {
        val end = OffsetDateTime.now()
        setButtonsEnabled(true)
        timer?.cancel()
        viewModel.manager?.session?.let {
            AutoMothRepository.updateSessionCompletion(it.sessionID, end)
        }
        viewModel.manager = null
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val bar: BottomNavigationView = requireActivity().findViewById(R.id.bottom_nav_bar)
        bar.visibility = if (enabled) View.VISIBLE else View.GONE
        menu?.findItem(R.id.imaging_settings).apply {
            this?.isVisible = enabled
        }
        listOf(binding.intervalButton, binding.autoStopButton).forEach {
            it.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun requestPermissionsIfNecessary() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted ->
                if (isGranted.values.all { it }) {
                    startCamera()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        finishSession()
        viewModel.imagingSettings.saveToFile(requireContext())
    }

    companion object {
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
