package com.google.samples.apps.iosched.shared.usecases.repository

import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.usecases.UseCase
import javax.inject.Inject

/**
 * TODO: Example use case that simulates a delay.
 */
open class LoadSessionsUseCase @Inject constructor(private val repository: SessionRepository)
    : UseCase<Unit, List<Session>>() {

    override fun execute(parameters: Unit): List<Session> {
        Thread.sleep(3000)
        return repository.getSessions()
    }
}
