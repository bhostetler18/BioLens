package com.uf.automoth.ui.imaging

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.uf.automoth.imaging.AutoStopMode
import com.uf.automoth.imaging.ImagingManager
import com.uf.automoth.imaging.ImagingSettings

class ImagingViewModel : ViewModel() {
    val imagingSettingsLiveData = MutableLiveData(ImagingSettings())
    var imagingManager: ImagingManager? = null

    val imagingSettings: ImagingSettings
        get() = imagingSettingsLiveData.value!!
}
