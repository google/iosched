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

package com.google.samples.apps.iosched.shared.data.signin

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo

/**
 * Delegates [AuthenticatedUserInfo] calls to a [FirebaseUser] to be used in production.
 */
class FirebaseRegisteredUserInfo(
    private val basicUserInfo: AuthenticatedUserInfoBasic?,
    private val isRegistered: Boolean?
) : AuthenticatedUserInfo {

    override fun isRegistered(): Boolean = isRegistered ?: false

    override fun isSignedIn(): Boolean = basicUserInfo?.isSignedIn() == true

    override fun getEmail(): String? = basicUserInfo?.getEmail()

    override fun getProviderData(): MutableList<out UserInfo>? = basicUserInfo?.getProviderData()

    override fun isAnonymous(): Boolean? = basicUserInfo?.isAnonymous()

    override fun getPhoneNumber(): String? = basicUserInfo?.getPhoneNumber()

    override fun getUid(): String? = basicUserInfo?.getUid()

    override fun isEmailVerified(): Boolean? = basicUserInfo?.isEmailVerified()

    override fun getDisplayName(): String? = basicUserInfo?.getDisplayName()

    override fun getPhotoUrl(): Uri? = basicUserInfo?.getPhotoUrl()

    override fun getProviderId(): String? = basicUserInfo?.getProviderId()

    override fun getLastSignInTimestamp(): Long? = basicUserInfo?.getLastSignInTimestamp()

    override fun getCreationTimestamp(): Long? = basicUserInfo?.getCreationTimestamp()

    override fun isRegistrationDataReady(): Boolean = isRegistered != null
}

open class FirebaseUserInfo(
    private val firebaseUser: FirebaseUser?
) : AuthenticatedUserInfoBasic {

    override fun isSignedIn(): Boolean = firebaseUser != null

    override fun getEmail(): String? = firebaseUser?.email

    override fun getProviderData(): MutableList<out UserInfo>? = firebaseUser?.providerData

    override fun isAnonymous(): Boolean? = firebaseUser?.isAnonymous

    override fun getPhoneNumber(): String? = firebaseUser?.phoneNumber

    override fun getUid(): String? = firebaseUser?.uid

    override fun isEmailVerified(): Boolean? = firebaseUser?.isEmailVerified

    override fun getDisplayName(): String? = firebaseUser?.displayName

    override fun getPhotoUrl(): Uri? = firebaseUser?.photoUrl

    override fun getProviderId(): String? = firebaseUser?.providerId

    override fun getLastSignInTimestamp(): Long? = firebaseUser?.metadata?.lastSignInTimestamp

    override fun getCreationTimestamp(): Long? = firebaseUser?.metadata?.creationTimestamp
}
