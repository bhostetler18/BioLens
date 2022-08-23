package com.uf.automoth.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.uf.automoth.data.AutoMothRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class SessionsViewModel : ViewModel() {

    enum class SessionSortMode {
        ASCENDING, DESCENDING
    }

    val sortMode = MutableStateFlow(SessionSortMode.ASCENDING)
    val allSessions = sortMode.combine(AutoMothRepository.allSessionsFlow) { sort, sessions ->
        when (sort) {
            SessionSortMode.ASCENDING -> sessions.sortedBy { it.started }
            SessionSortMode.DESCENDING -> sessions.sortedByDescending { it.started }
        }
    }.asLiveData()
}
