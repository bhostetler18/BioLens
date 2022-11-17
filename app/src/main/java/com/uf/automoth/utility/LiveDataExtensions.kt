package com.uf.automoth.utility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.mutate(changes: T.() -> Unit) {
    this.value = this.value?.apply(changes)
}

fun <T> LiveData<T>.combineWith(
    liveData: LiveData<T>,
    combine: (first: T?, second: T?) -> T?
): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this) {
        result.value = combine(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = combine(this.value, liveData.value)
    }
    return result
}
