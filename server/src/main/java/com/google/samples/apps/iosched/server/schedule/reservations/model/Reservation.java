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
package com.google.samples.apps.iosched.server.schedule.reservations.model;

import java.util.Map;

/**
 * Representation of a Reservation object in RTDB.
 */
public class Reservation implements Comparable<Reservation> {

  public long last_status_changed;
  public String status;
  public Map<String, String> results;

  @Override
  public int compareTo(Reservation reservation) {
    if (last_status_changed < reservation.last_status_changed) {
      return -1;
    } else if (last_status_changed > reservation.last_status_changed) {
      return 1;
    }
    return 0;
  }
}
