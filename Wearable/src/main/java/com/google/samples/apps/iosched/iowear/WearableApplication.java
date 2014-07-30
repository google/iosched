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

import android.app.Application;

public class WearableApplication extends Application {

    public static float SMALL_TEXT_SIZE;
    public static float MEDIUM_TEXT_SIZE;
    public static QuestionModel[] mQuestions = new QuestionModel[4];

    @Override
    public void onCreate() {
        super.onCreate();
        SMALL_TEXT_SIZE = getResources().getDimension(R.dimen.question_small_text_size);
        MEDIUM_TEXT_SIZE = getResources().getDimension(R.dimen.question_medium_text_size);
        mQuestions[0] = new QuestionModel(getApplicationContext(), 0);
        mQuestions[1] = new QuestionModel(getApplicationContext(), 1);
        mQuestions[2] = new QuestionModel(getApplicationContext(), 2);
        mQuestions[3] = new QuestionModel(getApplicationContext(), 3);

    }

    public static QuestionModel getQuestion(int number) {
        return mQuestions[number];
    }
}
