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

package com.uf.biolens.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.uf.biolens.data.BioLensRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class SessionsViewModel : ViewModel() {

    enum class SessionSortMode {
        ASCENDING, DESCENDING
    }

    val sortMode = MutableStateFlow(SessionSortMode.ASCENDING)
    val allSessions = sortMode.combine(BioLensRepository.allSessionsFlow) { sort, sessions ->
        when (sort) {
            SessionSortMode.ASCENDING -> sessions.sortedBy { it.started }
            SessionSortMode.DESCENDING -> sessions.sortedByDescending { it.started }
        }
    }.asLiveData()
}
