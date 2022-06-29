package com.uf.automoth.ui.imaging

import androidx.lifecycle.ViewModel
import com.uf.automoth.data.Session

class ImagingViewModel : ViewModel() {
    var imagingSettings = ImagingSettings()
    var currentSession: Session? = null
}
