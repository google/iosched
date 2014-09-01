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
package com.google.android.apps.iosched.gcm.server;

import com.google.android.apps.iosched.gcm.server.db.ApiKeyInitializer;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

public class AuthHelper {

  /** Keys that can be used to request GCM registration */
  private static final String[][] REGISTRATION_KEYS = {
    {"REG-madeinny", "Android app registration key"},
  };



  /**
   * Extract authorization info from the HTTP request header or query param.
   * @param req
   * @param servletConfig
   * @return null if no authorization found. An AuthInfo with admin set to true if a valid admin key
   * is used, or set to false if any non-admin key is present.
   */
  public static AuthInfo processAuthorization(HttpServletRequest req, ServletConfig servletConfig) {
    // Authenticate request
    // Auth key defaults to the 'key' query parameter
    String authKey = req.getParameter("key");

    String authHeader = req.getHeader("Authorization");
    if (authHeader != null) {
        // Use 'Authorization: key=...' header
        String splitHeader[] = authHeader.split("=");
        if ("key".equals(splitHeader[0])) {
            authKey = splitHeader[1];
        }
    }
    if (authKey == null) {
      return null;
    }

      String configedAdminKey = (String) servletConfig.getServletContext().getAttribute(ApiKeyInitializer.ATTRIBUTE_ADMIN_AUTH);
      if(configedAdminKey.equals(authKey))
      {
          AuthInfo info = new AuthInfo(configedAdminKey, "Updater AppEngine app");
          info.permAdmin = true;
          return info;
      }
     /* for (String[] candidateKey : ADMIN_KEYS) {
        if (candidateKey[0].equals(authKey)) {
            // caller is an admin
            AuthInfo info = new AuthInfo(candidateKey[0], candidateKey[1]);
            info.permAdmin = true;
            return info;
        }
    }*/

    for (String[] candidateKey : REGISTRATION_KEYS) {
      if (candidateKey[0].equals(authKey)) {
          // caller is using a valid registration API key, so they
          // can register
        AuthInfo info = new AuthInfo(candidateKey[0], candidateKey[1]);
        info.permRegister = true;
        return info;
      }
    }

    // the key is not a special admin or registration key, so all the user can do is
    // send message to themselves
    AuthInfo info = new AuthInfo(authKey, "User");
    info.permSendSelfMessage = true;
    return info;
  }

  public static class AuthInfo {
    public boolean permAdmin = false;
    public boolean permRegister = false;
    public boolean permSendSelfMessage = false;

    public String authKey;
    public String clientName;

    public AuthInfo(String authKey, String clientName) {
      this.authKey = authKey;
      this.clientName = clientName;
    }
  }

}
