package com.uf.automoth.ui.imaging

import androidx.lifecycle.ViewModel
import com.uf.automoth.imaging.ImagingManager

class ImagingViewModel : ViewModel() {
    var imagingSettings = ImagingSettings()
    var imagingManager: ImagingManager? = null
}
