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

package com.google.samples.apps.iosched.server;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.tasks.Task;
import com.google.firebase.tasks.Tasks;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Wrapper class to manage Firebase interactions. */
public class FirebaseWrapper {
    private static final Logger LOG = Logger.getLogger(FirebaseWrapper.class.getName());
    public static final String USER_TOKEN_HEADER = "Authorization";
    private FirebaseToken user;

    public void initFirebase(String databaseUrl, InputStream serviceAccount) {
        FirebaseOptions options =
                new FirebaseOptions.Builder()
                        .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                        .setDatabaseUrl(databaseUrl)
                        .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    public DatabaseReference getDatabaseReference() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public void authenticateFirebaseUser(String userToken) {
        Task<FirebaseToken> tokenTask = FirebaseAuth.getInstance().verifyIdToken(userToken);
        try {
            Tasks.await(tokenTask);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "An error occurred while authenticating the user token", e);
            return;
        }
        this.user = tokenTask.getResult();
    }

    public boolean isUserAuthenticated() {
        return this.user != null;
    }

    public String getUserEmail() {
        return user == null ? null : user.getEmail();
    }

    public String getUserId() {
        return user == null ? null : user.getUid();
    }

    public String getAccessToken(InputStream serviceAccount) {
        Task<String> tokenTask = FirebaseCredentials.fromCertificate(serviceAccount)
            .getAccessToken(false);
        try {
            Tasks.await(tokenTask);
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.SEVERE, "An error occurred while generating the access token", e);
            return null;
        }
        return tokenTask.getResult();
    }
}
