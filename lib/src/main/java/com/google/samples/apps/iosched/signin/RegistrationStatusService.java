/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.signin;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.samples.apps.iosched.rpc.registration.Registration;
import com.google.samples.apps.iosched.rpc.registration.model.RegistrationResult;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.ToDoubleBiFunction;

/**
 * Service to check the user's conference registration status as a background
 * task. (Would be implemented as an IntentService, but needs to do async work,
 * so the service lifecycle needs to be managed manually.)
 * <p>
 * To invoke, call updateRegStatusInBackground().
 */
public class RegistrationStatusService extends Service {
    public static final String EXTRA_GOOGLE_ACCOUNT = "GOOGLE_ACCOUNT";
    private static final String TAG = makeLogTag(RegistrationStatusService.class);
    private static final Executor executor = Executors.newSingleThreadExecutor();

    private final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    public static void updateRegStatusInBackground(@NonNull Context ctx,
            @NonNull GoogleSignInAccount acct) {
        Intent i = new Intent(ctx, RegistrationStatusService.class);
        i.putExtra(EXTRA_GOOGLE_ACCOUNT, acct);
        ctx.startService(i);
    }

    /** Do not use, not a bound service. */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final GoogleSignInAccount acct =
                (GoogleSignInAccount) intent.getExtras().get(EXTRA_GOOGLE_ACCOUNT);
        if (acct == null) {
            throw new IllegalArgumentException(
                    "Google Account not specified");
        }

        firebaseConnect(acct);

        // TODO (see b/37012781)
        return START_NOT_STICKY;  // Retry intent if service exits prematurely
    }

    /**
     * Connect to Firebase to get login details. Once available, continue execution.
     *
     * @param acct Account for currently logged-in user
     */
    private void firebaseConnect(@NonNull final GoogleSignInAccount acct) {
        LOGD(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        // Note: Callback returned in background thread
        mFirebaseAuth.signInWithCredential(credential).addOnCompleteListener(executor, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull final Task<AuthResult> task) {
                    try {
                        if (!task.isSuccessful()) {
                            throw new IOException("Failed to sign-in to Firebase",
                                    task.getException());
                        }
                        updateRegStatus(acct);
                    } catch (IOException e) {
                        LOGE(TAG, "Unable to update registration status", e);
                        // TODO(trevorjohns): Reschedule check
                    } finally {
                        // Clean up service. Must be called when work is done.
                        stopSelf();
                    }
                }
            });
    }

    /**
     * Determine if user is registered for the conference and update shared preferences with result.
     *
     * @param acct Account for currently logged-in user
     * @throws IOException If unable to verify user status
     */
    private void updateRegStatus(@NonNull final GoogleSignInAccount acct) throws IOException {
        // Validate auth state
        if (acct.getAccount() == null) {
            throw new IOException("Google user not authenticated");
        }

        if (mFirebaseAuth.getCurrentUser() == null) {
            throw new IllegalStateException("Firebase user not authenticated");
        }

        // Build auth tokens
        String fbToken = mFirebaseAuth.getCurrentUser().getToken(false).getResult().getToken();
        GoogleAccountCredential credential = getGoogleCredential(acct);

        // Validate auth tokens
        if (fbToken == null) {
            throw new IOException("Received null Firebase token");
        }

        LOGD(TAG, "Firebase token: " + fbToken);
        LOGD(TAG, "Authenticating as: " + acct.getAccount().name);

        // Communicate with server
        boolean isRegistered = isRegisteredAttendee(credential, fbToken);
        LOGD(TAG, "Conference attendance status: " +
                (isRegistered ? "REGISTERED" : "NOT_REGISTERED"));

        RegistrationUtils.setRegisteredAttendee(getApplicationContext(), isRegistered);
    }

    /**
     * Query backend server to see if the user is registered for the conference.
     *
     * @param credential Server login credential for current user
     * @param firebaseToken Firebase login credential for current user
     * @return Whether the user is registered for the conference
     * @throws IOException If unable to verify user status
     */
    private boolean isRegisteredAttendee(@NonNull GoogleAccountCredential credential, @NonNull String firebaseToken) throws IOException {
        Registration.Builder b = new Registration.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), credential);
        Registration registration = b.build();

        RegistrationResult result;
        try {
            result = registration.registrationEndpoint()
                    .registrationStatus(firebaseToken).execute();
        } catch (IOException e) {
            throw new IOException("Failed to communicate with registration server", e);
        }
        return result.getRegistered();
    }

    /**
     * Construct auth credential for accessing backend server.
     *
     * @param acct Account for current user
     * @return Server login credential for current user
     */
    @NonNull
    private GoogleAccountCredential getGoogleCredential(@NonNull GoogleSignInAccount acct) {
        GoogleAccountCredential
                credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(
                AccountUtils.AUTH_SCOPES));
        credential.setSelectedAccount(acct.getAccount());
        return credential;
    }
}
