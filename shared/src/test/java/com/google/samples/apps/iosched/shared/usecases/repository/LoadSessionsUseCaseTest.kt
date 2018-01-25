package com.google.samples.apps.iosched.shared.usecases.repository


import com.google.samples.apps.iosched.shared.data.session.SessionRepository
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
        val loadSessionsUseCase = LoadSessionsUseCase(SessionRepository(TestSessionDataSource))
        val sessions: Result.Success<List<Session>> =
                loadSessionsUseCase.executeNow(Unit) as Result.Success<List<Session>>

        assertEquals(sessions.data, SessionRepository(TestSessionDataSource).getSessions())
    }
}
