
/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.iosched.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.R.drawable;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.api.services.plus.model.Activity;

import java.util.ArrayList;
import java.util.List;

import static com.google.api.services.plus.model.Activity.PlusObject.Attachments.Thumbnails;

/**
 * Renders a Google+-like stream item.
 */
class PlusStreamRowViewBinder {
    private static class ViewHolder {
        // Author and metadata box
        private View authorContainer;
        private ImageView userImage;
        private TextView userName;
        private TextView time;

        // Author's content
        private TextView content;

        // Original share box
        private View originalContainer;
        private TextView originalAuthor;
        private TextView originalContent;

        // Media box
        private View mediaContainer;
        private ImageView mediaBackground;
        private ImageView mediaOverlay;
        private TextView mediaTitle;
        private TextView mediaSubtitle;

        // Interactions box
        private View interactionsContainer;
        private TextView plusOnes;
        private TextView shares;
        private TextView comments;
    }

    private static int PLACEHOLDER_USER_IMAGE = 0;
    private static int PLACEHOLDER_MEDIA_IMAGE = 1;

    public static ImageLoader getPlusStreamImageLoader(FragmentActivity activity,
            Resources resources) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int largestWidth = metrics.widthPixels > metrics.heightPixels ?
                metrics.widthPixels : metrics.heightPixels;

        // Create list of placeholder drawables (this ImageLoader requires two different
        // placeholder images).
        ArrayList<Drawable> placeHolderDrawables = new ArrayList<Drawable>(2);
        placeHolderDrawables.add(PLACEHOLDER_USER_IMAGE,
                resources.getDrawable(drawable.person_image_empty));
        placeHolderDrawables.add(PLACEHOLDER_MEDIA_IMAGE, new ColorDrawable(
                resources.getColor(R.color.plus_empty_image_background_color)));

        // Create ImageLoader instance
        return new ImageLoader(activity, placeHolderDrawables)
                .setMaxImageSize(largestWidth);
    }

    public static void bindActivityView(final View rootView, Activity activity,
            ImageLoader imageLoader, boolean singleSourceMode) {
        // Prepare view holder.
        ViewHolder tempViews = (ViewHolder) rootView.getTag();
        final ViewHolder views;
        if (tempViews != null) {
            views = tempViews;
        } else {
            views = new ViewHolder();
            rootView.setTag(views);

            // Author and metadata box
            views.authorContainer = rootView.findViewById(R.id.stream_author_container);
            views.userImage = (ImageView) rootView.findViewById(R.id.stream_user_image);
            views.userName = (TextView) rootView.findViewById(R.id.stream_user_name);
            views.time = (TextView) rootView.findViewById(R.id.stream_time);

            // Author's content
            views.content = (TextView) rootView.findViewById(R.id.stream_content);

            // Original share box
            views.originalContainer = rootView.findViewById(
                    R.id.stream_original_container);
            views.originalAuthor = (TextView) rootView.findViewById(
                    R.id.stream_original_author);
            views.originalContent = (TextView) rootView.findViewById(
                    R.id.stream_original_content);

            // Media box
            views.mediaContainer = rootView.findViewById(R.id.stream_media_container);
            views.mediaBackground = (ImageView) rootView.findViewById(
                    R.id.stream_media_background);
            views.mediaOverlay = (ImageView) rootView.findViewById(R.id.stream_media_overlay);
            views.mediaTitle = (TextView) rootView.findViewById(R.id.stream_media_title);
            views.mediaSubtitle = (TextView) rootView.findViewById(R.id.stream_media_subtitle);

            // Interactions box
            views.interactionsContainer = rootView.findViewById(
                    R.id.stream_interactions_container);
            views.plusOnes = (TextView) rootView.findViewById(R.id.stream_plus_ones);
            views.shares = (TextView) rootView.findViewById(R.id.stream_shares);
            views.comments = (TextView) rootView.findViewById(R.id.stream_comments);
        }

        final Context context = rootView.getContext();
        final Resources res = context.getResources();

        // Determine if this is a reshare (affects how activity fields are to be interpreted).
        Activity.PlusObject.Actor originalAuthor = activity.getObject().getActor();
        boolean isReshare = "share".equals(activity.getVerb()) && originalAuthor != null;

        // Author and metadata box
        views.authorContainer.setVisibility(singleSourceMode ? View.GONE : View.VISIBLE);
        views.userName.setText(activity.getActor().getDisplayName());

        // Find user profile image url
        String userImageUrl = null;
        if (activity.getActor().getImage() != null) {
            userImageUrl = activity.getActor().getImage().getUrl();
        }

        // Load image from network in background thread using Volley library
        imageLoader.get(userImageUrl, views.userImage, PLACEHOLDER_USER_IMAGE);

        long thenUTC = activity.getUpdated().getValue()
                + activity.getUpdated().getTimeZoneShift() * 60000;
        views.time.setText(DateUtils.getRelativeTimeSpanString(thenUTC,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_RELATIVE));

        // Author's additional content
        String selfContent = isReshare
                ? activity.getAnnotation()
                : activity.getObject().getContent();
        views.content.setMaxLines(singleSourceMode ? 1000 : 5);
        if (!TextUtils.isEmpty(selfContent)) {
            views.content.setVisibility(View.VISIBLE);
            views.content.setText(Html.fromHtml(selfContent));
        } else {
            views.content.setVisibility(View.GONE);
        }

        // Original share box
        if (isReshare) {
            views.originalContainer.setVisibility(View.VISIBLE);

            // Set original author text, highlight author name
            final String author = res.getString(
                    R.string.stream_originally_shared, originalAuthor.getDisplayName());
            final SpannableStringBuilder spannableAuthor = new SpannableStringBuilder(author);
            spannableAuthor.setSpan(new StyleSpan(Typeface.BOLD),
                    author.length() - originalAuthor.getDisplayName().length(), author.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            views.originalAuthor.setText(spannableAuthor, TextView.BufferType.SPANNABLE);

            String originalContent = activity.getObject().getContent();
            views.originalContent.setMaxLines(singleSourceMode ? 1000 : 3);
            if (!TextUtils.isEmpty(originalContent)) {
                views.originalContent.setVisibility(View.VISIBLE);
                views.originalContent.setText(Html.fromHtml(originalContent));
            } else {
                views.originalContent.setVisibility(View.GONE);
            }
        } else {
            views.originalContainer.setVisibility(View.GONE);
        }

        // Media box

        // Set media content.
        List<Activity.PlusObject.Attachments> attachments
                = activity.getObject().getAttachments();
        if (attachments != null && attachments.size() > 0) {
            Activity.PlusObject.Attachments attachment = attachments.get(0);
            String objectType = attachment.getObjectType();
            String imageUrl = attachment.getImage() != null
                    ? attachment.getImage().getUrl()
                    : null;
            if (imageUrl == null && attachment.getThumbnails() != null
                    && attachment.getThumbnails().size() > 0) {
                Thumbnails thumb = attachment.getThumbnails().get(0);
                imageUrl = thumb.getImage() != null
                        ? thumb.getImage().getUrl()
                        : null;
            }

            // Load image from network in background thread using Volley library
            imageLoader.get(imageUrl, views.mediaBackground, PLACEHOLDER_MEDIA_IMAGE);

            boolean overlayStyle = false;

            views.mediaOverlay.setImageDrawable(null);
            if (("photo".equals(objectType)
                    || "video".equals(objectType)
                    || "album".equals(objectType))
                    && !TextUtils.isEmpty(imageUrl)) {
                overlayStyle = true;
                views.mediaOverlay.setImageResource("video".equals(objectType)
                        ? R.drawable.ic_stream_media_overlay_video
                        : R.drawable.ic_stream_media_overlay_photo);

            } else if ("article".equals(objectType) || "event".equals(objectType)) {
                overlayStyle = false;
                views.mediaTitle.setText(attachment.getDisplayName());
                if (!TextUtils.isEmpty(attachment.getUrl())) {
                    Uri uri = Uri.parse(attachment.getUrl());
                    views.mediaSubtitle.setText(uri.getHost());
                } else {
                    views.mediaSubtitle.setText("");
                }
            }

            views.mediaContainer.setVisibility(View.VISIBLE);
            views.mediaContainer.setBackgroundResource(
                    overlayStyle ? R.color.plus_stream_media_background : android.R.color.black);
            if (overlayStyle) {
                views.mediaBackground.clearColorFilter();
            } else {
                views.mediaBackground.setColorFilter(res.getColor(R.color.plus_media_item_tint));
            }
            views.mediaOverlay.setVisibility(overlayStyle ? View.VISIBLE : View.GONE);
            views.mediaTitle.setVisibility(overlayStyle ? View.GONE : View.VISIBLE);
            views.mediaSubtitle.setVisibility(overlayStyle ? View.GONE : View.VISIBLE);
        } else {
            views.mediaContainer.setVisibility(View.GONE);
            views.mediaBackground.setImageDrawable(null);
            views.mediaOverlay.setImageDrawable(null);
        }

        // Interactions box
        final int plusOneCount = (activity.getObject().getPlusoners() != null)
                ? activity.getObject().getPlusoners().getTotalItems().intValue() : 0;
        if (plusOneCount > 0) {
            views.plusOnes.setVisibility(View.VISIBLE);
            views.plusOnes.setText(getPlusOneString(plusOneCount));
        } else {
            views.plusOnes.setVisibility(View.GONE);
        }

        final int commentCount = (activity.getObject().getReplies() != null)
                ? activity.getObject().getReplies().getTotalItems().intValue() : 0;
        if (commentCount > 0) {
            views.comments.setVisibility(View.VISIBLE);
            views.comments.setText(Integer.toString(commentCount));
        } else {
            views.comments.setVisibility(View.GONE);
        }

        final int resharerCount = (activity.getObject().getResharers() != null)
                ? activity.getObject().getResharers().getTotalItems().intValue() : 0;
        if (resharerCount > 0) {
            views.shares.setVisibility(View.VISIBLE);
            views.shares.setText(Integer.toString(resharerCount));
        } else {
            views.shares.setVisibility(View.GONE);
        }

        views.interactionsContainer.setVisibility(
                (plusOneCount > 0 || commentCount > 0 || resharerCount > 0)
                        ? View.VISIBLE : View.GONE);
    }

    private static final String LRM_PLUS = "\u200E+";

    private static String getPlusOneString(int count) {
        return LRM_PLUS + Integer.toString(count);
    }
}
