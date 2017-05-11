/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.info;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.text.method.LinkMovementMethod;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.ui.widget.HtmlTextView;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

public class CollapsibleCard extends FrameLayout {

    private boolean mExpanded = false;
    private TextView mCardTitle;
    private HtmlTextView mCardDescription;
    private ImageView mExpandIcon;
    private View mTitleContainer;

    public CollapsibleCard(Context context) {
        this(context, null);
    }

    public CollapsibleCard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollapsibleCard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.CollapsibleCard, 0, 0);
        final String cardTitle = arr.getString(R.styleable.CollapsibleCard_cardTitle);
        final String cardDescription = arr.getString(R.styleable.CollapsibleCard_cardDescription);
        arr.recycle();
        final View root = LayoutInflater.from(context)
                .inflate(R.layout.collapsible_card_content, this, true);

        mTitleContainer = root.findViewById(R.id.title_container);
        mCardTitle = (TextView) root.findViewById(R.id.card_title);
        mCardTitle.setText(cardTitle);
        setTitleContentDescription(cardTitle);
        mCardDescription = (HtmlTextView) root.findViewById(R.id.card_description);
        mCardDescription.setHtmlText(cardDescription);
        mExpandIcon = (ImageView) root.findViewById(R.id.expand_icon);
        if (SDK_INT < M) {
            mExpandIcon.setImageTintList(
                    AppCompatResources.getColorStateList(context, R.color.collapsing_section));
        }
        final Transition toggle = TransitionInflater.from(getContext())
                .inflateTransition(R.transition.info_card_toggle);
        final OnClickListener expandClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpanded = !mExpanded;
                toggle.setDuration(mExpanded ? 300L : 200L);
                TransitionManager.beginDelayedTransition((ViewGroup) root.getParent(), toggle);
                mCardDescription.setVisibility(mExpanded ? VISIBLE : GONE);
                mExpandIcon.setRotation(mExpanded ? 180f : 0f);
                // activated used to tint controls when expanded
                mExpandIcon.setActivated(mExpanded);
                mCardTitle.setActivated(mExpanded);
                setTitleContentDescription(cardTitle);
            }
        };
        mTitleContainer.setOnClickListener(expandClick);
    }

    public void setCardDescription(@NonNull String description) {
        mCardDescription.setHtmlText(description);
    }

    private void setTitleContentDescription(String cardTitle) {
        Resources res = getResources();
        mCardTitle.setContentDescription(cardTitle + ", " +
                (mExpanded ? res.getString(R.string.expanded) :
                        res.getString(R.string.collapsed)));
    }
}
