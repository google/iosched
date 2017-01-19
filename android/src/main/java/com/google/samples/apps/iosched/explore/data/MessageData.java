package com.google.samples.apps.iosched.explore.data;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.view.View;

/**
 * View data describing a message card to be displayed on the Explore I/O screen.
 */
public class MessageData {
    /**
     * String resource for the message to be displayed on the card.
     */
    private int mMessageStringResourceId = -1;

    /**
     * String resource for the text to be placed on the left aligned button. When RTL is active
     * this indicates the button closer to "start."
     */
    private int mStartButtonStringResourceId = -1;

    /**
     * String resource for the text to be placed on the right-aligned button. When RTL is active
     * this indicates the button closer to "end."
     */
    private int mEndButtonStringResourceId = -1;

    private int mIconDrawableId = -1;

    private String mMessage;

    /**
     * The click listener to be attached to the left aligned button. When RTL is active this
     * indicates the button closer to "start."
     */
    private View.OnClickListener mStartButtonClickListener;

    /**
     * The click listener to be attached to the right aligned button. When RTL is active this
     * indicates the button closer to "end."
     */
    private View.OnClickListener mEndButtonClickListener;

    public void setMessageStringResourceId(int messageStringResourceId) {
        mMessageStringResourceId = messageStringResourceId;
    }

    public Spanned getMessageString(Context context) {
        if (mMessageStringResourceId > 0) {
            return Html.fromHtml(context.getResources().getString(mMessageStringResourceId));
        } else {
            return Html.fromHtml(mMessage);
        }
    }

    public int getEndButtonStringResourceId() {
        return mEndButtonStringResourceId;
    }

    public void setEndButtonStringResourceId(int resourceId) {
        mEndButtonStringResourceId = resourceId;
    }

    public int getStartButtonStringResourceId() {
        return mStartButtonStringResourceId;
    }

    public void setStartButtonStringResourceId(int resourceId) {
        mStartButtonStringResourceId = resourceId;
    }

    public int getIconDrawableId() {
        return mIconDrawableId;
    }

    public void setIconDrawableId(int iconDrawableId) {
        mIconDrawableId = iconDrawableId;
    }

    public View.OnClickListener getEndButtonClickListener() {
        return mEndButtonClickListener;
    }

    public void setEndButtonClickListener(View.OnClickListener clickListener) {
        this.mEndButtonClickListener = clickListener;
    }

    public View.OnClickListener getStartButtonClickListener() {
        return mStartButtonClickListener;
    }

    public void setStartButtonClickListener(View.OnClickListener clickListener) {
        this.mStartButtonClickListener = clickListener;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MessageData that = (MessageData) o;

        return mMessageStringResourceId == that.mMessageStringResourceId;

    }

    @Override
    public int hashCode() {
        return mMessageStringResourceId;
    }
}
