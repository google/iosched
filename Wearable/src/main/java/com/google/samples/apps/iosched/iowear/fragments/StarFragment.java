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

package com.google.samples.apps.iosched.iowear.fragments;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.iowear.QuestionModel;
import com.google.samples.apps.iosched.iowear.WearableApplication;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * The fragment that builds the Overall Rating page.
 */
public class StarFragment extends FeedbackFragment {

    private TextView mQuestionView;
    private RatingBar mRatingBar;
    private Typeface mTypeFace;
    private QuestionModel mQuestion;
    private boolean mInitializing;

    public static StarFragment newInstance(int questionNumber, int persistedResponse) {
        StarFragment fragment = new StarFragment();
        Bundle b = new Bundle();
        b.putInt(KEY_QUESTION, questionNumber);
        b.putInt(KEY_PERSISTED_RESPONSE, persistedResponse);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mTypeFace = Typeface
                .createFromAsset(getActivity().getAssets(), "RobotoCondensed-Light.ttf");
        View view = inflater.inflate(R.layout.fragment_star, container, false);
        mQuestion = WearableApplication.getQuestion(getArguments().getInt(KEY_QUESTION));
        setupViews(view);
        loadTexts();
        return view;
    }

    private void setupViews(View view) {
        mQuestionView = (TextView) view.findViewById(R.id.question);
        mRatingBar = (RatingBar) view.findViewById(R.id.ratingbar);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                int ratingInt = (int) rating - 1;
                setQandA(ratingInt);
                if (!mInitializing) {
                    mListener.onQuestionAnswered(mQuestion.getQuestionNumber(), ratingInt);
                    mInitializing = false;
                }
            }
        });
        mRatingBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int ratingInt = (int) mRatingBar.getRating() - 1;
                setQandA(ratingInt);
                return false;
            }
        });
        int persistedResponse = getArguments().getInt(KEY_PERSISTED_RESPONSE);
        if(persistedResponse > -1) {
            mInitializing = true;
            mRatingBar.setRating(persistedResponse + 1);
        }
    }

    private void loadTexts() {
        setQandA(-1);
        mQuestionView.setTypeface(mTypeFace, Typeface.ITALIC);
    }

    private void setQandA(int answer) {
        mQuestionView.setText(answer < 0 ? mQuestion.getQuestion() : mQuestion.getAnswer(answer));
        mQuestionView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                answer < 0 ? WearableApplication.SMALL_TEXT_SIZE
                        : WearableApplication.MEDIUM_TEXT_SIZE);
    }

    public void removeOnQuestionListener() {
        this.mListener  = null;
    }

    public void reshowQuestion(){
        setQandA(-1);
    }
}
