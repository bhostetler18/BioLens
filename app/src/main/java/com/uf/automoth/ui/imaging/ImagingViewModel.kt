package com.uf.automoth.ui.imaging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ImagingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the imaging Fragment"
    }
    val text: LiveData<String> = _text
}