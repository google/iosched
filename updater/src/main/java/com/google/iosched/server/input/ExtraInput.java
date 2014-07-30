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
package com.google.iosched.server.input;

import com.google.iosched.input.fetcher.RemoteFilesEntityFetcherFactory;
import com.google.iosched.model.InputJsonKeys.ExtraSource.MainTypes;

/**
 * Encapsulation for all the input extracted from CloudStorage static files.
 */
public class ExtraInput extends DataSourceInput<MainTypes> {

  public static final String TAGSCONF_FILE = "tagsconf.json";

  public ExtraInput() {
    super(RemoteFilesEntityFetcherFactory.getBuilder().setSourceFiles(TAGSCONF_FILE).build());
  }

  @Override
  public Class<MainTypes> getType() {
    return MainTypes.class;
  }

}
