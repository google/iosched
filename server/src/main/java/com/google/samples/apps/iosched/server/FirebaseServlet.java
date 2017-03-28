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

import javax.servlet.http.HttpServlet;

/**
 * An abstract Servlet that handles connections and authentication with firebase.
 */
public abstract class FirebaseServlet extends HttpServlet {
    protected static final String USER_TOKEN_HEADER = "Authorization";
    protected FirebaseToken user = null;

    protected void initFirebase() {
        // Initialize the Firebase SDK
        String databaseUrl = getServletContext().getInitParameter("databaseUrl");
        String serviceAccountKey = getServletContext().getInitParameter("accountKey");
        InputStream serviceAccount = getServletContext().getResourceAsStream(serviceAccountKey);

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                .setDatabaseUrl(databaseUrl)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    protected DatabaseReference getDatabaseReference() {
        return FirebaseDatabase.getInstance().getReference();
    }

    protected void initFirebaseUser(String userToken) {
        Task<FirebaseToken> tokenTask = FirebaseAuth.getInstance().verifyIdToken(userToken);
        try {
          Tasks.await(tokenTask);
        } catch (Exception e) {
            log("An error occurred while authenticating the user token", e);
            return;
        }
        this.user = tokenTask.getResult();
    }
}