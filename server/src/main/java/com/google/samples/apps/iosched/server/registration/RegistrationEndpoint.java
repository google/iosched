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
package com.google.samples.apps.iosched.server.registration;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.tasks.OnCompleteListener;
import com.google.firebase.tasks.Task;
import com.google.firebase.tasks.TaskCompletionSource;
import com.google.firebase.tasks.Tasks;
import com.google.samples.apps.iosched.server.FirebaseWrapper;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.userdata.Ids;
import com.sun.org.apache.xpath.internal.operations.Bool;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.json.JSONObject;

/**
 * A servlet that proxies registration requests to the events server. Returns whether the current
 * user is registered for the event.
 */
/** Endpoint for user data storage. */
@Api(
    name = "registration",
    title = "IOSched Registration Status",
    description = "Event attendance information",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iosched.apps.samples.google.com",
        ownerName = "google.com",
        packagePath = "rpc"
    ),
    clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID,
            Ids.IOS_CLIENT_ID_DEV_IO2017,
            Ids.IOS_CLIENT_ID_DOGFOOD_IO2017,
            Ids.IOS_CLIENT_ID_GWEB_IO2017,
            Ids.IOS_CLIENT_ID_DEV_GWEB_IO2017,
            Ids.IOS_CLIENT_ID_DOGFOOD_GWEB_IO2017,
            com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID},
    audiences = {Ids.ANDROID_AUDIENCE}
)
public class RegistrationEndpoint {

    private static final Logger LOG = Logger.getLogger(RegistrationEndpoint.class.getName());
    private static final String EVENT_INFO_RSVP_STATUS_KEY = "rsvp_status";
    private static final String EVENT_INFO_RSVP_STATUS_CONFIRMED_VALUE = "confirmed";
    private static final String GOOGLER_EMAIL_DOMAIN = "@google.com";
    public static final int RTDB_RETRY_LIMIT = 5;

    @VisibleForTesting public FirebaseWrapper firebaseWrapper = new FirebaseWrapper();

    @VisibleForTesting
    public URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    @ApiMethod(path = "status", httpMethod = ApiMethod.HttpMethod.GET)
    public RegistrationResult registrationStatus(ServletContext context, @Named("firebaseUserToken") String firebaseUserToken)
        throws IOException, ForbiddenException, ExecutionException, InterruptedException, InternalServerErrorException {

        String databaseUrl = context.getInitParameter("databaseUrl");
        LOG.info("databaseUrl: " + databaseUrl);
        String serviceAccountKey = context.getInitParameter("accountKey");
        LOG.info("accountKey: " + serviceAccountKey);
        InputStream serviceAccount = context.getResourceAsStream(serviceAccountKey);
        LOG.info("serviceAccount: " + serviceAccount);

        firebaseWrapper.initFirebase(databaseUrl, serviceAccount);
        firebaseWrapper.authenticateFirebaseUser(firebaseUserToken);

        if (!firebaseWrapper.isUserAuthenticated()) {
            throw new ForbiddenException("Not authenticated");
        }

        boolean isRegistered = isUserRegistered(context);

        final TaskCompletionSource<Boolean> isRegisteredTCS = new TaskCompletionSource<>();
        final Task<Boolean> isRegisteredTCSTask = isRegisteredTCS.getTask();
        // Update the user registration state in the Real-time Database.
        DatabaseReference dbRef = firebaseWrapper.getDatabaseReference();
        int rtdbRetries = 0;
        while (rtdbRetries < RTDB_RETRY_LIMIT) {
            dbRef.child("users").child(firebaseWrapper.getUserId()).setValue(isRegistered)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            isRegisteredTCS.setResult(true);
                        } else {
                            isRegisteredTCS.setResult(false);
                        }
                    }
                });
            // If writing to RTDB was successful break out.
            if (Tasks.await(isRegisteredTCSTask)) {
                break;
            }
            LOG.info("Writing to RTDB has failed.");
            rtdbRetries++;
        }

        // If retry limit was reached return false, user will have to try again if they were
        // indeed registered.
        if (rtdbRetries >= RTDB_RETRY_LIMIT) {
            throw new InternalServerErrorException("Unable to write registration status to RTDB.");
        } else {
            // Return the user registration state.
            return new RegistrationResult(isRegistered);
        }
    }

    private boolean isUserRegistered(ServletContext context) throws IOException {
        // Session seat reservations are gated on the result of this method. If a user is registered
        // they can reserve otherwise they cannot. Googlers however are not allowed to reserve even
        // if they are registered. This check ensures that Googlers are marked as not registered so
        // that they will not be able to reserve seats.
        // TODO: Allow this to return the accurate registration status and handle reservation status
        // TODO: elsewhere.
        if (firebaseWrapper.getUserEmail().endsWith(GOOGLER_EMAIL_DOMAIN) ||
            Arrays.asList(Config.DOGFOOD_RESERVATION_WHITELIST)
                .contains(firebaseWrapper.getUserEmail())) {
            // TODO(arthurthompson): Return false here when reservation dogfood is over.
            // TODO                  Returning true here so that all Googlers can test reservations.
            // return false;
            return true;
        }

        List<JSONObject> eventDelegateInfo = queryDelegateInfo(context);
        for(JSONObject delegateInfo : eventDelegateInfo) {
            String registeredStatus = delegateInfo.optString(EVENT_INFO_RSVP_STATUS_KEY);
            if (registeredStatus.equals(EVENT_INFO_RSVP_STATUS_CONFIRMED_VALUE)) {
                return true;
            }
        }
        return false;
    }

    private ImmutableList<JSONObject> queryDelegateInfo(ServletContext context) throws IOException {
        String eventManagerUrl = context.getInitParameter("eventManagerUrl");
        String[] eventIds = context.getInitParameter("eventIds").split(",");

        FetchOptions fetchOptions =
                FetchOptions.Builder.doNotFollowRedirects().validateCertificate().disallowTruncate();

        ImmutableList.Builder delegateInfo = ImmutableList.<JSONObject>builder();
        for (String eventId : eventIds) {
            String urlStr = String.format("%s/%s/delegates/%s/",
                    eventManagerUrl, eventId.trim(), firebaseWrapper.getUserEmail());

            HTTPRequest httpRequest = new HTTPRequest(new URL(urlStr), HTTPMethod.GET, fetchOptions);
            HTTPResponse httpResponse = urlFetchService.fetch(httpRequest);

            switch (httpResponse.getResponseCode()) {
                case 200:
                    // Delegate was found.
                    delegateInfo.add(new JSONObject(new String(httpResponse.getContent())));
                case 404:
                    // Delegate was not found.
                    break;
                default:
                    // An unexpected error occurred.
                    LOG.severe(
                            String.format(
                                    "Could not retrieve event delegate info: %s",
                                    new String(httpResponse.getContent())));
                    break;
            }
        }
        return delegateInfo.build();
    }
}
