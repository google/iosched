/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.server.schedule.server.servlet;

import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.server.APIUpdater;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that runs the Updater flow. Can be called from a cron job or directly.
 * The parameter force="true" forces a new update, even if the API session data has
 * not changed since the last run.
 *
 */
public class RunUpdateServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // Ignore cron when running in production.
    if ("true".equals(req.getParameter("cron")) && !Config.STAGING) {
      return;
    }

    if ("true".equals(req.getParameter("show"))) {
      boolean obfuscate = Config.SHOW_UNPUBLISHED_DATA;
      // For safety reasons, the "obfuscate" param can only be overridden in "show" mode, where
      // nothing is stored in public cloud storage:
      if ("false".equals(req.getParameter("obfuscate"))) {
        obfuscate = false;
      }
      resp.setContentType("application/json");
      new APIUpdater().run(true, obfuscate, resp.getOutputStream());
    } else {
      // If showing unpublished data, it is mandatory to obfuscate:
      new APIUpdater().run("true".equals(req.getParameter("force")),
          Config.SHOW_UNPUBLISHED_DATA, null);
      resp.setContentType("text/plain");
      resp.getWriter().println("OK");
    }
  }
}
