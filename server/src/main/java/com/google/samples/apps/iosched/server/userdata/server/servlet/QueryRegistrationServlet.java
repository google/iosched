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
package com.google.samples.apps.iosched.server.userdata.server.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import com.google.samples.apps.iosched.server.FirebaseServlet;

/**
 * A servlet that proxies registration requests to the events server. Returns whether the current
 * user is registered for the event.
 */
public class QueryRegistrationServlet extends FirebaseServlet {
    @Override
    public void doGet(HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        String userToken = req.getHeader(USER_TOKEN_HEADER);

        initFirebase();
        initFirebaseUser(userToken);

        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        getIsRegistered(user.getEmail());
    }

    private boolean getIsRegistered(String email) {
        // TODO(crmarshall): Implement event server query.
        return true;
    }
}
