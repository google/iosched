/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.di

import android.content.Context
import com.google.samples.apps.iosched.util.initializers.AnalyticsHelperInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InitializerEntryPoint {

    companion object {
        fun resolve(@ApplicationContext context: Context): InitializerEntryPoint {
            return EntryPointAccessors.fromApplication(
                context,
                InitializerEntryPoint::class.java
            )
        }
    }

    fun inject(initializer: AnalyticsHelperInitializer)
}
