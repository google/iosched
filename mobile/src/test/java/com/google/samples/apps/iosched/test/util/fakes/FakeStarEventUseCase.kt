/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.test.util.fakes

import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.CoroutineDispatcher

class FakeStarEventUseCase(dispatcher: CoroutineDispatcher) : StarEventAndNotifyUseCase(
    DefaultSessionAndUserEventRepository(
        TestUserEventDataSource(),
        DefaultSessionRepository(TestDataRepository)
    ),
    mock {},
    dispatcher
)
