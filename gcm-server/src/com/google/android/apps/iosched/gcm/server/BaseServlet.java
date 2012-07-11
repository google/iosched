/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.iosched.gcm.server;

import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Skeleton class for all servlets in this package.
 */
abstract class BaseServlet extends HttpServlet {
    protected final Logger logger = Logger.getLogger(getClass().getSimpleName());

    protected String getParameter(HttpServletRequest req, String parameter)
            throws ServletException {
        String value = req.getParameter(parameter);
        if (value == null || value.trim().isEmpty()) {
            StringBuilder parameters = new StringBuilder();
            @SuppressWarnings("unchecked")
            Enumeration<String> names = req.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String param = req.getParameter(name);
                parameters.append(name).append("=").append(param).append("\n");
            }
            logger.info("parameters: " + parameters);
            throw new ServletException("Parameter " + parameter + " not found");
        }
        return value.trim();
    }

    protected void setSuccess(HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.setContentLength(0);
    }
}
