package com.uf.automoth.ui.sessions

import androidx.lifecycle.*
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import kotlinx.coroutines.launch

class SessionsViewModel(private val repository: AutoMothRepository) : ViewModel() {

    val allSessions: LiveData<List<Session>> = repository.allSessionsFlow.asLiveData()

    fun insert(session: Session) = viewModelScope.launch {
        repository.insert(session)
    }

    fun delete(session: Session) = viewModelScope.launch {
        repository.delete(session)
    }
}

class SessionsViewModelFactory(private val repository: AutoMothRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
