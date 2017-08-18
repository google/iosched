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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 * Worker that call function which pushes updated reservations from RTDB to onsite vendor.
 */
public class PushReservationQueueWorker extends HttpServlet {

  private static final Logger log = Logger.getLogger(LoadSessionsServlet.class.getName());
  private static final String functionUrl = "https://us-central1-gweb-io2017.cloudfunctions.net/"
      + "updateOnsiteReservations";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    URL url = new URL(functionUrl);
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setReadTimeout(540000);
    httpURLConnection.setConnectTimeout(540000);
    String respMessage = IOUtils.toString(httpURLConnection.getInputStream());
    log.info(respMessage);
    resp.setStatus(HttpServletResponse.SC_OK);
  }

}
