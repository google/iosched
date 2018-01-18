package com.google.samples.apps.iosched.shared.usecases.repository

import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.usecases.UseCase

/**
 * TODO: Example use case that simulates a delay.
 */
open class LoadSessionsUseCase(private val repository: SessionRepository)
    : UseCase<String, List<Session>>() {

    override fun execute(parameters: String): List<Session> {
        Thread.sleep(3000)
        return repository.getSessions()
    }
}