package com.google.samples.apps.iosched.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Point;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.samples.apps.iosched.lib.R;

import java.text.DateFormat;

public class FeedViewHolder extends RecyclerView.ViewHolder {
    private static final int SHORT_DESCRIPTION_MAX_LINES = 3;
    private static final int LONG_DESCRIPTION_MAX_LINES = 30;
    boolean expanded;
    boolean hasImage;
    private RelativeLayout mainLayout;
    private TextView title;
    private TextView dateTime;
    private ImageView image;
    private TextView description;
    private TextView category;
    private LinearLayout imageDescriptionLayout;
    private ImageView expandIcon;
    private ImageView emergencyIcon;

    public FeedViewHolder(View itemView) {
        super(itemView);
        mainLayout = (RelativeLayout) itemView.findViewById(R.id.feedMessageCardLayout);
        title = (TextView) itemView.findViewById(R.id.title);
        dateTime = (TextView) itemView.findViewById(R.id.dateTime);
        image = (ImageView) itemView.findViewById(R.id.image);
        description = (TextView) itemView.findViewById(R.id.description);
        category = (TextView) itemView.findViewById(R.id.categoryText);
        imageDescriptionLayout =
                (LinearLayout) itemView.findViewById(R.id.imageDescriptionLayout);
        expandIcon = (ImageView) itemView.findViewById(R.id.expandIcon);
        emergencyIcon = (ImageView) itemView.findViewById(R.id.emergencyIcon);
        expanded = false;
        hasImage = false;
    }

    public void updateCategory(String categoryString, int color) {
        category.setText(categoryString);
        category.setBackgroundColor(color);
    }

    public void updateImage(Context context, Point screenSize, String imageUrlString) {
        if (imageUrlString.isEmpty()) {
            image.setVisibility(View.GONE);
        }
        Glide.with(context)
                .load(imageUrlString)
                .override(screenSize.x, screenSize.x * 9 / 16) // Guaranteed 16:9 aspect ratio
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model,
                            Target<GlideDrawable> target, boolean isFirstResource) {
                        hasImage = false;
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model,
                            Target<GlideDrawable> target, boolean isFromMemoryCache,
                            boolean isFirstResource) {
                        hasImage = true;
                        return false;
                    }
                })
                .into(image);
    }

    public void updateDescription(String descriptionString) {
        description.setText(descriptionString);
        if (expanded) {
            description.setMaxLines(Integer.MAX_VALUE);
        } else {
            description.setMaxLines(SHORT_DESCRIPTION_MAX_LINES);
        }
    }

    public void updateDateTime(long unixTime) {
        DateFormat dateFormat = DateFormat
                .getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        dateTime.setText(dateFormat.format(unixTime));
    }

    public void updateTitle(String titleString) {
        title.setText(titleString);
    }

    public void setOnFeedItemExpandListener(
            final OnFeedItemExpandListener onFeedItemExpandListener) {
        expandIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expanded = !expanded;
                onFeedItemExpandListener.onFeedItemExpand();
            }
        });
    }

    public void updateExpandOrCollapse(boolean changed, int paddingNormal, int messageCardImageWidth,
            int messageCardImageHeight) {
        LinearLayout.LayoutParams imageLayoutParams =
                (LinearLayout.LayoutParams) image.getLayoutParams();
        LinearLayout.LayoutParams descriptionLayoutParams =
                (LinearLayout.LayoutParams) description.getLayoutParams();
        if (expanded) {
            imageDescriptionLayout.setOrientation(LinearLayout.VERTICAL);
            imageLayoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            imageLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            descriptionLayoutParams.setMarginStart(0);
            description.setMaxLines(LONG_DESCRIPTION_MAX_LINES);
        } else {
            imageDescriptionLayout.setOrientation(LinearLayout.HORIZONTAL);
            imageLayoutParams.width = messageCardImageWidth;
            imageLayoutParams.height = messageCardImageHeight;
            if (hasImage) {
                descriptionLayoutParams.setMarginStart(paddingNormal);
            }
            description.setMaxLines(SHORT_DESCRIPTION_MAX_LINES);
        }
        if (changed) {
            TransitionManager.beginDelayedTransition(imageDescriptionLayout);
        } else {
            mainLayout.requestLayout();
        }
    }

    public void updateExpandIcon(boolean changed) {
        if (changed && expanded) {
            expandIcon.animate().rotation(180f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    expandIcon.setColorFilter(new LightingColorFilter(Color.BLUE, Color.BLUE));
                }
            }).start();
        } else if (changed && !expanded) {
            expandIcon.animate().rotation(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    expandIcon.setColorFilter(new LightingColorFilter(Color.BLACK, Color.BLACK));
                }
            }).start();
        } else if (!changed && expanded) {
            expandIcon.setRotation(180f);
            expandIcon.setColorFilter(new LightingColorFilter(Color.BLUE, Color.BLUE));
        } else if (!changed && !expanded) {
            expandIcon.setRotation(0f);
            expandIcon.setColorFilter(new LightingColorFilter(Color.BLACK, Color.BLACK));
        }

        float rotationAngle = expanded ? 180f : 0f;
        if (changed) {
            expandIcon.animate().rotation(rotationAngle).start();
        } else {
            expandIcon.setRotation(rotationAngle);
        }
    }

    public void updateIconVisibilityForEmergency() {
        emergencyIcon.setVisibility(View.VISIBLE);
        expandIcon.setVisibility(View.GONE);
    }

    public void updateIconVisibilityForNonEmergency() {
        emergencyIcon.setVisibility(View.GONE);
        expandIcon.setVisibility(View.VISIBLE);
    }
}