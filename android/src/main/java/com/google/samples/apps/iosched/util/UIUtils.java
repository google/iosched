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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.samples.apps.iosched.ui.phone.MapActivity;
import com.google.samples.apps.iosched.ui.tablet.MapMultiPaneActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An assortment of UI helpers.
 */
public class UIUtils {
    private static final String TAG = makeLogTag(UIUtils.class);

    /**
     * Factor applied to session color to derive the background color on panels and when
     * a session photo could not be downloaded (or while it is being downloaded)
     */
    public static final float SESSION_BG_COLOR_SCALE_FACTOR = 0.75f;

    private static final float SESSION_PHOTO_SCRIM_ALPHA = 0.25f; // 0=invisible, 1=visible image
    private static final float SESSION_PHOTO_SCRIM_SATURATION = 0.2f; // 0=gray, 1=color image

    public static final String TARGET_FORM_FACTOR_ACTIVITY_METADATA =
            "com.google.samples.apps.iosched.meta.TARGET_FORM_FACTOR";

    public static final String TARGET_FORM_FACTOR_HANDSET = "handset";
    public static final String TARGET_FORM_FACTOR_TABLET = "tablet";

    /**
     * Flags used with {@link DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    /**
     * Regex to search for HTML escape sequences.
     *
     * <p></p>Searches for any continuous string of characters starting with an ampersand and ending with a
     * semicolon. (Example: &amp;amp;)
     */
    private static final Pattern REGEX_HTML_ESCAPE = Pattern.compile(".*&\\S;.*");

    private static CharSequence sNowPlayingText;
    private static CharSequence sLivestreamNowText;
    private static CharSequence sLivestreamAvailableText;

    public static final String GOOGLE_PLUS_PACKAGE_NAME = "com.google.android.apps.plus";
    public static final String YOUTUBE_PACKAGE_NAME = "com.google.android.youtube";

    public static final int ANIMATION_FADE_IN_TIME = 250;
    public static final String TRACK_ICONS_TAG = "tracks";

    private static SimpleDateFormat sDayOfWeekFormat = new SimpleDateFormat("E");
    private static DateFormat sShortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    public static String formatSessionSubtitle(long intervalStart, long intervalEnd, String roomName, StringBuilder recycle,
            Context context) {
        return formatSessionSubtitle(intervalStart, intervalEnd, roomName, recycle, context, false);
    }

    /**
     * Format and return the given session time and {@link Rooms} values using
     * {@link Config#CONFERENCE_TIMEZONE}.
     */
    public static String formatSessionSubtitle(long intervalStart, long intervalEnd, String roomName, StringBuilder recycle,
            Context context, boolean shortFormat) {

        // Determine if the session is in the past
        long currentTimeMillis = UIUtils.getCurrentTime(context);
        boolean conferenceEnded = currentTimeMillis > Config.CONFERENCE_END_MILLIS;
        boolean sessionEnded = currentTimeMillis > intervalEnd;
        if (sessionEnded && !conferenceEnded) {
            return context.getString(R.string.session_finished);
        }

        if (roomName == null) {
            roomName = context.getString(R.string.unknown_room);
        }

        if (shortFormat) {
            Date intervalStartDate = new Date(intervalStart);
            sDayOfWeekFormat.setTimeZone(PrefUtils.getDisplayTimeZone(context));
            sShortTimeFormat.setTimeZone(PrefUtils.getDisplayTimeZone(context));
            return sDayOfWeekFormat.format(intervalStartDate) + " "
                    + sShortTimeFormat.format(intervalStartDate);
        } else {
            return context.getString(R.string.session_subtitle,
                    formatIntervalTimeString(intervalStart, intervalEnd, recycle, context), roomName);
        }
    }

    /**
     * Format and return the given session speakers and {@link Rooms} values.
     */
    public static String formatSessionSubtitle(String roomName, String speakerNames,
            Context context) {

        // Determine if the session is in the past
        if (roomName == null) {
            roomName = context.getString(R.string.unknown_room);
        }

        if (!TextUtils.isEmpty(speakerNames)) {
            return speakerNames + "\n" + roomName;
        } else {
            return roomName;
        }
    }

    /**
     * Format and return the given time interval using {@link Config#CONFERENCE_TIMEZONE}
     * (unless local time was explicitly requested by the user).
     */
    public static String formatIntervalTimeString(long intervalStart, long intervalEnd,
            StringBuilder recycle, Context context) {
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        Formatter formatter = new Formatter(recycle);
        return DateUtils.formatDateRange(context, formatter, intervalStart, intervalEnd, TIME_FLAGS,
                PrefUtils.getDisplayTimeZone(context).getID()).toString();
    }

    public static boolean isSameDayDisplay(long time1, long time2, Context context) {
        TimeZone displayTimeZone = PrefUtils.getDisplayTimeZone(context);
        Calendar cal1 = Calendar.getInstance(displayTimeZone);
        Calendar cal2 = Calendar.getInstance(displayTimeZone);
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Populate the given {@link TextView} with the requested text, formatting
     * through {@link Html#fromHtml(String)} when applicable. Also sets
     * {@link TextView#setMovementMethod} so inline links are handled.
     */
    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if ((text.contains("<") && text.contains(">")) || REGEX_HTML_ESCAPE.matcher(text).find()) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    public static String getLiveBadgeText(final Context context, long start, long end) {
        long now = getCurrentTime(context);

        if (now < start) {
            // Will be live later
            return context.getString(R.string.live_available);
        } else if (start <= now && now <= end) {
            // Live right now!
            // Indicated by a visual live now badge
            return "";
        } else {
            // Too late.
            return "";
        }
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);

        // Walk through string, inserting bold snippet spans
        int startIndex, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);

            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);

            // Insert bold style
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //builder.setSpan(new ForegroundColorSpan(0xff111111),
            //        startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            delta += 2;
        }

        return builder;
    }

    public static void preferPackageForIntent(Context context, Intent intent, String packageName) {
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo resolveInfo : pm.queryIntentActivities(intent, 0)) {
            if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                intent.setPackage(packageName);
                break;
            }
        }
    }

    public static String getSessionHashtagsString(String hashtags) {
        if (!TextUtils.isEmpty(hashtags)) {
            if (!hashtags.startsWith("#")) {
                hashtags = "#" + hashtags;
            }

            if (hashtags.contains(Config.CONFERENCE_HASHTAG_PREFIX)) {
                return hashtags;
            }

            return Config.CONFERENCE_HASHTAG + " " + hashtags;
        } else {
            return Config.CONFERENCE_HASHTAG;
        }
    }

    private static final int BRIGHTNESS_THRESHOLD = 130;

    /**
     * Calculate whether a color is light or dark, based on a commonly known
     * brightness formula.
     *
     * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
     */
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    // Whether a feedback notification was fired for a particular session. In the event that a
    // feedback notification has not been fired yet, return false and set the bit.
    public static boolean isFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    // Clear the flag that says a notification was fired for the given session.
    // Typically used to debug notifications.
    public static void unmarkFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        sp.edit().putBoolean(key, false).commit();
    }
    // Shows whether a notification was fired for a particular session time block. In the
    // event that notification has not been fired yet, return false and set the bit.
    public static boolean isNotificationFiredForBlock(Context context, String blockId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("notification_fired_%s", blockId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    private static final long sAppLoadTime = System.currentTimeMillis();

    public static long getCurrentTime(final Context context) {
        if (BuildConfig.DEBUG) {
            return context.getSharedPreferences("mock_data", Context.MODE_PRIVATE)
                    .getLong("mock_current_time", System.currentTimeMillis())
                    + System.currentTimeMillis() - sAppLoadTime;
//            return ParserUtils.parseTime("2012-06-27T09:44:45.000-07:00")
//                    + System.currentTimeMillis() - sAppLoadTime;
        } else {
            return System.currentTimeMillis();
        }
    }

    public static boolean shouldShowLiveSessionsOnly(final Context context) {
        return !PrefUtils.isAttendeeAtVenue(context)
                && getCurrentTime(context) < Config.CONFERENCE_END_MILLIS;
    }

    /**
     * Enables and disables {@linkplain android.app.Activity activities} based on their
     * {@link #TARGET_FORM_FACTOR_ACTIVITY_METADATA}" meta-data and the current device.
     * Values should be either "handset", "tablet", or not present (meaning universal).
     * <p>
     * <a href="http://stackoverflow.com/questions/13202805">Original code</a> by Dandre Allison.
     * @param context the current context of the device
     * @see #isHoneycombTablet(android.content.Context)
     */
    public static void enableDisableActivitiesByFormFactor(Context context) {
        final PackageManager pm = context.getPackageManager();
        boolean isTablet = isTablet(context);

        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
            if (pi == null) {
                LOGE(TAG, "No package info found for our own package.");
                return;
            }

            final ActivityInfo[] activityInfos = pi.activities;
            for (ActivityInfo info : activityInfos) {
                String targetDevice = null;
                if (info.metaData != null) {
                    targetDevice = info.metaData.getString(TARGET_FORM_FACTOR_ACTIVITY_METADATA);
                }
                boolean tabletActivity = TARGET_FORM_FACTOR_TABLET.equals(targetDevice);
                boolean handsetActivity = TARGET_FORM_FACTOR_HANDSET.equals(targetDevice);

                boolean enable = !(handsetActivity && isTablet)
                        && !(tabletActivity && !isTablet);

                String className = info.name;
                pm.setComponentEnabledSetting(
                        new ComponentName(context, Class.forName(className)),
                        enable
                                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        } catch (PackageManager.NameNotFoundException e) {
            LOGE(TAG, "No package info found for our own package.", e);
        } catch (ClassNotFoundException e) {
            LOGE(TAG, "Activity not found within package.", e);
        }
    }

    public static Class getMapActivityClass(Context context) {
        if (UIUtils.isTablet(context)) {
            return MapMultiPaneActivity.class;
        }

        return MapActivity.class;
    }

    /**
     * If an activity's intent is for a Google I/O web URL that the app can handle
     * natively, this method translates the intent to the equivalent native intent.
     */
    public static void tryTranslateHttpIntent(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        if (uri == null || TextUtils.isEmpty(uri.getPath())) {
            return;
        }

        String prefixPath = Config.SESSION_DETAIL_WEB_URL_PREFIX.getPath();
        String path = uri.getPath();

        if (Config.SESSION_DETAIL_WEB_URL_PREFIX.getScheme().equals(uri.getScheme()) &&
                Config.SESSION_DETAIL_WEB_URL_PREFIX.getHost().equals(uri.getHost()) &&
                path.startsWith(prefixPath)) {
            String sessionId = path.substring(prefixPath.length());
            activity.setIntent(new Intent(
                    Intent.ACTION_VIEW,
                    ScheduleContract.Sessions.buildSessionUri(sessionId)));
        }
    }

    private static final int[] RES_IDS_ACTION_BAR_SIZE = { R.attr.actionBarSize };

    /** Calculates the Action Bar height in pixels. */
    public static int calculateActionBarSize(Context context) {
        if (context == null) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }

        float size = att.getDimension(0, 0);
        att.recycle();
        return (int) size;
    }

    public static int setColorAlpha(int color, float alpha) {
        int alpha_int = Math.min(Math.max((int)(alpha * 255.0f), 0), 255);
        return Color.argb(alpha_int, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int scaleColor(int color, float factor, boolean scaleAlpha) {
        return Color.argb(scaleAlpha ? (Math.round(Color.alpha(color) * factor)) : Color.alpha(color),
                Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    public static int scaleSessionColorToDefaultBG(int color) {
        return scaleColor(color, SESSION_BG_COLOR_SCALE_FACTOR, false);
    }

    public static void showHashtagStream(final Context context, String hashTag) {
        final String hashTagsString = getSessionHashtagsString(hashTag);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://plus.google.com/s/"
                        + hashTagsString.replaceAll(" ", "%20").replaceAll("#", "%23")));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        preferPackageForIntent(context, intent, UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
        context.startActivity(intent);
    }

    public static void setStartPadding(final Context context, View view, int padding) {
        if (isRtl(context)) {
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), padding, view.getPaddingBottom());
        } else {
            view.setPadding(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else {
            return context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        }
    }

    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public static void setUpButterBar(View butterBar, String messageText, String actionText,
            View.OnClickListener listener) {
        if (butterBar == null) {
            LOGE(TAG, "Failed to set up butter bar: it's null.");
            return;
        }

        TextView textView = (TextView) butterBar.findViewById(R.id.butter_bar_text);
        if (textView != null) {
            textView.setText(messageText);
        }

        Button button = (Button) butterBar.findViewById(R.id.butter_bar_button);
        if (button != null) {
            button.setText(actionText == null ? "" : actionText);
            button.setVisibility(!TextUtils.isEmpty(actionText) ? View.VISIBLE : View.GONE);
        }

        button.setOnClickListener(listener);
        butterBar.setVisibility(View.VISIBLE);
    }

    public static float getProgress(int value, int min, int max) {
        if (min == max) {
            throw new IllegalArgumentException("Max (" + max + ") cannot equal min (" + min + ")");
        }

        return (value - min) / (float) (max - min);
    }

    // Desaturates and color-scrims the image
    public static ColorFilter makeSessionImageScrimColorFilter(int sessionColor) {
        float a = SESSION_PHOTO_SCRIM_ALPHA;
        float sat = SESSION_PHOTO_SCRIM_SATURATION; // saturation (0=gray, 1=color)
        return new ColorMatrixColorFilter(new float[]{
                ((1 - 0.213f) * sat + 0.213f) * a, ((0 - 0.715f) * sat + 0.715f) * a, ((0 - 0.072f) * sat + 0.072f) * a, 0, Color.red(sessionColor) * (1 - a),
                ((0 - 0.213f) * sat + 0.213f) * a, ((1 - 0.715f) * sat + 0.715f) * a, ((0 - 0.072f) * sat + 0.072f) * a, 0, Color.green(sessionColor) * (1 - a),
                ((0 - 0.213f) * sat + 0.213f) * a, ((0 - 0.715f) * sat + 0.715f) * a, ((1 - 0.072f) * sat + 0.072f) * a, 0, Color.blue(sessionColor) * (1 - a),
                0, 0, 0, 0, 255
        });
    }
}
