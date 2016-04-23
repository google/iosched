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

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class JZDate {

  public int day;
  public int year;
  public int hour;
  public int month;
  public int minute;
  public int second;
  private GregorianCalendar calendar = new GregorianCalendar();

  public JZDate(final String dateString)  {

    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    Date  date;
    try {
      date = fmt.parse(dateString);
      day = date.getDate();
      year = 1900+date.getYear();
      hour = date.getHours()+2; // TODO timezone hack...
      month  = date.getMonth();
      minute = date.getMinutes();
      second = date.getSeconds();


    }
    catch (ParseException e) {
      Log.e(this.getClass().getName(),e.getMessage(),e);
    }


  }

  public long millis(){

    calendar.set(year,month,day,hour,minute,second);

    return calendar.getTimeInMillis();
  }

}
