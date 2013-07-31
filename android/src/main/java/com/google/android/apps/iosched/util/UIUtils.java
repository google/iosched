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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.iosched.BuildConfig;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.ui.phone.MapActivity;
import com.google.android.apps.iosched.ui.tablet.MapMultiPaneActivity;

import java.io.*;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An assortment of UI helpers.
 */
public class UIUtils {
    private static final String TAG = makeLogTag(UIUtils.class);

    /**
     * Time zone to use when formatting all session times. To always use the
     * phone local time, use {@link TimeZone#getDefault()}.
     */
    public static final TimeZone CONFERENCE_TIME_ZONE = TimeZone.getTimeZone("America/Los_Angeles");

    public static final long CONFERENCE_START_MILLIS = ParserUtils.parseTime(
            "2013-05-15T09:00:00.000-07:00");
    public static final long CONFERENCE_END_MILLIS = ParserUtils.parseTime(
            "2013-05-17T16:00:00.000-07:00");

    public static final String CONFERENCE_HASHTAG = "#io13";

    public static final String TARGET_FORM_FACTOR_ACTIVITY_METADATA =
            "com.google.android.apps.iosched.meta.TARGET_FORM_FACTOR";

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

    /**
     * Used in {@link #tryTranslateHttpIntent(android.app.Activity)}.
     */
    private static final Uri SESSION_DETAIL_WEB_URL_PREFIX
            = Uri.parse("https://developers.google.com/events/io/sessions/");

    private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);
    private static ForegroundColorSpan sColorSpan = new ForegroundColorSpan(0xff111111);

    private static CharSequence sEmptyRoomText;

    private static CharSequence sNowPlayingText;
    private static CharSequence sLivestreamNowText;
    private static CharSequence sLivestreamAvailableText;

    public static final String GOOGLE_PLUS_PACKAGE_NAME = "com.google.android.apps.plus";

    public static final int ANIMATION_FADE_IN_TIME = 250;
    public static final String TRACK_ICONS_TAG = "tracks";

    /**
     * Format and return the given {@link Blocks} and {@link Rooms} values using
     * {@link #CONFERENCE_TIME_ZONE}.
     */
    public static String formatSessionSubtitle(String sessionTitle,
            long blockStart, long blockEnd, String roomName, StringBuilder recycle,
            Context context) {

        // Determine if the session is in the past
        long currentTimeMillis = UIUtils.getCurrentTime(context);
        boolean conferenceEnded = currentTimeMillis > UIUtils.CONFERENCE_END_MILLIS;
        boolean blockEnded = currentTimeMillis > blockEnd;
        if (blockEnded && !conferenceEnded) {
            return context.getString(R.string.session_finished);
        }

        if (sEmptyRoomText == null) {
            sEmptyRoomText = context.getText(R.string.unknown_room);
        }

        if (roomName == null) {
            return context.getString(R.string.session_subtitle,
                    formatBlockTimeString(blockStart, blockEnd, recycle, context),
                    sEmptyRoomText);
        }

        return context.getString(R.string.session_subtitle,
                formatBlockTimeString(blockStart, blockEnd, recycle, context), roomName);
    }

    /**
     * Format and return the given {@link Blocks} values using {@link #CONFERENCE_TIME_ZONE}
     * (unless local time was explicitly requested by the user).
     */
    public static String formatBlockTimeString(long blockStart, long blockEnd,
            StringBuilder recycle, Context context) {
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        Formatter formatter = new Formatter(recycle);
        return DateUtils.formatDateRange(context, formatter, blockStart, blockEnd, TIME_FLAGS,
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

    public static void updateTimeAndLivestreamBlockUI(final Context context,
            long blockStart, long blockEnd, boolean hasLivestream, TextView titleView,
            TextView subtitleView, CharSequence subtitle) {
        long currentTimeMillis = getCurrentTime(context);

        boolean conferenceEnded = currentTimeMillis > CONFERENCE_END_MILLIS;
        boolean blockEnded = currentTimeMillis > blockEnd;
        boolean blockNow = (blockStart <= currentTimeMillis && currentTimeMillis <= blockEnd);

        if (titleView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                titleView.setTypeface(Typeface.create((blockEnded && !conferenceEnded)
                        ? "sans-serif-light" : "sans-serif", Typeface.NORMAL));
            } else {
                titleView.setTypeface(Typeface.SANS_SERIF,
                        (blockEnded && !conferenceEnded) ? Typeface.NORMAL : Typeface.BOLD);
            }
        }

        if (subtitleView != null) {
            boolean empty = true;
            SpannableStringBuilder sb = new SpannableStringBuilder(); // TODO: recycle
            if (subtitle != null) {
                sb.append(subtitle);
                empty = false;
            }

            if (blockNow) {
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
        int startIndex, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);

            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);

            // Insert bold style
            builder.setSpan(sBoldSpan, startIndex - delta, endIndex - delta - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(sColorSpan, startIndex - delta, endIndex - delta - 1,
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

    /**
     * Create the track icon bitmap. Don't call this directly, instead use either
     * {@link UIUtils.TrackIconAsyncTask} or {@link UIUtils.TrackIconViewAsyncTask} to
     * asynchronously load the track icon.
     */
    private static Bitmap createTrackIcon(Context context, String trackName, int trackColor) {
        final Resources res = context.getResources();
        int iconSize = res.getDimensionPixelSize(R.dimen.track_icon_source_size);
        Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(trackColor);
        canvas.drawCircle(iconSize / 2, iconSize / 2, iconSize / 2, paint);

        int iconResId = res.getIdentifier(
                "track_" + ParserUtils.sanitizeId(trackName),
                "drawable", context.getPackageName());
        if (iconResId != 0) {
            Drawable sourceIconDrawable = res.getDrawable(iconResId);
            sourceIconDrawable.setBounds(0, 0, iconSize, iconSize);
            sourceIconDrawable.draw(canvas);
        }

        return icon;
    }

    /**
     * Synchronously get the track icon bitmap. Don't call this from the main thread, instead use either
     * {@link UIUtils.TrackIconAsyncTask} or {@link UIUtils.TrackIconViewAsyncTask} to
     * asynchronously load the track icon.
     */
    public static Bitmap getTrackIconSync(Context ctx, String trackName, int trackColor) {

        if (TextUtils.isEmpty(trackName)) {
            return null;
        }

        // Find a suitable disk cache directory for the track icons and create if it doesn't
        // already exist.
        File outputDir = ImageLoader.getDiskCacheDir(ctx, TRACK_ICONS_TAG);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Generate a unique filename to store this track icon in using a hash function.
        File imageFile = new File(outputDir + File.separator + hashKeyForDisk(trackName));

        Bitmap bitmap = null;

        // If file already exists and is readable, try and decode the bitmap from the disk.
        if (imageFile.exists() && imageFile.canRead()) {
            bitmap = BitmapFactory.decodeFile(imageFile.toString());
        }

        // If bitmap is still null here the track icon was not found in the disk cache.
        if (bitmap == null) {

            // Create the icon using the provided track name and color.
            bitmap = UIUtils.createTrackIcon(ctx, trackName, trackColor);

            // Now write it out to disk for future use.
            BufferedOutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(imageFile));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } catch (FileNotFoundException e) {
                LOGE(TAG, "TrackIconAsyncTask - unable to open file - " + e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        return bitmap;
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable for using as a
     * disk filename.
     */
    private static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * A subclass of {@link TrackIconAsyncTask} that loads the generated track icon bitmap into
     * the provided {@link ImageView}. This class also handles concurrency in the case the
     * ImageView is recycled (eg. in a ListView adapter) so that the incorrect image will not show
     * in a recycled view.
     */
    public static class TrackIconViewAsyncTask extends TrackIconAsyncTask {
        private WeakReference<ImageView> mImageViewReference;

        public TrackIconViewAsyncTask(ImageView imageView, String trackName, int trackColor,
                BitmapCache bitmapCache) {
            super(trackName, trackColor, bitmapCache);

            // Store this AsyncTask in the tag of the ImageView so we can compare if the same task
            // is still running on this ImageView once processing is complete. This helps with
            // view recycling that takes place in a ListView type adapter.
            imageView.setTag(this);

            // If we have a BitmapCache, check if this track icon is available already.
            Bitmap bitmap =
                    bitmapCache != null ? bitmapCache.getBitmapFromMemCache(trackName) : null;

            // If found in BitmapCache set the Bitmap directly and cancel the task.
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                cancel(true);
            } else {
                // Otherwise clear the ImageView and store a WeakReference for later use. Better
                // to use a WeakReference here in case the task runs long and the holding Activity
                // or Fragment goes away.
                imageView.setImageDrawable(null);
                mImageViewReference = new WeakReference<ImageView>(imageView);
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = mImageViewReference != null ? mImageViewReference.get() : null;

            // If ImageView is still around, bitmap processed OK and this task is not canceled.
            if (imageView != null && bitmap != null && !isCancelled()) {

                // Ensure this task is still the same one assigned to this ImageView, if not the
                // view was likely recycled and a new task with a different icon is now running
                // on the view and we shouldn't proceed.
                if (this.equals(imageView.getTag())) {

                    // On HC-MR1 run a quick fade-in animation.
                    if (hasHoneycombMR1()) {
                        imageView.setAlpha(0f);
                        imageView.setImageBitmap(bitmap);
                        imageView.animate()
                                .alpha(1f)
                                .setDuration(ANIMATION_FADE_IN_TIME)
                                .setListener(null);
                    } else {
                        // Before HC-MR1 set the Bitmap directly.
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }

    /**
     * Asynchronously load the track icon bitmap. To use, subclass and override
     * {@link #onPostExecute(android.graphics.Bitmap)} which passes in the generated track icon
     * bitmap.
     */
    public static abstract class TrackIconAsyncTask extends AsyncTask<Context, Void, Bitmap> {
        private String mTrackName;
        private int mTrackColor;
        private BitmapCache mBitmapCache;

        public TrackIconAsyncTask(String trackName, int trackColor) {
            mTrackName = trackName;
            mTrackColor = trackColor;
        }

        public TrackIconAsyncTask(String trackName, int trackColor, BitmapCache bitmapCache) {
            mTrackName = trackName;
            mTrackColor = trackColor;
            mBitmapCache = bitmapCache;
        }

        @Override
        protected Bitmap doInBackground(Context... contexts) {

            Bitmap bitmap = getTrackIconSync(contexts[0], mTrackName, mTrackColor);

            // Store bitmap in memory cache for future use.
            if (bitmap != null && mBitmapCache != null) {
                mBitmapCache.addBitmapToCache(mTrackName, bitmap);
            }

            return bitmap;
        }

        protected abstract void onPostExecute(Bitmap bitmap);
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

    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return hasHoneycomb() && isTablet(context);
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
                && getCurrentTime(context) < CONFERENCE_END_MILLIS;
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
        boolean isTablet = isHoneycombTablet(context);

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

        String prefixPath = SESSION_DETAIL_WEB_URL_PREFIX.getPath();
        String path = uri.getPath();

        if (SESSION_DETAIL_WEB_URL_PREFIX.getScheme().equals(uri.getScheme()) &&
                SESSION_DETAIL_WEB_URL_PREFIX.getHost().equals(uri.getHost()) &&
                path.startsWith(prefixPath)) {
            String sessionId = path.substring(prefixPath.length());
            activity.setIntent(new Intent(
                    Intent.ACTION_VIEW,
                    ScheduleContract.Sessions.buildSessionUri(sessionId)));
        }
    }

    /**
     * The Conference API image URLs can be absolute or relative. In the case they are relative
     * we should prepend the main Conference URL.
     * @param imageUrl A Conference API image URL
     * @return An absolute image URL
     */
    public static String getConferenceImageUrl(String imageUrl) {
        if (!TextUtils.isEmpty(imageUrl) && !imageUrl.startsWith("http")) {
            return Config.CONFERENCE_IMAGE_PREFIX_URL + imageUrl;
        }
        return imageUrl;
    }
}
