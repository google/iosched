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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.common.base.Optional;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.server.image.ServingUrlManager;
import com.google.samples.apps.iosched.server.userdata.Ids;

/**
 * Endpoint for retrieving the serving URL of feed images stored in Google Cloud Storage.
 */
@Api(
    name = "imagefeed",
    title = "Serving URL",
    description = "Serving URL for images in GCS.",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iosched.apps.samples.google.com",
        ownerName = "google.com",
        packagePath = "rpc"
    ),
    clientIds = {Ids.SERVICE_ACCOUNT_CLIENT_ID,
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID}
)
public class ServingUrlEndpoint {

  /**
   * Get the serving URL for the image at the given filepath.
   *
   * @param filepath Filepath of image in GCS.
   * @return Serving URL that can be used to request different sizes of an image.
   */
  @ApiMethod(name = "getServingUrl", path = "imageurl")
  public ServingUrlResult getServingUrl(@Named("filepath") String filepath) {
    GcsFilename gcsFilename = new GcsFilename(Config.CLOUD_STORAGE_BUCKET, filepath);
    // Use ImageService to get serving URL for image.
    String url = ServingUrlManager.INSTANCE
        .createServingUrl(gcsFilename, Optional.<String>absent());
    // Use https for url.
    url = url.replace("http://", "https://");

    return new ServingUrlResult(url);
  }
}