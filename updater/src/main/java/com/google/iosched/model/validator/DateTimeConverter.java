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
package com.google.iosched.model.validator;

import com.google.gson.JsonPrimitive;
import com.google.iosched.Config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class DateTimeConverter extends Converter {
  SimpleDateFormat[] inputFormats = new SimpleDateFormat[] {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
  };
  SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  public DateTimeConverter() {
  }
  @SuppressWarnings("deprecation")
  @Override
  public JsonPrimitive convert(JsonPrimitive value) {
    if (value == null) {
      return null;
    }
    Date date = null;
    Exception lastEx = null;
    for (int i=0; i<inputFormats.length && date==null; i++) {
      try {
        date = inputFormats[i].parse(value.getAsString());
      } catch (NumberFormatException e) {
        lastEx = e;
      } catch (ParseException e) {
        lastEx = e;
      }
    }

    if (date == null) {
      throw new ConverterException(value, this, lastEx.getMessage());
    }

    if (date.getYear() < 100) {
      // hack to fix invalid dates on temporary data
      date.setYear(114);
      date.setMonth(Calendar.JUNE);
      date.setDate(25);
    }
    if (Config.TIME_TRAVEL_SHIFT != 0) {
      date=new Date(date.getTime() + Config.TIME_TRAVEL_SHIFT);
    }
    return new JsonPrimitive(outputFormat.format(date));
  }
}
