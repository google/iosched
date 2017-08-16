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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.response.ForbiddenException;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.firebase.database.DatabaseReference;
import com.google.samples.apps.iosched.server.FirebaseWrapper;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegistrationEndpointTest {
    private static final String REGISTERED_EVENT_RESPONSE = "{\"rsvp_status\":\"confirmed\"}";
    private static final String UNREGISTERED_EVENT_RESPONSE = "{\"rsvp_status\":\"invited\"}";
    private static final String EMPTY_EVENT_RESPONSE = "";

    private static final String EVENT_MANAGER_URL = "http://events.example.com";
    private static final String EVENT_ID_1 = "event_id_1";
    private static final String EVENT_ID_2 = "event_id_2";
    private static final String USER_ID = "user_id";
    private static final String USER_EMAIL = "email@example.com";
    private static final String DATABASE_URL = "database_url";
    private static final String SERVICE_ACCOUNT = "service_account";
    private static final String SERVICE_ACCOUNT_KEY = "service_account_key";
    private static final String FIREBASE_TOKEN = "token";

    @Mock private HttpServletRequest mockRequest;
    @Mock private HttpServletResponse mockResponse;
    @Mock private FirebaseWrapper mockFirebaseWrapper;
    @Mock private ServletConfig mockServletConfig;
    @Mock private ServletContext mockServletContext;
    @Mock private URLFetchService mockUrlFetchService;
    @Mock private HTTPResponse mockHttpResponse;
    @Mock private PrintWriter mockWriter;
    @Mock private DatabaseReference mockDatabaseReference;

    RegistrationEndpoint endpoint = new RegistrationEndpoint();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // Set up servlet environment mocks.
        when(mockServletContext.getInitParameter("databaseUrl")).thenReturn(DATABASE_URL);
        when(mockServletContext.getInitParameter("accountKey")).thenReturn(SERVICE_ACCOUNT);
        when(mockServletContext.getResourceAsStream(SERVICE_ACCOUNT))
                .thenReturn(new ByteArrayInputStream(SERVICE_ACCOUNT_KEY.getBytes()));
        when(mockServletContext.getInitParameter("eventIds"))
                .thenReturn(EVENT_ID_1 + ", " + EVENT_ID_2);
        when(mockServletContext.getInitParameter("eventManagerUrl")).thenReturn(EVENT_MANAGER_URL);
        when(mockResponse.getWriter()).thenReturn(mockWriter);

        // Set up Firebase-related mocks.
        when(mockUrlFetchService.fetch(any(HTTPRequest.class))).thenReturn(mockHttpResponse);
        when(mockFirebaseWrapper.getDatabaseReference()).thenReturn(mockDatabaseReference);
        when(mockDatabaseReference.child(any(String.class))).thenReturn(mockDatabaseReference);
        when(mockFirebaseWrapper.getUserId()).thenReturn(USER_ID);
        when(mockFirebaseWrapper.getUserEmail()).thenReturn(USER_EMAIL);

        // Configure test servlet;
        endpoint.firebaseWrapper = mockFirebaseWrapper;
        endpoint.urlFetchService = mockUrlFetchService;
    }

    @Test
    public void testGetRegisteredUser() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(200);
        when(mockHttpResponse.getContent()).thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());
        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertTrue(result.isRegistered());
    }

    @Test
    public void testGetRegisteredUserInSecondEventAfterNotFound() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode())
                .thenReturn(404)
                .thenReturn(200);
        when(mockHttpResponse.getContent())
                .thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());

        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertTrue(result.isRegistered());
    }

    @Test
    public void testGetRegisteredUserInSecondEventAfterUnregistered() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode())
                .thenReturn(200)
                .thenReturn(200);
        when(mockHttpResponse.getContent())
                .thenReturn(UNREGISTERED_EVENT_RESPONSE.getBytes())
                .thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());

        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertTrue(result.isRegistered());
    }

    @Test
    public void testGetRegisteredUserInSecondEventAfterServiceError() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode())
                .thenReturn(500)
                .thenReturn(200);
        when(mockHttpResponse.getContent())
                .thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());

        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertTrue(result.isRegistered());
    }

    @Test
    public void testGetNoAuth() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(false);

        try {
            endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);
            fail();
        } catch (ForbiddenException e) {
            // Expected exception.
        }
    }

    @Test
    public void testGetUserNotInEventSystem() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(404);

        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertFalse(result.isRegistered());
    }

    @Test
    public void testGetUserNotRegistered() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(200);
        when(mockHttpResponse.getContent()).thenReturn(UNREGISTERED_EVENT_RESPONSE.getBytes());

        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertFalse(result.isRegistered());
    }

    @Test
    public void testGetQueryEventServiceError() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(500);
        when(mockHttpResponse.getContent()).thenReturn(EMPTY_EVENT_RESPONSE.getBytes());

        RegistrationResult result = endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        assertFalse(result.isRegistered());
    }

    @Test
    public void testGetQueryEventService() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(404).thenReturn(200);
        when(mockHttpResponse.getContent()).thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());

        endpoint.registrationStatus(mockServletContext, FIREBASE_TOKEN);

        ArgumentCaptor<HTTPRequest> httpRequestCaptor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(mockUrlFetchService, times(2)).fetch(httpRequestCaptor.capture());
        assertEquals(
                httpRequestCaptor.getAllValues().get(0).getURL().toString(),
                "http://events.example.com/event_id_1/delegates/email@example.com/");
        assertEquals(
                httpRequestCaptor.getAllValues().get(1).getURL().toString(),
                "http://events.example.com/event_id_2/delegates/email@example.com/");
    }
}
