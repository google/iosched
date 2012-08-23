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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.BuildConfig;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.ui.phone.MapActivity;
import com.google.android.apps.iosched.ui.phone.SessionDetailActivity;
import com.google.android.apps.iosched.ui.phone.SessionsActivity;
import com.google.android.apps.iosched.ui.phone.TrackDetailActivity;
import com.google.android.apps.iosched.ui.phone.VendorDetailActivity;
import com.google.android.apps.iosched.ui.tablet.MapMultiPaneActivity;
import com.google.android.apps.iosched.ui.tablet.SessionsVendorsMultiPaneActivity;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An assortment of UI helpers.
 */
public class UIUtils {
    /**
     * Time zone to use when formatting all session times. To always use the
     * phone local time, use {@link TimeZone#getDefault()}.
     */
    public static final TimeZone CONFERENCE_TIME_ZONE = TimeZone.getTimeZone("America/Los_Angeles");

    public static final long CONFERENCE_START_MILLIS = ParserUtils.parseTime(
            "2012-06-27T09:30:00.000-07:00");
    public static final long CONFERENCE_END_MILLIS = ParserUtils.parseTime(
            "2012-06-29T18:00:00.000-07:00");

    public static final String CONFERENCE_HASHTAG = "#io12";

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    /** Flags used with {@link DateUtils#formatDateRange}. */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    /** {@link StringBuilder} used for formatting time block. */
    private static StringBuilder sBuilder = new StringBuilder(50);
    /** {@link Formatter} used for formatting time block. */
    private static Formatter sFormatter = new Formatter(sBuilder, Locale.getDefault());

    private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

    private static CharSequence sEmptyRoomText;
    private static CharSequence sCodeLabRoomText;

    private static CharSequence sNowPlayingText;
    private static CharSequence sLivestreamNowText;
    private static CharSequence sLivestreamAvailableText;

    public static final String GOOGLE_PLUS_PACKAGE_NAME = "com.google.android.apps.plus";

    /**
     * Format and return the given {@link Blocks} and {@link Rooms} values using
     * {@link #CONFERENCE_TIME_ZONE}.
     */
    public static String formatSessionSubtitle(String sessionTitle,
            long blockStart, long blockEnd, String roomName, Context context) {
        if (sEmptyRoomText == null || sCodeLabRoomText == null) {
            sEmptyRoomText = context.getText(R.string.unknown_room);
            sCodeLabRoomText = context.getText(R.string.codelab_room);
        }

        if (roomName == null) {
            // TODO: remove the WAR for API not returning rooms for code labs
            return context.getString(R.string.session_subtitle,
                    formatBlockTimeString(blockStart, blockEnd, context),
                    sessionTitle.contains("Code Lab")
                            ? sCodeLabRoomText
                            : sEmptyRoomText);
        }

        return context.getString(R.string.session_subtitle,
                formatBlockTimeString(blockStart, blockEnd, context), roomName);
    }

    /**
     * Format and return the given {@link Blocks} values using
     * {@link #CONFERENCE_TIME_ZONE}.
     */
    public static String formatBlockTimeString(long blockStart, long blockEnd, Context context) {
        TimeZone.setDefault(CONFERENCE_TIME_ZONE);

        // NOTE: There is an efficient version of formatDateRange in Eclair and
        // beyond that allows you to recycle a StringBuilder.
        return DateUtils.formatDateRange(context, blockStart, blockEnd, TIME_FLAGS);
    }

    public static boolean isSameDay(long time1, long time2) {
        TimeZone.setDefault(CONFERENCE_TIME_ZONE);

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static String getTimeAgo(long time, Context ctx) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = getCurrentTime(ctx);
        if (time > now || time <= 0) {
            return null;
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
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
        if (text.contains("<") && text.contains(">")) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    public static void updateTimeAndLivestreamBlockUI(final Context context,
            long blockStart, long blockEnd, boolean hasLivestream,
            View backgroundView, TextView titleView, TextView subtitleView,
            CharSequence subtitle) {
        long currentTimeMillis = getCurrentTime(context);

        boolean past = (currentTimeMillis > blockEnd && currentTimeMillis < CONFERENCE_END_MILLIS);
        boolean present = (blockStart <= currentTimeMillis && currentTimeMillis <= blockEnd);

        final Resources res = context.getResources();
        if (backgroundView != null) {
            backgroundView.setBackgroundColor(past
                    ? res.getColor(R.color.past_background_color)
                    : 0);
        }

        if (titleView != null) {
            titleView.setTypeface(Typeface.SANS_SERIF, past ? Typeface.NORMAL : Typeface.BOLD);
        }

        if (subtitleView != null) {
            boolean empty = true;
            SpannableStringBuilder sb = new SpannableStringBuilder(); // TODO: recycle
            if (subtitle != null) {
                sb.append(subtitle);
                empty = false;
            }

            if (present) {
                if (sNowPlayingText == null) {
                    sNowPlayingText = Html.fromHtml(context.getString(R.string.now_playing_badge));
                }
                if (!empty) {
                    sb.append("  ");
                }
                sb.append(sNowPlayingText);

                if (hasLivestream) {
                    if (sLivestreamNowText == null) {
                        sLivestreamNowText = Html.fromHtml("&nbsp;&nbsp;" +
                                context.getString(R.string.live_now_badge));
                    }
                    sb.append(sLivestreamNowText);
                }
            } else if (hasLivestream) {
                if (sLivestreamAvailableText == null) {
                    sLivestreamAvailableText = Html.fromHtml(
                            context.getString(R.string.live_available_badge));
                }
                if (!empty) {
                    sb.append("  ");
                }
                sb.append(sLivestreamAvailableText);
            }

            subtitleView.setText(sb);
        }
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);

        // Walk through string, inserting bold snippet spans
        int startIndex = -1, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);

            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);

            // Insert bold style
            builder.setSpan(sBoldSpan, startIndex - delta, endIndex - delta - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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

    public static ImageFetcher getImageFetcher(final FragmentActivity activity) {
        // The ImageFetcher takes care of loading remote images into our ImageView
        ImageFetcher fetcher = new ImageFetcher(activity);
        fetcher.addImageCache(activity);
        return fetcher;
    }

    public static String getSessionHashtagsString(String hashtags) {
        if (!TextUtils.isEmpty(hashtags)) {
            if (!hashtags.startsWith("#")) {
                hashtags = "#" + hashtags;
            }

            if (hashtags.contains(CONFERENCE_HASHTAG)) {
                return hashtags;
            }
            return CONFERENCE_HASHTAG + " " + hashtags;
        } else {
            return CONFERENCE_HASHTAG;
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
        } else {
            return System.currentTimeMillis();
        }
    }

    public static void safeOpenLink(Context context, Intent linkIntent) {
        try {
            context.startActivity(linkIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT) .show();
        }
    }

    // TODO: use <meta-data> element instead
    private static final Class[] sPhoneActivities = new Class[]{
            MapActivity.class,
            SessionDetailActivity.class,
            SessionsActivity.class,
            TrackDetailActivity.class,
            VendorDetailActivity.class,
    };

    // TODO: use <meta-data> element instead
    private static final Class[] sTabletActivities = new Class[]{
            MapMultiPaneActivity.class,
            SessionsVendorsMultiPaneActivity.class,
    };

    public static void enableDisableActivities(final Context context) {
        boolean isHoneycombTablet = isHoneycombTablet(context);
        PackageManager pm = context.getPackageManager();

        // Enable/disable phone activities
        for (Class a : sPhoneActivities) {
            pm.setComponentEnabledSetting(new ComponentName(context, a),
                    isHoneycombTablet
                            ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }

        // Enable/disable tablet activities
        for (Class a : sTabletActivities) {
            pm.setComponentEnabledSetting(new ComponentName(context, a),
                    isHoneycombTablet
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    public static Class getMapActivityClass(Context context) {
        if (UIUtils.isHoneycombTablet(context)) {
            return MapMultiPaneActivity.class;
        }

        return MapActivity.class;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setActivatedCompat(View view, boolean activated) {
        if (hasHoneycomb()) {
            view.setActivated(activated);
        }
    }

    public static boolean isGoogleTV(Context context) {
    	return context.getPackageManager().hasSystemFeature("com.google.android.tv");
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return hasHoneycomb() && isTablet(context);
    }
}
