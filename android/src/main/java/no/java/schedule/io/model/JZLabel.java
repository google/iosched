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

package no.java.schedule.io.model;

import java.net.URL;

public class JZLabel {


  public String displayName;
  public URL iconUrl;
  public String id;

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    JZLabel jzLabel = (JZLabel) o;

    if (!id.equals(jzLabel.id)) { return false; }

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public JZLabel(final String pString) {
    displayName=pString;
    id=pString;


  }
}
