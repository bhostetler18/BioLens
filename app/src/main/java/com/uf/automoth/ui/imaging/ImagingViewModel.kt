package com.uf.automoth.ui.imaging

import androidx.lifecycle.ViewModel

class ImagingViewModel : ViewModel() {
    var imagingSettings = ImagingSettings()
    var imagingManager: ImagingManager? = null
}
