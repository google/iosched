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

import static com.google.firebase.database.Transaction.success;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction.Handler;
import com.google.firebase.database.Transaction.Result;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.tasks.Task;
import com.google.firebase.tasks.TaskCompletionSource;
import com.google.firebase.tasks.Tasks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.reservations.model.Reservation;
import com.google.samples.apps.iosched.server.schedule.reservations.model.Seats;
import com.google.samples.apps.iosched.server.schedule.reservations.model.Session;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 * LoadSessionsServlet loads sessions into the reservation system.
 * Sessions retrieved from the CMS are stored in JSON files in Google Cloud Storage. This Servlet
 * retrieves the sessions and rooms from the JSON files stored in Google Cloud Storage and inserts
 * them into Firebase RTDB to make them ready to accept reservations.
 */
public class LoadSessionsServlet extends HttpServlet {

  public static final String START_TIME_KEY = "startTimestamp";
  private static final String END_TIME_KEY = "endTimestamp";
  // 80% of room capacity is made available for reservations.
  private static final double RESERVABLE_CAPACITY_PERCENTAGE = 0.8;
  public static final String MANIFEST_FILENAME = "manifest_v1.json";
  public static final String PATH_SESSIONS = "sessions";
  private static final String RESERVATION_TYPE_WAITING = "waiting";
  private static final String RESERVATION_TYPE_GRANTED = "granted";
  public static final String ROOMS_KEY = "rooms";
  public static final String SESSIONS_KEY = "sessions";
  public static final String ROOM_KEY = "room";
  public static final String ID_KEY = "id";
  public static final String TITLE_KEY = "title";
  public static final String CAPACITY_KEY = "capacity";
  public static final String NAME_KEY = "name";
  private final UserService userService = UserServiceFactory.getUserService();
  private static final HashSet<String> nonAdminUsers = new HashSet<>();
  private static final Logger log = Logger.getLogger(LoadSessionsServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    // Check that only admins or other allowed users can make this call.
    if (!performBasicChecking(req, resp)) {
      return;
    }

    // Pull existing session and room data from Google Cloud Storage.
    JsonParser jsonParser = new JsonParser();

    String manifestStr = IOUtils.toString(new URL(Config.CLOUD_STORAGE_BASE_URL +
        MANIFEST_FILENAME).openStream());
    JsonElement jManifest = jsonParser.parse(manifestStr);
    JsonArray jDataFiles = jManifest.getAsJsonObject().get("data_files").getAsJsonArray();
    String sessionDataFileName = null;
    for (int i = 0; i < jDataFiles.size(); i++) {
      String filename = jDataFiles.get(i).getAsString();
      if (filename.startsWith("session_data")) {
        sessionDataFileName = filename;
        break;
      }
    }

    if (sessionDataFileName == null) {
      // Unable to find session data to load.
      resp.setContentType("text/plain");
      resp.getWriter().println("Unable to find session data to load.");
      return;
    }

    // Get session and room data from file in GCS.
    String sessionDataStr = IOUtils.toString(new URL(Config.CLOUD_STORAGE_BASE_URL +
        sessionDataFileName).openStream());
    JsonElement jSessionData = jsonParser.parse(sessionDataStr);

    // Extract rooms and sessions
    final JsonArray jRooms = jSessionData.getAsJsonObject().get(ROOMS_KEY).getAsJsonArray();
    final JsonArray jSessions = jSessionData.getAsJsonObject().get(SESSIONS_KEY).getAsJsonArray();

    // Only sessions that are of type TYPE_SESSIONS can be reserved so remove those that do not
    // have this type.
    List<JsonElement> sessionsToRemove = new ArrayList<>();
    for (JsonElement jSession : jSessions) {
      // TODO: The keynote should not have a type of TYPE_SESSIONS. Remove this hack once
      // TODO: keynotes have a better type.
      if (jSession.getAsJsonObject().get("id").getAsString().startsWith("__keynote")) {
        sessionsToRemove.add(jSession);
        continue;
      }

      JsonArray jTags = jSession.getAsJsonObject().get("tags").getAsJsonArray();
      boolean isReservable = false;
      for (JsonElement jTag : jTags) {
        if (jTag.getAsString().equals("TYPE_SESSIONS")) {
          isReservable = true;
          break;
        }
      }
      if (!isReservable) {
        sessionsToRemove.add(jSession);
      }
    }

    for (JsonElement jsonElement : sessionsToRemove) {
      jSessions.remove(jsonElement);
    }

    log.info("Non-Reservable session count: " + sessionsToRemove.size());
    log.info("Reservable session count: " + jSessions.size());

    resp.setContentType("text/plain");
    resp.getWriter().println("Room and session data retrieved.");

    // Initialize Firebase app with service account credentials.
    FirebaseOptions options = new FirebaseOptions.Builder()
        .setCredential(FirebaseCredentials.fromCertificate(getServletContext().getResourceAsStream(
            "/WEB-INF/io2017-backend-dev-serv-cred.json")))
        .setDatabaseUrl("https://io2017-backend-dev.firebaseio.com/")
        .build();

    try {
      FirebaseApp.initializeApp(options);
      log.info("Initialized Firebase");
    } catch (Exception e) {
      // Firebase Instance already exists.
      log.info("Firebase already initialized");
    }

    // Session retrieval task.
    final TaskCompletionSource<Map<String, Session>> sessionsTaskCompletionSource =
        new TaskCompletionSource<>();
    final Task<Map<String, Session>> sessionsTask = sessionsTaskCompletionSource.getTask();

    // Get Firebase Database reference to sessions path, add listener for single event.
    final FirebaseDatabase defaultDatabase = FirebaseDatabase.getInstance();
    defaultDatabase.getReference(PATH_SESSIONS).addListenerForSingleValueEvent(
        new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            Map<String, Session> sessions = new HashMap<>();
            for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
              Session s = sessionSnapshot.getValue(Session.class);
              sessions.put(sessionSnapshot.getKey(), s);
            }

            sessionsTaskCompletionSource.setResult(sessions);
          }

          @Override
          public void onCancelled(DatabaseError databaseError) {
            log.warning("RTDB error: " + databaseError.getMessage());
          }
        });

    try {
      // Wait for the sessions from RTDB.
      Map<String, Session> rtdbSessions = Tasks.await(sessionsTask);

      // Update sessions in RTDB with values from GCS.
      for (String sessionId : rtdbSessions.keySet()) {

        // Update session task.
        final TaskCompletionSource<Void> updateSessionTCS = new TaskCompletionSource<>();
        final Task<Void> updateSessionTCSTask = updateSessionTCS.getTask();

        // Check that GCS has a matching session and room as the one from RTDB.
        JsonObject jSession = getSession(sessionId, jSessions);
        if (jSession != null) {
          JsonObject jRoom = getRoom(jSession, jRooms);
          if (jRoom != null) {
            final int gcsCap = Long.valueOf(Math.round(jRoom.get(CAPACITY_KEY)
                .getAsInt() * RESERVABLE_CAPACITY_PERCENTAGE)).intValue();
            final String gcsRoomName = jRoom.get(NAME_KEY).getAsString();
            final long gcsStartTime = getTimeInMillis(jSession, START_TIME_KEY);
            final long gcsEndTime = getTimeInMillis(jSession, END_TIME_KEY);
            final String gcsTitle = jSession.get(TITLE_KEY).getAsString();

            // Update session in a transaction.
            defaultDatabase.getReference().child(PATH_SESSIONS).child(sessionId)
                .runTransaction(new Handler() {
              @Override
              public Result doTransaction(MutableData mutableData) {
                Session session = mutableData.getValue(Session.class);
                if (session != null) {
                  // Update start and end times of session in RTDB.
                  session.time_start = gcsStartTime;
                  session.time_end = gcsEndTime;

                  // Update session title.
                  session.title = gcsTitle;
                  // Update session room name.
                  session.room_name = gcsRoomName;

                  int currResCount = session.seats.reserved;
                  boolean currHasSeats = session.seats.seats_available;

                  if (currResCount > gcsCap) {
                    // If there are to many reservations move extras to waitlist.
                    int resToMove = currResCount - gcsCap;
                    moveReservationsToWaitList(resToMove, session);
                  } else if (currResCount < gcsCap && !currHasSeats) {
                    // If there is space and a waitlist, promote as many as possible from the
                    // waitlist.
                    int numSeatsAvailable = gcsCap - currResCount;
                    promoteFromWaitList(currResCount, numSeatsAvailable, session);
                  }

                  // Update session capacity.
                  session.seats.capacity = gcsCap;
                  mutableData.setValue(session);
                }
                return success(mutableData);
              }

              @Override
              public void onComplete(DatabaseError databaseError, boolean b,
                  DataSnapshot dataSnapshot) {
                // Signal that session update is complete.
                updateSessionTCS.setResult(null);
              }
            });

            // Wait for session update to complete.
            Tasks.await(updateSessionTCSTask);
          }

          // Remove updated sessions from list of sessions to be added.
          jSessions.remove(jSession);
        }
      }
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }

    // Add all sessions that were retrieved from GCS but did not already in RTDB.
    for (int i = 0; i < jSessions.size(); i++) {
      JsonObject jSession = jSessions.get(i).getAsJsonObject();
      String sessionId = jSession.get(ID_KEY).getAsString();
      String sessionTitle = jSession.get(TITLE_KEY).getAsString();
      String roomId = jSession.get(ROOM_KEY).getAsString();
      JsonObject jRoom = getRoom(roomId, jRooms);
      int capacity = Long.valueOf(Math.round(jRoom.get(CAPACITY_KEY)
          .getAsInt() * RESERVABLE_CAPACITY_PERCENTAGE)).intValue();
      String sessionRoomName = jRoom.get(NAME_KEY).getAsString();

      long startTime = getTimeInMillis(jSession, START_TIME_KEY);
      long endTime = getTimeInMillis(jSession, END_TIME_KEY);

      Session session = new Session();
      session.title = sessionTitle;
      session.room_name = sessionRoomName;
      session.time_end = endTime;
      session.time_start = startTime;

      Seats seats = new Seats();
      seats.capacity = capacity;
      seats.reserved = 0;
      seats.seats_available = true;
      seats.waitlisted = false;

      session.seats = seats;

      defaultDatabase.getReference(PATH_SESSIONS).child(sessionId)
          .setValue(session);
    }

    resp.getWriter().println("Sessions added to RTDB.");
  }

  /**
   * This method is called if there are more reservations than reservable seats for a session.
   * The excess reservations will be moved to the waitlist. Since their last_status_changed value
   * will remain the same the moved reservations will be at the front of the waitlist.
   *
   * @param numOfRes Number of existing reservations to move to the waitlist.
   * @param session Session to update.
   */
  private void moveReservationsToWaitList(int numOfRes, Session session) {
    Map<String, Reservation> reservations = session.reservations;
    // Get reservations.
    List<Reservation> granted = getReservationsByType(reservations, RESERVATION_TYPE_GRANTED);
    // Sort by last status changed, reverse order.
    Collections.sort(granted, Collections.<Reservation>reverseOrder());
    // Put (numOfRes) attendees on the waitlist.
    for (int i = 0; i < numOfRes; i++) {
      Reservation reservation = granted.get(i);
      reservation.status = RESERVATION_TYPE_WAITING;
    }
    session.seats.reserved = session.seats.reserved - numOfRes;
    session.seats.seats_available = false;
    session.seats.waitlisted = true;
    // TODO(arthurthompson): send notification to demoted attendees
  }

  /**
   * This method is called if there are available seats to be reserved and waitlisted reservations.
   * All waitlisted reservations will be promoted to reserved once there are still seats available.
   *
   * @param currResCount Current number of reserved seats.
   * @param numSeatsAvailable Number of seats available to be reserved.
   * @param session Session to be updated.
   */
  private void promoteFromWaitList(int currResCount, int numSeatsAvailable,
      Session session) {
    Map<String, Reservation> reservations = session.reservations;
    // Get waiting reservations.
    List<Reservation> waiting = getReservationsByType(reservations, RESERVATION_TYPE_WAITING);
    // Sort by last status changed.
    Collections.sort(waiting);
    int promoCount = 0;
    for (int i = 0; i < numSeatsAvailable && i < waiting.size(); i++) {
      Reservation reservation = waiting.get(i);
      reservation.status = RESERVATION_TYPE_GRANTED;
      promoCount++;
    }

    session.seats.reserved = currResCount + promoCount;

    if (promoCount == numSeatsAvailable) {
      session.seats.seats_available = false;
    } else {
      session.seats.seats_available = true;
    }

    int stillWaiting = waiting.size() - promoCount;
    if (stillWaiting > 0) {
      session.seats.waitlisted = true;
    } else {
      session.seats.waitlisted = false;
    }
    // TODO(arthurthompson): send notification to promoted attendees
  }

  /**
   * Filter reservations by type, expected values are "waiting" and "granted".
   *
   * @param reservations Map of all reservations.
   * @param reservationType Type of reservations to keep.
   * @return List of reservations matching the type defined.
   */
  private List<Reservation> getReservationsByType(Map<String, Reservation> reservations,
      String reservationType) {
    List<Reservation> grantedReservations = new ArrayList<>();
    for (Reservation res : reservations.values()) {
      if (res.status != null && res.status.equals(reservationType)) {
        grantedReservations.add(res);
      }
    }
    return grantedReservations;
  }

  /**
   * Get JSON representation of a room with matching ID as the room in the passed session.
   *
   * @param jSession Session that contains the desired room ID.
   * @param jRooms Array of rooms.
   * @return JSON room if found, null otherwise.
   */
  private JsonObject getRoom(JsonObject jSession, JsonArray jRooms) {
    String roomId = jSession.get(ROOM_KEY).getAsString();
    return getRoom(roomId, jRooms);
  }

  /**
   * Get JSON representation of a room with matching ID as the room in the passed session.
   *
   * @param roomId ID of the desired room.
   * @param jRooms Array of rooms.
   * @return JSON room if found, null otherwise.
   */
  private JsonObject getRoom(String roomId, JsonArray jRooms) {
    for (int i = 0; i < jRooms.size(); i++) {
      JsonObject jRoom = jRooms.get(i).getAsJsonObject();
      if (jRoom.get(ID_KEY).getAsString().equals(roomId)) {
        return jRoom;
      }
    }
    return null;
  }

  /**
   * Find a session with matching ID from an array of sessions.
   *
   * @param sessionId ID to use to find desired session.
   * @param jSessions Array of sessions to search.
   * @return JSON session if found, null otherwise.
   */
  private JsonObject getSession(String sessionId, JsonArray jSessions) {
    for (int i = 0; i < jSessions.size(); i++) {
      JsonObject jSession = jSessions.get(i).getAsJsonObject();
      if (jSession.get(ID_KEY).getAsString().equals(sessionId)) {
        return jSession;
      }
    }
    return null;
  }

  /**
   * Convert formatted dates to easily comparable longs.
   *
   * @param jSession JSON session with times to convert.
   * @param timeKey Field name of time to convert. Usually startTimestamp and endTimestamp.
   * @return long representation of date matching timeKey.
   */
  private long getTimeInMillis(JsonObject jSession, String timeKey) {
    String timeStr = jSession.get(timeKey).getAsString();
    SimpleDateFormat outputFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    try {
      Date startTime = outputFormat.parse(timeStr);
      return startTime.getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return -1;
  }

  // Helpers to ensure that only Admins call this servlet.
  private boolean performBasicChecking(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    if (!userService.isUserLoggedIn()) {
      resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
      return false;
    }
    if (!isValidUser()) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sorry, your user has no permission. "
          + "If you think you should have, you know who to contact to get it.");
      return false;
    }
    // Ignore cron when running in production.
    if ("true".equals(req.getParameter("cron")) && !Config.STAGING) {
      return false;
    }
    return true;
  }

  private boolean isValidUser() {
    return userService.isUserLoggedIn() &&
        ( userService.isUserAdmin() ||
            nonAdminUsers.contains(userService.getCurrentUser().getEmail()));
  }
}
