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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * The fragment that shows a question along with 5 responses for user to choose from.
 */
public class RadioFragment extends FeedbackFragment{

    private TextView mQuestionView;
    private Typeface mTypeFace;
    private QuestionModel mQuestion;
    private RadioButton[] mRadioButtons = new RadioButton[5];
    private RadioGroup mRadioGroup;
    private int mPersistedAnswer;

    public static RadioFragment newInstance(int questionNumber, int persistedResponse) {
        RadioFragment fragment = new RadioFragment();
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
        View view = inflater.inflate(R.layout.fragment_generic_question, container, false);
        mQuestion = WearableApplication.getQuestion(getArguments().getInt(KEY_QUESTION));
        mPersistedAnswer = getArguments().getInt(KEY_PERSISTED_RESPONSE);
        setupViews(view);
        loadTexts();
        return view;
    }

    private void setupViews(View view) {
        mQuestionView = (TextView) view.findViewById(R.id.question);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        mRadioButtons[0] = (RadioButton) view.findViewById(R.id.radioButton1);
        mRadioButtons[1] = (RadioButton) view.findViewById(R.id.radioButton2);
        mRadioButtons[2] = (RadioButton) view.findViewById(R.id.radioButton3);
        mRadioButtons[3] = (RadioButton) view.findViewById(R.id.radioButton4);
        mRadioButtons[4] = (RadioButton) view.findViewById(R.id.radioButton5);
        for (RadioButton radio : mRadioButtons) {
            radio.setTypeface(mTypeFace);
        }
        if (mPersistedAnswer > -1) {
            // we have a persisted response
            mRadioButtons[mPersistedAnswer].setChecked(true);
        }
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int answer = -1;
                switch(i) {
                    case R.id.radioButton1:
                        answer = 0;
                        break;
                    case R.id.radioButton2:
                        answer = 1;
                        break;
                    case R.id.radioButton3:
                        answer = 2;
                        break;
                    case R.id.radioButton4:
                        answer = 3;
                        break;
                    case R.id.radioButton5:
                        answer = 4;
                        break;
                }
                mListener.onQuestionAnswered(mQuestion.getQuestionNumber(), answer);
            }
        });
    }

    private void loadTexts(){
        String[] options = null;
        String question = null;
        switch (mQuestion.getQuestionNumber()) {
            case 1:
                options = getResources().getStringArray(R.array.q1_options);
                question = getString(R.string.q1);
                break;

            case 2:
                options = getResources().getStringArray(R.array.q2_options);
                question = getString(R.string.q2);
                break;

            case 3:
                options = getResources().getStringArray(R.array.q3_options);
                question = getString(R.string.q3);
                break;
        }
        mQuestionView.setTypeface(mTypeFace, Typeface.ITALIC);
        mQuestionView.setText(question);
        for (int i=0; i < options.length; i++) {
            mRadioButtons[i].setText(options[i]);
        }
    }
}
