package com.google.samples.apps.iosched.shared.usecases.repository


import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TODO: placeholder/example for use cases.
 */
class LoadSessionsUseCaseTest {

    @Test
    fun returnsListOfSessions() {
        val loadSessionsUseCase = LoadSessionsUseCase(DefaultSessionRepository)
        val sessions: Result.Success<List<Session>> = loadSessionsUseCase.executeNow("test")
                as Result.Success<List<Session>>

        assertEquals(sessions.data, DefaultSessionRepository.getSessions())
    }
}