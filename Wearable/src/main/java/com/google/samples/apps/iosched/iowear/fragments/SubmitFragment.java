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
import com.google.samples.apps.iosched.iowear.OnQuestionAnsweredListener;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Te fragment that builds the submission page.
 */
public class SubmitFragment extends Fragment {

    private final OnQuestionAnsweredListener mListener;

    public SubmitFragment(OnQuestionAnsweredListener listener) {
        this.mListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submit, container, false);
        Typeface typeFace = Typeface
                .createFromAsset(getActivity().getAssets(), "RobotoCondensed-Light.ttf");
        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.submit();
            }
        });
        TextView submitText = (TextView) view.findViewById(R.id.submit_text);
        submitText.setTypeface(typeFace);
        return view;
    }

}
