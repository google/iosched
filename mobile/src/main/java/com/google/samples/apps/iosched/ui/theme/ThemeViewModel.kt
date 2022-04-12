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

package com.google.samples.apps.iosched.ui.theme

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel

/**
 * Thin ViewModel for themed Activities that don't have another ViewModel to use with
 * [ThemedActivityDelegate].
 */
class ThemeViewModel @ViewModelInject constructor(
    themedActivityDelegate: ThemedActivityDelegate
) : ViewModel(), ThemedActivityDelegate by themedActivityDelegate
