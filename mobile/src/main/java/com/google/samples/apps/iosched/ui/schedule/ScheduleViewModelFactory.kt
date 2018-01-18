package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.usecases.repository.LoadSessionsUseCase


/**
 * Creates [ScheduleViewModel]s, used with the [android.arch.lifecycle.ViewModelProviders].
 */
class ScheduleViewModelFactory : ViewModelProvider.Factory {

    private val repository = DefaultSessionRepository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            return ScheduleViewModel(LoadSessionsUseCase(repository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}