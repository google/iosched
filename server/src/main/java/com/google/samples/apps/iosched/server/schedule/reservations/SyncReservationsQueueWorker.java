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
package com.google.samples.apps.iosched.server.schedule.reservations;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.server.FirebaseWrapper;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.userdata.db.ReservedSession;
import com.google.samples.apps.iosched.server.userdata.db.ReservedSession.Status;
import com.google.samples.apps.iosched.server.userdata.db.UserData;
import com.googlecode.objectify.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Worker that retrieves reservations from RTDB and updates them in datastore.
 */
public class SyncReservationsQueueWorker extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(SyncReservationsQueueWorker.class.getName());
  public static final String RESERVATIONS_KEY = "reservations";

  public FirebaseWrapper firebaseWrapper = new FirebaseWrapper();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    ServletContext context = getServletContext();

    String serviceAccountKey = context.getInitParameter("accountKey");
    LOG.info("accountKey: " + serviceAccountKey);
    InputStream serviceAccount = context.getResourceAsStream(serviceAccountKey);
    LOG.info("serviceAccount: " + serviceAccount);

    String accessToken = firebaseWrapper.getAccessToken(serviceAccount);
    if (accessToken == null) {
      LOG.severe("Unable to get access token");
      return;
    }

    // Get all existing reservations from RTDB.
    JsonArray firebaseReservations = getFirebaseReservations(accessToken);

    // For each user update reservations in datastore with reservations from RTDB.
    for (int i = 0; firebaseReservations != null && i < firebaseReservations.size(); i++) {
      JsonObject jReservation = firebaseReservations.get(i).getAsJsonObject();
      String userId = jReservation.get("userId").getAsString();
      try {
        // Deserialize reservations from RTDB.
        Map<String, ReservedSession> reservedSessions = new HashMap<>();
        if (jReservation.has(RESERVATIONS_KEY)) {
          JsonArray jUserReservations = jReservation.get(RESERVATIONS_KEY).getAsJsonArray();
          for (int j = 0; j < jUserReservations.size(); j++) {
            JsonObject jUserReservation = jUserReservations.get(j).getAsJsonObject();
            String sessionId = jUserReservation.get("sessionId").getAsString();
            Status status = mapToDatastoreStatus(jUserReservation.get("status").getAsString());
            ReservedSession reservedSession = new ReservedSession(sessionId, status,
                System.currentTimeMillis());
            reservedSessions.put(sessionId, reservedSession);
          }
        }

        // Update reservations in datastore with those from RTDB.
        UserData userData = ofy().load().type(UserData.class).id(userId).safe();
        for (String sessionId : userData.reservedSessions.keySet()) {
          if (reservedSessions.containsKey(sessionId)) {
            userData.reservedSessions.put(sessionId, reservedSessions.get(sessionId));
            reservedSessions.remove(sessionId);
          } else {
            // Mark DELETED if reservation does not exist in RTDB.
            userData.reservedSessions.get(sessionId).status = Status.DELETED;
            userData.reservedSessions.get(sessionId).timestampUTC = System.currentTimeMillis();
          }
        }
        // Add reservations from RTDB that do not exist in datastore.
        for (ReservedSession reservedSession : reservedSessions.values()) {
          userData.reservedSessions.put(reservedSession.sessionID, reservedSession);
        }

        ofy().save().entity(userData).now();
      } catch (NotFoundException e) {
        LOG.severe(e.getMessage());
      }
    }
  }

  /**
   * Map RTDB reservation status to reservation status in datastore.
   *
   * @param status Status from reservation in RTDB.
   * @return Corresponding datastore status.
   */
  private ReservedSession.Status mapToDatastoreStatus(String status) {
    if (status == null) {
      return Status.DELETED;
    }
    switch(status) {
      case "granted":
        return Status.RESERVED;
      case "waiting":
        return Status.WAITLISTED;
      default:
        return Status.DELETED;
    }
  }

  /**
   * Retrieve all reservations currently in RTDB.
   *
   * @return JSON array of all reservations in RTDB.
   */
  private JsonArray getFirebaseReservations(String accessToken) {
    try {
      String urlStr = Config.HTTP_FUNCTIONS_BASE_URL + "/getReservations?token=" + accessToken;
      URL url = new URL(urlStr);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(540000);
      connection.setReadTimeout(540000);

      InputStream stream = connection.getInputStream();
      JsonReader reader = new JsonReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
      return new JsonParser().parse(reader).getAsJsonArray();
    } catch (IOException e) {
      LOG.severe("Unable to get Firebase reservations: " + e.getMessage());
    }
    return null;
  }
}
