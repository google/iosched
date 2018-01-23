package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableInt
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.repository.LoadSessionsUseCase


/**
 * Loads data and exposes it to the view.
 */
class ScheduleViewModel(loadSessionsUseCase: LoadSessionsUseCase) : ViewModel() {

    // TODO: Example of data holder using LiveData
    private var _sessions = MutableLiveData<List<Session>>()
    private val _isLoading = MutableLiveData<Boolean>().apply{ value = true }

    // TODO: Example LiveData getters
    val sessions: LiveData<List<Session>> get() = _sessions
    val isLoading: LiveData<Boolean> get() = _isLoading

    // TODO: Example data binding observable
    val numberOfSessions = ObservableInt()

    init {
        // TODO: replace. Dummy async task
        loadSessionsUseCase.executeAsync("someVar") { result: Result<List<Session>> ->
            when (result) {
                is Result.Success<List<Session>> -> {
                    _sessions.value = result.data
                    numberOfSessions.set(result.data.size)
                    _isLoading.value = false
                }
                is Result.Error -> numberOfSessions.set(0)
                is Result.Loading -> _isLoading.value = true
            }
        }
    }
}

