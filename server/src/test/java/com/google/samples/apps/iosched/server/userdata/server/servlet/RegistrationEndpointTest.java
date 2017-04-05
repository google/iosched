package com.google.samples.apps.iosched.server.userdata.server.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.firebase.database.DatabaseReference;
import com.google.samples.apps.iosched.server.FirebaseWrapper;
import com.google.samples.apps.iosched.server.registration.RegistrationEndpoint;
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
    private static final String REGISTERED_SERVLET_RESPONSE = "{\"registered\":\"true\"}";
    private static final String UNREGISTERED_SERVLET_RESPONSE = "{\"registered\":\"false\"}";

    private static final String EVENT_MANAGER_URL = "http://events.example.com";
    private static final String EVENT_ID = "event_id";
    private static final String USER_ID = "user_id";
    private static final String USER_EMAIL = "email@example.com";
    private static final String DATABASE_URL = "database_url";
    private static final String SERVICE_ACCOUNT = "service_account";
    private static final String SERVICE_ACCOUNT_KEY = "service_account_key";

    @Mock private HttpServletRequest mockRequest;
    @Mock private HttpServletResponse mockResponse;
    @Mock private FirebaseWrapper mockFirebaseWrapper;
    @Mock private ServletConfig mockServletConfig;
    @Mock private ServletContext mockServletContext;
    @Mock private URLFetchService mockUrlFetchService;
    @Mock private HTTPResponse mockHttpResponse;
    @Mock private PrintWriter mockWriter;
    @Mock private DatabaseReference mockDatabaseReference;

    RegistrationEndpoint servlet = new RegistrationEndpoint();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // Set up servlet environment mocks.
        when(mockServletConfig.getServletContext()).thenReturn(mockServletContext);
        when(mockServletContext.getInitParameter("databaseUrl")).thenReturn(DATABASE_URL);
        when(mockServletContext.getInitParameter("accountKey")).thenReturn(SERVICE_ACCOUNT);
        when(mockServletContext.getResourceAsStream(SERVICE_ACCOUNT))
                .thenReturn(new ByteArrayInputStream(SERVICE_ACCOUNT_KEY.getBytes()));
        when(mockServletConfig.getInitParameter("eventId")).thenReturn(EVENT_ID);
        when(mockServletConfig.getInitParameter("eventManagerUrl")).thenReturn(EVENT_MANAGER_URL);
        when(mockResponse.getWriter()).thenReturn(mockWriter);

        // Set up Firebase-related mocks.
        when(mockUrlFetchService.fetch(any(HTTPRequest.class))).thenReturn(mockHttpResponse);
        when(mockFirebaseWrapper.getDatabaseReference()).thenReturn(mockDatabaseReference);
        when(mockDatabaseReference.child(any(String.class))).thenReturn(mockDatabaseReference);
        when(mockFirebaseWrapper.getUserId()).thenReturn(USER_ID);
        when(mockFirebaseWrapper.getUserEmail()).thenReturn(USER_EMAIL);

        // Configure test servlet.
        servlet.init(mockServletConfig);
        servlet.firebaseWrapper = mockFirebaseWrapper;
        servlet.urlFetchService = mockUrlFetchService;
    }

    @Test
    public void testGetRegisteredUser() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(200);
        when(mockHttpResponse.getContent()).thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
        verify(mockResponse).setContentType("application/json");
        verify(mockWriter).write(REGISTERED_SERVLET_RESPONSE);
    }

    @Test
    public void testGetNoAuth() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(false);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testGetUserNotInEventSystem() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(404);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
        verify(mockResponse).setContentType("application/json");
        verify(mockWriter).write(UNREGISTERED_SERVLET_RESPONSE);
    }

    @Test
    public void testGetUserNotRegistered() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(200);
        when(mockHttpResponse.getContent()).thenReturn(UNREGISTERED_EVENT_RESPONSE.getBytes());

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
        verify(mockResponse).setContentType("application/json");
        verify(mockWriter).write(UNREGISTERED_SERVLET_RESPONSE);
    }

    @Test
    public void testGetQueryEventServiceError() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(500);
        when(mockHttpResponse.getContent()).thenReturn(EMPTY_EVENT_RESPONSE.getBytes());

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
        verify(mockResponse).setContentType("application/json");
        verify(mockWriter).write(UNREGISTERED_SERVLET_RESPONSE);
    }

    @Test
    public void testGetQueryEventService() throws Exception {
        when(mockFirebaseWrapper.isUserAuthenticated()).thenReturn(true);
        when(mockHttpResponse.getResponseCode()).thenReturn(200);
        when(mockHttpResponse.getContent()).thenReturn(REGISTERED_EVENT_RESPONSE.getBytes());

        servlet.doGet(mockRequest, mockResponse);

        ArgumentCaptor<HTTPRequest> httpRequestCaptor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(mockUrlFetchService).fetch(httpRequestCaptor.capture());
        assertEquals(
                httpRequestCaptor.getValue().getURL().toString(),
                "http://events.example.com/event_id/delegates/email@example.com/");
    }
}
