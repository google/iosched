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
package com.google.iosched.input.fetcher;

/**
 * The simplest implementation of "dependency injection" ever.
 *
 * Isolates the decision of which EntityFetcher to use for entities saved as remote JSON files.
 * Each environment (AppEngine, tests or command line) should set the appropriate fetcher in
 * the factory before first usage. No default is set on purpose, to raise errors earlier.
 */
public class RemoteFilesEntityFetcherFactory {
  private static FetcherBuilder builder;

  public static FetcherBuilder getBuilder() {
    return builder;
  }

  public static void setBuilder(FetcherBuilder builder) {
    RemoteFilesEntityFetcherFactory.builder = builder;
  }

  public static interface FetcherBuilder {
    FetcherBuilder setSourceFiles(String... filenames);
    EntityFetcher build();
  }
}

