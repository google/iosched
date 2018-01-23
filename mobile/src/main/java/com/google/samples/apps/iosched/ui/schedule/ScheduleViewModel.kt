package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.repository.LoadSessionsUseCase


/**
 * Loads data and exposes it to the view.
 */
class ScheduleViewModel(loadSessionsUseCase: LoadSessionsUseCase) : ViewModel() {

    // TODO: Example LiveData holders
    val sessions: LiveData<List<Session>>
    val isLoading: LiveData<Boolean>
    val numberOfSessions: LiveData<Int>

    init {
        // TODO: replace. Dummy async task
        val liveResult: LiveData<Result<List<Session>>> = loadSessionsUseCase.executeAsync("dummy")

        sessions = Transformations.map(liveResult, { result ->
            (result as? Result.Success)?.data ?: emptyList()
        })
        isLoading = Transformations.map(liveResult, { result -> result == Result.Loading })
        numberOfSessions = Transformations.map(liveResult, { result ->
            (result as? Result.Success)?.data?.size ?: 0
        })
    }
}

