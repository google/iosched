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

package com.google.samples.apps.iosched.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class TimeUtils {
    public static final int SECOND = 1000;
    public static final int MINUTE = 60 * SECOND;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;

    private static final String TAG = makeLogTag(TimeUtils.class);

    private static final SimpleDateFormat[] ACCEPTED_TIMESTAMP_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z", Locale.US)
    };

    private static final SimpleDateFormat VALID_IFMODIFIEDSINCE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    public static Date parseTimestamp(String timestamp) {
        for (SimpleDateFormat format : ACCEPTED_TIMESTAMP_FORMATS) {
            // TODO: We shouldn't be forcing the time zone when parsing dates.
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return format.parse(timestamp);
            } catch (ParseException ex) {
                continue;
            }
        }

        // All attempts to parse have failed
        return null;
    }

    public static boolean isValidFormatForIfModifiedSinceHeader(String timestamp) {
        try {
            return VALID_IFMODIFIEDSINCE_FORMAT.parse(timestamp) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static long timestampToMillis(String timestamp, long defaultValue) {
        if (TextUtils.isEmpty(timestamp)) {
            return defaultValue;
        }
        Date d = parseTimestamp(timestamp);
        return d == null ? defaultValue : d.getTime();
    }

    /**
     * Format a {@code date} honoring the app preference for using Conference or device timezone.
     * {@code Context} is used to lookup the shared preference settings.
     */
    public static String formatShortDate(Context context, Date date) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        return DateUtils.formatDateRange(context, formatter, date.getTime(), date.getTime(),
                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_NO_YEAR,
                SettingsUtils.getDisplayTimeZone(context).getID()).toString();
    }

    public static String formatShortTime(Context context, Date time) {
        // Android DateFormatter will honor the user's current settings.
        DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
        // Override with Timezone based on settings since users can override their phone's timezone
        // with Pacific time zones.
        TimeZone tz = SettingsUtils.getDisplayTimeZone(context);
        if (tz != null) {
            format.setTimeZone(tz);
        }
        return format.format(time);
    }

    public static String formatShortDateTime(Context context, Date date) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        return DateUtils.formatDateRange(context, formatter, date.getTime(), date.getTime(),
                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_WEEKDAY
                        | DateUtils.FORMAT_SHOW_TIME,
                SettingsUtils.getDisplayTimeZone(context).getID()).toString().toUpperCase();
    }

    public static boolean hasConferenceEnded(final Context context) {
        long now = getCurrentTime(context);
        return now > Config.CONFERENCE_END_MILLIS;
    }

    public static boolean isConferenceInProgress(final Context context) {
        long now = getCurrentTime(context);
        return now >= Config.CONFERENCE_START_MILLIS && now <= Config.CONFERENCE_END_MILLIS;
    }

    /**
     * Returns "Today", "Tomorrow", "Yesterday", or a short date format.
     */
    public static String formatHumanFriendlyShortDate(final Context context, long timestamp) {
        long localTimestamp, localTime;
        long now = getCurrentTime(context);

        TimeZone tz = SettingsUtils.getDisplayTimeZone(context);
        localTimestamp = timestamp + tz.getOffset(timestamp);
        localTime = now + tz.getOffset(now);

        long dayOrd = localTimestamp / 86400000L;
        long nowOrd = localTime / 86400000L;

        if (dayOrd == nowOrd) {
            return context.getString(R.string.day_title_today);
        } else if (dayOrd == nowOrd - 1) {
            return context.getString(R.string.day_title_yesterday);
        } else if (dayOrd == nowOrd + 1) {
            return context.getString(R.string.day_title_tomorrow);
        } else {
            return formatShortDate(context, new Date(timestamp));
        }
    }

    /**
     * @return the name of the day at the given {@code position} in the {@link
     * Config#CONFERENCE_DAYS}. It is assumed that all days in {@link Config#CONFERENCE_DAYS} are
     * consecutive.
     */
    public static String getDayName(Context context, int position) {
        long day1Start = Config.CONFERENCE_DAYS[0][0];
        long day = 1000 * 60 * 60 * 24;
        return TimeUtils.formatShortDate(context, new Date(day1Start + day * position));
    }


    /**
     * Retrieve the current time. If the current build is a debug build, the mock time is returned
     * when set, taking into account the passage of time by adding the difference between the
     * current system time and the system time when the application was created.
     */
    public static long getCurrentTime(final Context context) {
        if (BuildConfig.DEBUG) {
            return context.getSharedPreferences(UIUtils.MOCK_DATA_PREFERENCES, Context.MODE_PRIVATE)
                          .getLong(UIUtils.PREFS_MOCK_CURRENT_TIME, System.currentTimeMillis())
                    + System.currentTimeMillis() - getAppStartTime(context);
        } else {
            return System.currentTimeMillis();
        }
    }

    /**
     * Set the current time only when the current build is a debug build.
     */
    private static void setCurrentTime(Context context, long newTime) {
        if (BuildConfig.DEBUG) {
            java.util.Date currentTime = new java.util.Date(TimeUtils.getCurrentTime(context));
            LOGW(TAG, "Setting time from " + currentTime + " to " + newTime);
            context.getSharedPreferences(UIUtils.MOCK_DATA_PREFERENCES, Context.MODE_PRIVATE).edit()
                   .putLong(UIUtils.PREFS_MOCK_CURRENT_TIME, newTime).apply();
        }
    }

    /**
     * Retrieve the app start time,set when the application was created. This is used to calculate
     * the current time, in debug mode only.
     */
    private static long getAppStartTime(final Context context) {
        return context.getSharedPreferences(UIUtils.MOCK_DATA_PREFERENCES, Context.MODE_PRIVATE)
                      .getLong(UIUtils.PREFS_MOCK_APP_START_TIME, System.currentTimeMillis());
    }

    /**
     * Set the app start time only when the current build is a debug build.
     */
    public static void setAppStartTime(Context context, long newTime) {
        if (BuildConfig.DEBUG) {
            java.util.Date previousAppStartTime = new java.util.Date(TimeUtils.getAppStartTime(
                    context));
            LOGW(TAG, "Setting app startTime from " + previousAppStartTime + " to " + newTime);
            context.getSharedPreferences(UIUtils.MOCK_DATA_PREFERENCES, Context.MODE_PRIVATE).edit()
                   .putLong(UIUtils.PREFS_MOCK_APP_START_TIME, newTime).apply();
        }
    }

    /**
     * Sets the current time to a time relative to the start of the conference. If {@code
     * timeDifference} is positive, it is set to {@code timeDifference} ms after the start of the
     * conference, if it is negative, it is set to {@code timeDifference} ms before the start of the
     * conference. This should only be called from code in debug package or in tests.
     */
    public static void setCurrentTimeRelativeToStartOfConference(Context context,
            long timeDifference) {
        java.util.Date newTime =
                new java.util.Date(Config.CONFERENCE_START_MILLIS + timeDifference);
        TimeUtils.setCurrentTime(context, newTime.getTime());
    }

    /**
     * Sets the current time to a time relative to the start of the second day of the conference. If
     * {@code timeDifference} is positive, it is set to {@code timeDifference} ms after the start of
     * the second day of the conference, if it is negative, it is set to {@code timeDifference} ms
     * before the start of the second day of the conference. This should only be called from code in
     * debug package or in tests.
     */
    public static void setCurrentTimeRelativeToStartOfSecondDayOfConference(Context context,
            long timeDifference) {
        java.util.Date newTime = new java.util.Date(Config.CONFERENCE_DAYS[1][0] + timeDifference);
        TimeUtils.setCurrentTime(context, newTime.getTime());
    }

    /**
     * Sets the current time to a time relative to the end of the conference. If {@code
     * timeDifference} is positive, it is set to {@code timeDifference} ms after the end of the
     * conference, if it is negative, it is set to {@code timeDifference} ms before the end of the
     * conference. This should only be called from code in debug package or in tests.
     */
    public static void setCurrentTimeRelativeToEndOfConference(Context context,
            long timeDifference) {
        java.util.Date newTime = new java.util.Date(Config.CONFERENCE_END_MILLIS + timeDifference);
        TimeUtils.setCurrentTime(context, newTime.getTime());
    }
}
