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

package com.google.samples.apps.iosched.ui.speaker

import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Speaker

/**
 * Formats a [TextView] to display a [Speaker]'s social links.
 */
@BindingAdapter(value = ["websiteUrl", "twitterUrl", "githubUrl", "linkedInUrl"], requireAll = true)
fun createSpeakerLinksView(
    textView: TextView,
    websiteUrl: String?,
    twitterUrl: String?,
    githubUrl: String?,
    linkedInUrl: String?
) {
    val links = mapOf(
        R.string.speaker_link_website to websiteUrl,
        R.string.speaker_link_twitter to twitterUrl,
        R.string.speaker_link_github to githubUrl,
        R.string.speaker_link_linkedin to linkedInUrl
    )
        .filterValues { !it.isNullOrEmpty() }
        .map { (labelRes, url) ->
            val span = SpannableString(textView.context.getString(labelRes))
            span.setSpan(URLSpan(url), 0, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            span
        }
        .joinTo(
            SpannableStringBuilder(),
            separator = textView.context.getString(R.string.speaker_link_separator)
        )
    if (links.isNotBlank()) {
        textView.apply {
            visibility = VISIBLE
            text = links
            // Make links clickable
            movementMethod = LinkMovementMethod.getInstance()
            isFocusable = false
            isClickable = false
        }
    } else {
        textView.visibility = GONE
    }
}

/**
 * Loads a [Speaker]'s photo or picks a default avatar if no photo is specified.
 */
@BindingAdapter(value = ["speakerImage", "listener"], requireAll = false)
fun speakerImage(
    imageView: ImageView,
    speaker: Speaker?,
    listener: ImageLoadListener?
) {
    speaker ?: return

    // Want a 'random' default avatar but should be stable as used on both session details &
    // speaker detail screens (as a shared element transition), so use first initial to pick.
    val placeholderId = when (speaker.name[0].toLowerCase()) {
        in 'a'..'i' -> R.drawable.ic_default_avatar_1
        in 'j'..'r' -> R.drawable.ic_default_avatar_2
        else -> R.drawable.ic_default_avatar_3
    }

    if (speaker.imageUrl.isNullOrBlank()) {
        imageView.setImageResource(placeholderId)
    } else {
        val imageLoad = Glide.with(imageView)
            .load(speaker.imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(placeholderId)
                    .circleCrop()
            )
        if (listener != null) {
            imageLoad.listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onImageLoaded()
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onImageLoadFailed()
                    return false
                }
            })
        }
        imageLoad.into(imageView)
    }
}

/**
 * An interface for responding to image loading completion.
 */
interface ImageLoadListener {
    fun onImageLoaded()
    fun onImageLoadFailed()
}
