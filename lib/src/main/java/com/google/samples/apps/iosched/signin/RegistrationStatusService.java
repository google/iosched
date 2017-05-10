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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.samples.apps.iosched.rpc.registration.Registration;
import com.google.samples.apps.iosched.rpc.registration.model.RegistrationResult;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service to check the user's conference registration status as a background
 * task. (Would be implemented as an IntentService, but needs to do async work,
 * so the service lifecycle needs to be managed manually.)
 * <p>
 * To invoke, call updateRegStatusInBackground().
 */
public class RegistrationStatusService extends Service {
    private static final String TAG = makeLogTag(RegistrationStatusService.class);
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    public static void updateRegStatusInBackground(@NonNull Context ctx) {
        Intent i = new Intent(ctx, RegistrationStatusService.class);
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
        LOGD(TAG, "Refreshing registration status...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateRegStatus();
                } catch (Exception e) {
                    LOGE(TAG, "Unable to update registration status", e);
                } finally {
                    stopSelf();
                }
            }
        }).start();

        return START_REDELIVER_INTENT;  // Retry intent if service exits prematurely
    }

    /**
     * Determine if user is registered for the conference and update shared preferences with result.
     *
     * @throws IOException If unable to verify user status
     */
    private void updateRegStatus() throws Exception {
        // Build auth tokens
        GoogleAccountCredential credential = getGoogleCredential();
        String fbToken = getFirebaseToken();

        LOGD(TAG, "Firebase token: " + fbToken);
        LOGD(TAG, "Authenticating as: " + credential.getSelectedAccountName());

        // Communicate with server
        boolean isRegistered;
        isRegistered = isRegisteredAttendee(credential, fbToken);
        LOGD(TAG, "Conference attendance status: " +
                (isRegistered ? "REGISTERED" : "NOT_REGISTERED"));

        RegistrationUtils.setRegisteredAttendee(getApplicationContext(), isRegistered);
        RegistrationUtils.updateRegCheckTimestamp(this);
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
            result = registration.registrationEndpoint()
                    .registrationStatus(firebaseToken).execute();
        return result.getRegistered();
    }

    /**
     * Get Firebase token for current user
     */
    private String getFirebaseToken() throws Exception {
        FirebaseUser fbUser = mFirebaseAuth.getCurrentUser();
        if (fbUser == null) {
            throw new IOException("Firebase user not authenticated");
        }

        // Get Firebase token and wait for operation to complete
        final CountDownLatch latch = new CountDownLatch(1);
        Task<GetTokenResult> task = fbUser.getToken(false);
        task.addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                latch.countDown();
            }
        });

        latch.await();

        if (task.isSuccessful()) {
            String fbToken = task.getResult().getToken();
            if (fbToken == null) {
                throw new IOException("Received null Firebase token");
            }
            return fbToken;
        } else {
            throw task.getException();
        }
    }


    /**
     * Construct auth credential for accessing backend server.
     *
     * @return Server login credential for current user
     */
    @NonNull
    private GoogleAccountCredential getGoogleCredential() {
        GoogleAccountCredential
                credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(
                AccountUtils.AUTH_SCOPES));
        credential.setSelectedAccount(AccountUtils.getActiveAccount(this));
        return credential;
    }
}
