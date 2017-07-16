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

package com.google.samples.apps.iosched.feedback;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackQueryEnum;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackUserActionEnum;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.ui.widget.CustomRatingBar;
import com.google.samples.apps.iosched.util.AnalyticsHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * A fragment that lets the user submit feedback about a given session.
 */
public class SessionFeedbackFragment extends Fragment
        implements UpdatableView<SessionFeedbackModel, SessionFeedbackQueryEnum,
        SessionFeedbackUserActionEnum> {

    private TextView mTitle;

    private TextView mSpeakers;

    private CustomRatingBar mOverallFeedbackBar;

    private CustomRatingBar mSessionRelevantFeedbackBar;

    private CustomRatingBar mContentFeedbackBar;

    private CustomRatingBar mSpeakerFeedbackBar;

    private List<UserActionListener> listeners = new ArrayList<>();

    public SessionFeedbackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.session_feedback_frag, container, false);

        mTitle = (TextView) rootView.findViewById(R.id.feedback_header_session_title);
        mSpeakers = (TextView) rootView.findViewById(R.id.feedback_header_session_speakers);
        mOverallFeedbackBar = (CustomRatingBar) rootView.findViewById(R.id.rating_bar_0);
        mSessionRelevantFeedbackBar = (CustomRatingBar) rootView.findViewById(
                R.id.session_relevant_feedback_bar);
        mContentFeedbackBar = (CustomRatingBar) rootView.findViewById(R.id.content_feedback_bar);
        mSpeakerFeedbackBar = (CustomRatingBar) rootView.findViewById(R.id.speaker_feedback_bar);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Helps accessibility services determine the importance of this view.
            mOverallFeedbackBar
                    .setImportantForAccessibility(RatingBar.IMPORTANT_FOR_ACCESSIBILITY_YES);

            // Automatically notifies the user about changes to the view's content description.
            mOverallFeedbackBar
                    .setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        }

        rootView.findViewById(R.id.submit_feedback_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        submitFeedback();
                    }
                }
        );
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initPresenter();
    }

    private void initPresenter() {
        SessionFeedbackModel model = ModelProvider.provideSessionFeedbackModel(
                ((SessionFeedbackActivity) getActivity()).getSessionUri(), getContext(),
                new FeedbackHelper(getContext()), getLoaderManager());
        PresenterImpl presenter =
                new PresenterImpl(model, this, SessionFeedbackUserActionEnum.values(),
                        SessionFeedbackQueryEnum.values());
        presenter.loadInitialQueries();
    }

    private void submitFeedback() {
        int overallAnswer = mOverallFeedbackBar.getRating();
        int sessionRelevantAnswer = mSessionRelevantFeedbackBar.getRating();
        int contentAnswer = mContentFeedbackBar.getRating();
        int speakerAnswer = mSpeakerFeedbackBar.getRating();
        String comments = "";

        Bundle args = new Bundle();
        args.putInt(SessionFeedbackModel.DATA_RATING_INT, overallAnswer);
        args.putInt(SessionFeedbackModel.DATA_SESSION_RELEVANT_ANSWER_INT, sessionRelevantAnswer);
        args.putInt(SessionFeedbackModel.DATA_CONTENT_ANSWER_INT, contentAnswer);
        args.putInt(SessionFeedbackModel.DATA_SPEAKER_ANSWER_INT, speakerAnswer);
        args.putString(SessionFeedbackModel.DATA_COMMENT_STRING, comments);

        for (UserActionListener h1 : listeners) {
            h1.onUserAction(SessionFeedbackModel.SessionFeedbackUserActionEnum.SUBMIT, args);
        }

        getActivity().finish();
    }

    @Override
    public void displayData(final SessionFeedbackModel model,
            final SessionFeedbackQueryEnum query) {
        switch (query) {
            case SESSION:
                mTitle.setText(model.getSessionTitle());
                if (!TextUtils.isEmpty(model.getSessionSpeakers())) {
                    mSpeakers.setText(model.getSessionSpeakers());
                } else {
                    mSpeakers.setVisibility(View.GONE);
                }

                // ANALYTICS SCREEN: View Send Session Feedback screen
                // Contains: Session title
                AnalyticsHelper.sendScreenView("Feedback: " + model.getSessionTitle());
                break;
        }
    }

    @Override
    public void displayErrorMessage(final SessionFeedbackQueryEnum query) {
        // Close the Activity
        getActivity().finish();
    }

    @Override
    public void displayUserActionResult(final SessionFeedbackModel model,
            final SessionFeedbackUserActionEnum userAction, final boolean success) {
        // User actions all handled in model
    }

    @Override
    public Uri getDataUri(final SessionFeedbackQueryEnum query) {
        switch (query) {
            case SESSION:
                return ((SessionFeedbackActivity) getActivity()).getSessionUri();
            default:
                return null;

        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void addListener(UserActionListener listener) {
        listeners.add(listener);
    }
}
