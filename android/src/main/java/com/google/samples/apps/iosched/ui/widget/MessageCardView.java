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

package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MessageCardView extends CardView implements View.OnClickListener {
    private static final String TAG = makeLogTag("MessageCardView");
    private TextView mTitleView;
    private TextView mMessageView;
    private Button[] mButtons;
    private String[] mButtonTags;
    private OnMessageCardButtonClicked mListener = null;
    private View mRoot;
    public static final int ANIM_DURATION = 200;

    public interface OnMessageCardButtonClicked {
        public void onMessageCardButtonClicked(String tag);
    }

    public MessageCardView(Context context) {
        super(context, null, 0);
        initialize(context, null, 0);
    }

    public MessageCardView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        initialize(context, attrs, 0);
    }

    public MessageCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflater.inflate(R.layout.message_card, this, true);
        mTitleView = (TextView) mRoot.findViewById(R.id.title);
        mMessageView = (TextView) mRoot.findViewById(R.id.text);
        mButtons = new Button[] {
                (Button) mRoot.findViewById(R.id.button1),
                (Button) mRoot.findViewById(R.id.button2)
        };
        mButtonTags = new String[] { "", "" };

        for (Button button : mButtons) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(this);
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MessageCard, defStyle, 0);
        String title = a.getString(R.styleable.MessageCard_messageTitle);
        setTitle(title);
        String text = a.getString(R.styleable.MessageCard_messageText);
        if (text != null) {
            setText(text);
        }
        String button1text = a.getString(R.styleable.MessageCard_button1text);
        boolean button1emphasis = a.getBoolean(R.styleable.MessageCard_button1emphasis, false);
        String button1tag = a.getString(R.styleable.MessageCard_button1tag);
        String button2text = a.getString(R.styleable.MessageCard_button2text);
        boolean button2emphasis = a.getBoolean(R.styleable.MessageCard_button2emphasis, false);
        String button2tag = a.getString(R.styleable.MessageCard_button2tag);
        int emphasisColor = a.getColor(R.styleable.MessageCard_emphasisColor,
                getResources().getColor(R.color.theme_primary));

        if (button1text != null) {
            setButton(0, button1text, button1tag, button1emphasis, 0);
        }
        if (button2text != null) {
            setButton(1, button2text, button2tag, button2emphasis, emphasisColor);
        }

        setRadius(getResources().getDimensionPixelSize(R.dimen.card_corner_radius));
        setCardElevation(getResources().getDimensionPixelSize(R.dimen.card_elevation));
        setPreventCornerOverlap(false);
    }

    public void setListener(OnMessageCardButtonClicked listener) {
        mListener = listener;
    }

    public void setButton(int index, String text, String tag, boolean emphasis, int emphasisColor) {
        if (index < 0 || index >= mButtons.length) {
            LOGW(TAG, "Invalid button index: " + index);
            return;
        }
        mButtons[index].setText(text);
        mButtons[index].setVisibility(View.VISIBLE);
        mButtonTags[index] = tag;
        if (emphasis) {
            if (emphasisColor == 0) {
                emphasisColor = getResources().getColor(R.color.theme_primary);
            }
            mButtons[index].setTextColor(emphasisColor);
            mButtons[index].setTypeface(null, Typeface.BOLD);
        }
    }

    /**
     * Use sparingly.
     */
    public void setTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            mTitleView.setVisibility(View.GONE);
        } else {
            mTitleView.setVisibility(View.VISIBLE);
            mTitleView.setText(title);
        }
    }
    public void setText(String text) {
        mMessageView.setText(text);
    }

    public void overrideBackground(int bgResId) {
        findViewById(R.id.card_root).setBackgroundResource(bgResId);
    }

    @Override
    public void onClick(View v) {
        if (mListener == null) {
            return;
        }

        for (int i = 0; i < mButtons.length; i++) {
            if (mButtons[i] == v) {
                mListener.onMessageCardButtonClicked(mButtonTags[i]);
                break;
            }
        }
    }

    public void dismiss() {
        dismiss(false);
    }

    public void dismiss(boolean animate) {
        if (!animate) {
            setVisibility(View.GONE);
        } else {
            animate().scaleY(0.1f).alpha(0.1f).setDuration(ANIM_DURATION);
        }
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }
}
