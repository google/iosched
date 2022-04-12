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

package com.google.samples.apps.iosched.shared.data.login.datasources

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.firebase.auth.UserInfo
import com.google.samples.apps.iosched.shared.R
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfoBasic
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.RegisteredUserDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.NotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.result.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * A configurable [RegisteredUserDataSource] used for staging.
 *
 * @see LoginModule
 */
class StagingRegisteredUserDataSource(val isRegistered: Boolean) : RegisteredUserDataSource {
    override fun observeUserChanges(userId: String): Flow<Result<Boolean?>> = channelFlow {
        channel.offer(Result.Success(isRegistered))
        awaitClose { }
    }
}

/**
 * A configurable [AuthenticatedUserInfo] used for staging.
 *
 * @see [LoginModule]
 */
open class StagingAuthenticatedUserInfo(
    val context: Context,
    val registered: Boolean = true,
    val signedIn: Boolean = true,
    val userId: String? = "StagingUser"

) : AuthenticatedUserInfo {

    override fun isSignedIn(): Boolean = signedIn

    override fun isRegistered(): Boolean = registered

    override fun isRegistrationDataReady(): Boolean = true

    override fun getEmail(): String? = "staginguser@example.com"

    override fun getProviderData(): MutableList<out UserInfo> = TODO("Not implemented")

    override fun isAnonymous(): Boolean = !signedIn

    override fun getPhoneNumber(): String? = TODO("Not implemented")

    override fun getUid(): String? = userId

    override fun isEmailVerified(): Boolean = TODO("Not implemented")

    override fun getDisplayName(): String? = "Staging User"

    override fun getPhotoUrl(): Uri? {
        val resources = context.resources
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.staging_user_profile))
            .appendPath(resources.getResourceTypeName(R.drawable.staging_user_profile))
            .appendPath(resources.getResourceEntryName(R.drawable.staging_user_profile))
            .build()
    }

    override fun getProviderId(): String = TODO("Not implemented")

    override fun getLastSignInTimestamp(): Long? = TODO("not implemented")

    override fun getCreationTimestamp(): Long? = TODO("not implemented")
}

/**
 * A configurable [AuthStateUserDataSource] used for staging.
 *
 * @see LoginModule
 */
class StagingAuthStateUserDataSource(
    val isSignedIn: Boolean,
    val isRegistered: Boolean,
    val userId: String?,
    val context: Context,
    val notificationAlarmUpdater: NotificationAlarmUpdater
) : AuthStateUserDataSource {

    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfoBasic?>> = channelFlow {
        userId?.let {
            notificationAlarmUpdater.updateAll(userId)
        }

        channel.offer(
            Result.Success(
                StagingAuthenticatedUserInfo(
                    registered = isRegistered,
                    signedIn = isSignedIn,
                    context = context
                )
            )
        )

        awaitClose { }
    }
}
