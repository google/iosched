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
package com.google.samples.apps.iosched.server.schedule.server;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSource;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSources;
import com.google.samples.apps.iosched.server.schedule.server.cloudstorage.CloudFileManager;
import com.google.samples.apps.iosched.server.schedule.server.image.ImageUpdater;
import com.google.samples.apps.iosched.server.schedule.server.input.VendorDynamicInput;
import com.google.samples.apps.iosched.server.userdata.Ids;
import java.io.IOException;

/**
 * Endpoint to retrieve data from CMS vendor.
 */
@Api(
    name = "cms",
    title = "IOSched CMS",
    description = "Interface to CMS",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iosched.apps.samples.google.com",
        ownerName = "google.com",
        packagePath = "rpc"
    ),
    clientIds = {Ids.ADMIN_WEB_CLIENT_ID,
                 com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID}
)
public class CmsUpdateEndpoint {

  /**
   * Retrieve session data from CMS and make it ready for processing.
   *
   * @param user User making the request (injected by Endpoints)
   * @throws UnauthorizedException
   * @throws IOException
   */
  @ApiMethod(name = "getDataFromCms", path = "topics")
  public void getDataFromCms(User user) throws UnauthorizedException, IOException {
    if (user == null || !isAllowed(user)) {
      throw new UnauthorizedException("Invalid credentials");
    }

    // everything ok, let's update
    StringBuilder summary = new StringBuilder();
    JsonObject contents = new JsonObject();
    JsonDataSources sources = new VendorDynamicInput().fetchAllDataSources();
    for (String entity: sources) {
      JsonArray array = new JsonArray();
      JsonDataSource source = sources.getSource(entity);
      for (JsonObject obj: source) {
        array.add(obj);
      }
      summary.append(entity).append(": ").append(source.size()).append("\n");
      contents.add(entity, array);
    }

    // Fetch new images and set up serving URLs.
    new ImageUpdater().run(sources);

    // Write file to cloud storage
    CloudFileManager fileManager = new CloudFileManager();
    fileManager.createOrUpdate("__raw_session_data.json", contents, true);
  }

  /**
   * Process session data from the CMS and make it available to clients.
   *
   * @param user User making request (injected by Endpoints)
   * @throws UnauthorizedException
   * @throws IOException
   */
  @ApiMethod(name = "useDataFromCms", path = "update")
  public void updateIODataWithDataFromCms(User user) throws UnauthorizedException, IOException {
    if (user == null || !isAllowed(user)) {
      throw new UnauthorizedException("Invalid credentials");
    }

    new APIUpdater().run(true, false, null);
  }

  private boolean isAllowed(User user) {
    for (String email: Config.ALLOWED_CMS_UPDATERS) {
      if (email.equals(user.getEmail())) {
        return true;
      }
    }
    return false;
  }

}
