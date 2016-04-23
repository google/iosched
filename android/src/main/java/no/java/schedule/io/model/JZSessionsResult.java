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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class JZSessionsResult {

  public String bodyHtml;
  public JZDate start;
  public JZDate end;

  public String format;
  public String id;
  public JZLabel [] labels;

  public JZLevel level;
  public String room;

  public URI selfUri;
  public URI sessionHtmlUrl;

  public Set<String> speakers;

  public String title;

  public String attending;
  public String timeslot;
  public String speakerItems;

  public String labelstrings() {

    String result="";

    if (labels==null){
      return "";
    }

    for (JZLabel label : labels) {
      result += label.displayName + ",";
    }

    return result;
  }

  public static JZSessionsResult from(final EMSItem pItem) {

    JZSessionsResult session = new JZSessionsResult();

    session.bodyHtml = "<b>"+pItem.getValue("summary")+"</b><p/>" +pItem.getValue("body");
    // Start / End populated late
    //session.start =
    //session.end =
    session.timeslot = pItem.getLinkHref("slot item");
    session.format = pItem.getValue("format");
    session.id = pItem.href.toString();
    session.labels = toJZLabels(pItem.getArray("keywords")); // TODO
    session.level = new JZLevel(pItem.getValue("level"));
    session.room = pItem.getLinkHref("room item");
    session.selfUri = pItem.href;
    //session.sessionHtmlUrl // TODO
    session.speakerItems =  pItem.getLinkHref("speaker item");
    session.title = pItem.getValue("title");

    return session;

  }

  private static JZLabel[] toJZLabels(final String[] pStrings) {

    if (pStrings==null) return new JZLabel[0];

    ArrayList<JZLabel> result = new ArrayList<JZLabel>(pStrings.length);

    for (String string : pStrings) {
      result.add(new JZLabel(string));

    }
    return  result.toArray(new JZLabel[result.size()]);
  }

}
