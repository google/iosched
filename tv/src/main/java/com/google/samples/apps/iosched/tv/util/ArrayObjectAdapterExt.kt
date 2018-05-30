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

package com.google.samples.apps.iosched.tv.util

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Presenter

/**
 * Extension functions to make the [ArrayObjectAdapter] feel like a [Collection] object and provides
 * convenience functions to convert from a [Collection] to an [ArrayObjectAdapter] instance.
 */

operator fun <T> ArrayObjectAdapter.plusAssign(element: T) {
    this.add(element)
}

fun <T> Collection<T>.toArrayObjectAdapter(): ArrayObjectAdapter {
    return copyIntoArrayObjectAdapter(ArrayObjectAdapter())
}

fun <T> Collection<T>.toArrayObjectAdapter(
    presenter: Presenter
): ArrayObjectAdapter {
    val adapter = ArrayObjectAdapter(presenter)
    return copyIntoArrayObjectAdapter(adapter)
}

fun <T> Collection<T>.copyIntoArrayObjectAdapter(adapter: ArrayObjectAdapter): ArrayObjectAdapter {
    this.forEach { item -> adapter += item }
    return adapter
}
