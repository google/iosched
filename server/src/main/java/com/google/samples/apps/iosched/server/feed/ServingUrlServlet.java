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
package com.google.samples.apps.iosched.server.feed;

import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.gson.JsonObject;
import com.google.samples.apps.iosched.server.schedule.Config;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.protocol.HTTP;

/**
 * ServingUrlServlet is used to provide serving URLs for feed images. Feed images
 * are added to Google Cloud Storage and are retrieved by a serving URL generated
 * here.
 *
 * Requests must include an API Key set as the Authorization header, as well as
 * specify the path to the image within the bucket.
 */
public class ServingUrlServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    PrintWriter out = resp.getWriter();

    // Check that request is authorized.
    String utilKey = req.getHeader("Authorization");
    if (utilKey == null || !utilKey.equals(Config.FEED_API_KEY)) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      resp.setContentType("plain/text");
      out.println("Bad Request, missing or incorrect credentials.");
      return;
    }

    // Get file path from request.
    String filepath = req.getParameter("filepath");

    // Prepare image service to retrieve serving URL for image.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions servingUrlOptions = ServingUrlOptions.Builder
        .withGoogleStorageFileName("/gs/" +
            Config.CLOUD_STORAGE_BUCKET + "/" + filepath);

    resp.setContentType("application/json");
    JsonObject jsonObject = new JsonObject();
    try {
      // Use ImageService to get serving URL for image.
      String url = imagesService.getServingUrl(servingUrlOptions);
      // Use https for url.
      url = url.replace("http://", "https://");
      jsonObject.addProperty("servingUrl", url);
    } catch (IllegalArgumentException ex) {
      jsonObject = new JsonObject();
      jsonObject.addProperty("error", ex.getMessage());
    } finally {
      out.print(jsonObject.toString());
    }
  }

}
