/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.search

import com.google.samples.apps.iosched.model.Codelab
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Speaker

/**
 * Sealed class that represents searchable contents.
 */
sealed class Searchable {

    class SearchedSession(val session: Session) : Searchable()
    class SearchedSpeaker(val speaker: Speaker) : Searchable()
    class SearchedCodelab(val codelab: Codelab) : Searchable()
}
