package com.uf.automoth.utility

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.mutate(changes: T.() -> Unit) {
    this.value = this.value?.apply(changes)
}
