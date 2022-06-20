package com.uf.automoth.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InfoViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the info Fragment"
    }
    val text: LiveData<String> = _text
}