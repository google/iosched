/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.debug;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.TextUtils;

import com.google.samples.apps.iosched.model.ScheduleItem;

import java.util.ArrayList;

/**
 * Helper methods to generate code, as a string, for creating different type of mock data. This can
 * be used to easily create  mock data objects in integration tests (in mockdata package).
 */
public class OutputMockData {

    private static final String NEWLINE = ";\n";

    private static final String SEP = ",";

    private static final String QUOTE = "\"";

    private static final String LONG = "l";

    /**
     * This generates code, as a String, to create a {@link android.database.MatrixCursor} with a
     * row with the same data as the current row of the {@code cursor}. This can be used to easily
     * create cursor with mock data in integration tests (in mockdata package), by logging the
     * output of this method in {@link com.google.samples.apps.iosched.archframework
     * .ModelWithLoaderManager#onLoadFinished(QueryEnum, Cursor)} and copying the logged string into
     * a method that returns a {@link MatrixCursor} in a class in {@link
     * com.google.samples.apps.iosched.mockdata}.
     */
    public static String generateMatrixCursorCodeForCurrentRow(Cursor cursor) {
        String output = "";
        String[] data = new String[cursor.getColumnCount()];
        String[] columns = new String[cursor.getColumnCount()];
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            columns[i] = cursor.getColumnName(i);
            data[i] = cursor.getString(i);
        }

        output += "String[] data = {";
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                output += SEP;
            }
            output += QUOTE + data[i] + QUOTE;
        }
        output += "}" + NEWLINE;

        output += "String[] columns = {";
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                output += SEP;
            }
            output += QUOTE + columns[i] + QUOTE;
        }
        output += "}" + NEWLINE;

        output += "MatrixCursor matrixCursor = new MatrixCursor(columns)";
        output += NEWLINE;
        output += "matrixCursor.addRow(data)";
        output += NEWLINE;

        return output;
    }

    /**
     * This generates code, as a String, to create an {@link ArrayList} of {@link ScheduleItem}s
     * with the same data as {@code items}. This can be used to easily create schedule items with
     * mock data in integration tests (in mockdata package), by logging the output of this method in
     * {@link com.google.samples.apps.iosched.myschedule.MyScheduleModel
     * .LoadScheduleDataListener#onDataLoaded(ArrayList)}
     * and copying the logged string into a method that returns a {@link ArrayList<ScheduleItem>} in
     * a class in {@link com.google.samples.apps.iosched.mockdata}.
     */
    public static String generateScheduleItemCode(ArrayList<ScheduleItem> items) {
        String output = "";

        output += "ArrayList<ScheduleItem> newItems = new ArrayList<ScheduleItem>()";
        output += NEWLINE;

        for (int i = 0; i < items.size(); i++) {
            ScheduleItem item = items.get(i);
            String newItem = "newItem" + i;
            output += "ScheduleItem " + newItem + " = new ScheduleItem()";
            output += NEWLINE;
            output += newItem + ".type = " + item.type;
            output += NEWLINE;
            output += newItem + ".sessionType = " + item.sessionType;
            output += NEWLINE;
            if (!TextUtils.isEmpty(item.mainTag)) {
                output += newItem + ".mainTag = " + QUOTE + item.mainTag + QUOTE;
                output += NEWLINE;
            }
            output += newItem + ".startTime = " + item.startTime + LONG;
            output += NEWLINE;
            output += newItem + ".endTime = " + item.endTime + LONG;
            output += NEWLINE;
            if (item.numOfSessions != 0) {
                output += newItem + ".numOfSessions = " + item.numOfSessions;
                output += NEWLINE;
            }
            if (!TextUtils.isEmpty(item.sessionId)) {
                output += newItem + ".sessionId = " + QUOTE + item.sessionId + QUOTE;
                output += NEWLINE;
            }
            output += newItem + ".title = " + QUOTE + item.title + QUOTE;
            output += NEWLINE;
            output += newItem + ".subtitle = " + QUOTE + item.subtitle + QUOTE;
            output += NEWLINE;
            output += newItem + ".room = " + QUOTE + item.room + QUOTE;
            output += NEWLINE;
            output += newItem + ".hasGivenFeedback = " + item.hasGivenFeedback;
            output += NEWLINE;
            if (!TextUtils.isEmpty(item.backgroundImageUrl)) {
                output += newItem + ".backgroundImageUrl = " + QUOTE + item.backgroundImageUrl +
                        QUOTE;
                output += NEWLINE;
            }
            if (item.backgroundColor != 0) {
                output += newItem + ".backgroundColor = " + item.backgroundColor;
                output += NEWLINE;
            }
            if (item.flags != 0) {
                output += newItem + ".flags = " + item.flags;
                output += NEWLINE;
            }
            output += "newItems.add(" + newItem + ")";
            output += NEWLINE;
        }

        output += "return newItems";
        output += NEWLINE;

        return output;
    }
}
