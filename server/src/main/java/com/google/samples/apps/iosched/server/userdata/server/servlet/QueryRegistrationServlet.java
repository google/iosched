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

import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.firebase.database.DatabaseReference;
import com.google.samples.apps.iosched.server.FirebaseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * A servlet that proxies registration requests to the events server. Returns whether the current
 * user is registered for the event.
 */
public class QueryRegistrationServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(QueryRegistrationServlet.class.getName());
    private static final String RESPONSE_REGISTERED_KEY = "registered";
    private static final String EVENT_INFO_RSVP_STATUS_KEY = "rsvp_status";
    private static final String EVENT_INFO_RSVP_STATUS_CONFIRMED_VALUE = "confirmed";

    @VisibleForTesting public FirebaseWrapper firebaseWrapper = new FirebaseWrapper();

    @VisibleForTesting
    public URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    @Override
    public void doGet(HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String userToken = req.getHeader(FirebaseWrapper.USER_TOKEN_HEADER);

        String databaseUrl = getServletContext().getInitParameter("databaseUrl");
        String serviceAccountKey = getServletContext().getInitParameter("accountKey");
        InputStream serviceAccount = getServletContext().getResourceAsStream(serviceAccountKey);

        firebaseWrapper.initFirebase(databaseUrl, serviceAccount);
        firebaseWrapper.authenticateFirebaseUser(userToken);

        if (!firebaseWrapper.isUserAuthenticated()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean isRegistered = isUserRegistered();

        // Update the user registration state in the Real-time Database.
        DatabaseReference dbRef = firebaseWrapper.getDatabaseReference();
        dbRef.child("users").child(firebaseWrapper.getUserId()).setValue(isRegistered);

        // Return the user registration state.
        JSONObject responseJson = new JSONObject();
        responseJson.put(RESPONSE_REGISTERED_KEY, String.valueOf(isRegistered));
        resp.getWriter().write(responseJson.toString());
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean isUserRegistered() throws IOException {
        JSONObject eventDelegateInfo = queryDelegateInfo();
        if (eventDelegateInfo == null) {
            return false;
        }
        String registeredStatus = eventDelegateInfo.optString(EVENT_INFO_RSVP_STATUS_KEY);
        return registeredStatus.equals(EVENT_INFO_RSVP_STATUS_CONFIRMED_VALUE);
    }

    private JSONObject queryDelegateInfo() throws IOException {
        String eventManagerUrl = getInitParameter("eventManagerUrl");
        String eventId = getInitParameter("eventId");
        String urlStr = String.format(
                "%s/%s/delegates/%s/", eventManagerUrl, eventId, firebaseWrapper.getUserEmail());

        FetchOptions fetchOptions =
                FetchOptions.Builder.doNotFollowRedirects().validateCertificate().disallowTruncate();

        HTTPRequest httpRequest = new HTTPRequest(new URL(urlStr), HTTPMethod.GET, fetchOptions);
        HTTPResponse httpResponse = urlFetchService.fetch(httpRequest);

        switch (httpResponse.getResponseCode()) {
            case 200:
                // Delegate was found.
                return new JSONObject(new String(httpResponse.getContent()));
            case 404:
                // Delegate was not found.
                return null;
            default:
                // An unexpected error occurred.
                LOG.severe(
                        String.format(
                                "Could not retrieve event delegate info: %s",
                                new String(httpResponse.getContent())));
                return null;
        }
    }
}
