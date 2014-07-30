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
package com.google.android.apps.iosched.gcm.server.admin;

import com.google.android.apps.iosched.gcm.server.BaseServlet;
import com.google.android.apps.iosched.gcm.server.db.DeviceStore;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AdminServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.print("<html><body>");
        out.print("<head><title>IOSched GCM Server</title>");
        out.print("</head>");
        String status = (String) req.getAttribute("status");
        if (status != null) {
            out.print(status);
        }
        out.print("</body></html>");
        out.print("<h2>" + DeviceStore.getDeviceCount() + " device(s) registered!</h2>");
        out.print("<form method='POST' action='/scheduleupdate'>");
        out.print("<table>");
        out.print("<tr>");
        out.print("<td>Key:</td>");
        out.print("<td><input type='password' name='key' size='80'/></td>");
        out.print("</tr>");
        out.print("<tr>");
        out.print("<td>Announcement:</td>");
        out.print("<td><input type='text' name='announcement' size='80'/></td>");
        out.print("</tr>");
        out.print("</table>");
        out.print("<br/>");
        out.print("<input type='submit' value='Send Message' />");
        out.print("</form>");
        resp.addHeader("X-FRAME-OPTIONS", "DENY");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
