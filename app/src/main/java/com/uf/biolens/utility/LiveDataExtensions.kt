/*
 * Copyright (c) 2022 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.biolens.utility

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
