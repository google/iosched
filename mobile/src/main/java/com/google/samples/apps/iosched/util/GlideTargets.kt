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

package com.google.samples.apps.iosched.util

import android.graphics.drawable.Drawable
import android.view.MenuItem
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

fun MenuItem.asGlideTarget(size: Int): Target<Drawable> = object : BaseTarget<Drawable>() {

    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(size, size)
    }

    override fun removeCallback(cb: SizeReadyCallback) {}

    override fun onLoadStarted(placeholder: Drawable?) {
        icon = placeholder
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        icon = errorDrawable
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        icon = placeholder
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        icon = resource
    }
}
