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

package com.google.samples.apps.iosched.iowear;


import com.google.samples.apps.iosched.R;

import android.content.Context;

/**
 * This class encapsulates a question and its answers.
 */
public class QuestionModel {

    private String mQuestion;
    private String[] mAnswers;
    private int mQuestionNumber;

    public String getQuestion() {
        return mQuestion;
    }

    public String getAnswer(int number) {
        return mAnswers[number];
    }

    public int getQuestionNumber() {
        return mQuestionNumber;
    }

    public QuestionModel(Context context, int questionNumber) {
        int questionId = 0;
        int optionsId = 0;
        switch (questionNumber) {
            case 0:
                questionId = R.string.q0;
                optionsId = R.array.q0_options;
                break;

            case 1:
                questionId = R.string.q1;
                optionsId = R.array.q1_options;
                break;

            case 2:
                questionId = R.string.q2;
                optionsId = R.array.q2_options;
                break;

            case 3:
                questionId = R.string.q3;
                optionsId = R.array.q3_options;
                break;
        }
        mAnswers = context.getResources().getStringArray(optionsId);
        mQuestion = context.getString(questionId);
        mQuestionNumber = questionNumber;
    }

}
