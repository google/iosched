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

package com.google.samples.apps.iosched.ui.signin

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.MenuItem
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.util.asGlideTarget
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun Toolbar.setupProfileMenuItem(
    viewModel: MainActivityViewModel,
    lifecycleOwner: LifecycleOwner
) {
    inflateMenu(R.menu.profile)
    val profileItem = menu.findItem(R.id.action_profile) ?: return
    profileItem.setOnMenuItemClickListener {
        viewModel.onProfileClicked()
        true
    }
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            viewModel.userInfo.collect {
                setProfileContentDescription(profileItem, resources, it)
            }
        }
    }

    val avatarSize = resources.getDimensionPixelSize(R.dimen.nav_account_image_size)
    val target = profileItem.asGlideTarget(avatarSize)
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            viewModel.currentUserImageUri.collect {
                setProfileAvatar(context, target, it)
            }
        }
    }
}

fun setProfileContentDescription(item: MenuItem, res: Resources, user: AuthenticatedUserInfo?) {
    val description = if (user?.isSignedIn() == true) {
        res.getString(R.string.a11y_signed_in_content_description, user.getDisplayName())
    } else {
        res.getString(R.string.a11y_signed_out_content_description)
    }
    MenuItemCompat.setContentDescription(item, description)
}

fun setProfileAvatar(
    context: Context,
    target: Target<Drawable>,
    imageUri: Uri?,
    placeholder: Int = R.drawable.ic_default_profile_avatar
) {
    // Inflate the drawable for proper tinting
    val placeholderDrawable = AppCompatResources.getDrawable(context, placeholder)
    when (imageUri) {
        null -> {
            Glide.with(context)
                .load(placeholderDrawable)
                .apply(RequestOptions.circleCropTransform())
                .into(target)
        }
        else -> {
            Glide.with(context)
                .load(imageUri)
                .apply(RequestOptions.placeholderOf(placeholderDrawable).circleCrop())
                .into(target)
        }
    }
}
