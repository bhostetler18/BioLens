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

package com.uf.biolens.ui.imaging

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.uf.biolens.imaging.ImagingManager
import com.uf.biolens.imaging.ImagingSettings

class ImagingViewModel : ViewModel() {
    val imagingSettingsLiveData = MutableLiveData(ImagingSettings())
    var imagingManager: ImagingManager? = null

    val imagingSettings: ImagingSettings
        get() = imagingSettingsLiveData.value!!
}
