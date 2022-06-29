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
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.FragmentImagingBinding
import java.io.File
import java.time.OffsetDateTime

class ImagingFragment : Fragment() {

    private var _binding: FragmentImagingBinding? = null
    private lateinit var viewModel: ImagingViewModel

    private var menu: Menu? = null
    private var imageCapture: ImageCapture? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[ImagingViewModel::class.java]
        ImagingSettings.loadFromFile(requireContext())?.let {
            viewModel.imagingSettings = it
        }

        _binding = FragmentImagingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requestPermissionsIfNecessary()
        binding.startButton.setOnClickListener {
            startSessionPressed()
        }
//        binding.cameraPreview.scaleType = PreviewView.ScaleType.FIT_CENTER
//        binding.cameraPreview.rotation = 0F

        setHasOptionsMenu(true)
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.imaging_menu, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.imaging_settings -> {
                startSession()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto(saveLocation: File) {
        val imageCapture = imageCapture ?: return
        val session = viewModel.currentSession ?: return

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLocation)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val image = Image(saveLocation.name, OffsetDateTime.now(), session.sessionID)
                    AutoMothRepository.insert(image)
                }
            }
        )
    }

    private fun startSessionPressed() {
        if (viewModel.currentSession == null) {
            binding.startButton.text = getString(R.string.stop_session)
            startSession()
        } else {
            binding.startButton.text = getString(R.string.stop_session)
            finishSession()
        }
    }

    private fun startSession() {
        setButtonsEnabled(false)
    }

    private fun finishSession() {
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val bar: BottomNavigationView = requireActivity().findViewById(R.id.bottom_nav_bar)
        bar.visibility = if (enabled) View.VISIBLE else View.GONE
        menu?.findItem(R.id.imaging_settings).apply {
            this?.isVisible = enabled
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
            requireContext(), R.string.permissions_denied_toast, Toast.LENGTH_SHORT
        ).show()
        // TODO: more persistent warning, maybe mention going to settings
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
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
