package com.uf.automoth.ui.sessions

import androidx.lifecycle.*
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session

class SessionsViewModel : ViewModel() {

    val allSessions: LiveData<List<Session>> = AutoMothRepository.allSessionsFlow.asLiveData()
}
