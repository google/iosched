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

import com.google.samples.apps.iosched.iowear.OnQuestionAnsweredListener;

import android.app.Fragment;

/**
 * The base class for feedback fragments.
 */
public class FeedbackFragment extends Fragment {

    protected  OnQuestionAnsweredListener mListener;
    protected static final String KEY_PERSISTED_RESPONSE = "persistedResponse";
    protected static final String KEY_QUESTION = "question";

    public void setOnQuestionListener(OnQuestionAnsweredListener listener) {
        if (null != listener) {
            this.mListener = listener;
        }
    }

    public void removeOnQuestionListener() {
        this.mListener  = null;
    }

    public void reshowQuestion(){
       // do nothing, let the subclasses handle
    }


}
