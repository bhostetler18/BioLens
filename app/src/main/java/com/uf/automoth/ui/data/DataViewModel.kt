package com.uf.automoth.ui.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DataViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the data Fragment"
    }
    val text: LiveData<String> = _text
}