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

package com.google.samples.apps.iosched.shared.data.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.firebase.auth.UserInfo
import com.google.samples.apps.iosched.shared.R
import com.google.samples.apps.iosched.shared.result.Result


/**
 * An [AuthenticatedUser] used for hermetic development and testing.
 */
class StagingAuthenticatedUser(val context: Context): AuthenticatedUser {

    val stagingLoggedInFirebaseUser = StagingLoggedInFirebaseUser(context)
    val stagingLoggedOutFirebaseUser = StagingLoggedOutFirebaseUser(context)

    val currentUserResult = MutableLiveData<Result<AuthenticatedUserInfo>?>()

    init {
        currentUserResult.value = Result.Success(stagingLoggedInFirebaseUser)
    }

    override fun getToken(): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()
                .apply { value =  Result.Success("123")  }
        return result
    }

    override fun getCurrentUser(): LiveData<Result<AuthenticatedUserInfo>?> {

        return currentUserResult
    }

    private var loggedIn: Boolean = false

    fun logIn() {
        loggedIn = true
        currentUserResult.postValue(Result.Success(stagingLoggedInFirebaseUser))
    }

    fun logOut() {
        loggedIn = false
        currentUserResult.postValue(Result.Success(stagingLoggedOutFirebaseUser))
    }

}

open class StagingLoggedInFirebaseUser(val context: Context) : AuthenticatedUserInfo {

    override fun isLoggedIn(): Boolean = true

    override fun getEmail(): String? {
        TODO("not implemented")
    }

    override fun getProviderData(): MutableList<out UserInfo> {
        TODO("not implemented")
    }

    override fun isAnonymous(): Boolean {
        TODO("not implemented")
    }

    override fun getPhoneNumber(): String? {
        TODO("not implemented")
    }

    override fun getUid(): String {
        TODO("not implemented")
    }

    override fun isEmailVerified(): Boolean {
        TODO("not implemented")
    }

    override fun getDisplayName(): String? {
        TODO("not implemented")
    }

    override fun getPhotoUrl(): Uri? {
        val resources = context.getResources()
        val uri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(R.drawable.staging_user_profile))
                .appendPath(resources.getResourceTypeName(R.drawable.staging_user_profile))
                .appendPath(resources.getResourceEntryName(R.drawable.staging_user_profile))
                .build()
        return uri
    }

    override fun getProviders(): MutableList<String> {
        TODO("not implemented")
    }

    override fun getProviderId(): String {
        TODO("not implemented")
    }

    override fun getLastSignInTimestamp(): Long? = TODO("not implemented")

    override fun getCreationTimestamp(): Long? = TODO("not implemented")
}

class StagingLoggedOutFirebaseUser(_context: Context) : StagingLoggedInFirebaseUser(_context) {

    override fun isLoggedIn(): Boolean = false

    override fun getPhotoUrl(): Uri? = null
}