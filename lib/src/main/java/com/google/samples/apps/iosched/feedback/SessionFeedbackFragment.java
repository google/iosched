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

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackQueryEnum;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackUserActionEnum;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.ui.widget.CustomRatingBar;
import com.google.samples.apps.iosched.util.AnalyticsHelper;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import static android.view.View.*;


/**
 * A fragment that lets the user submit feedback about a given session.
 */
public class SessionFeedbackFragment extends Fragment
        implements UpdatableView<SessionFeedbackModel, SessionFeedbackQueryEnum,
        SessionFeedbackUserActionEnum> {

    private CollapsingToolbarLayout mCollapsingToolbar;

    private TextView mSpeakers;

    private CustomRatingBar mOverallFeedbackBar;

    private CustomRatingBar mSessionRelevantFeedbackBar;

    private CustomRatingBar mContentFeedbackBar;

    private CustomRatingBar mSpeakerFeedbackBar;

    private List<UserActionListener<SessionFeedbackUserActionEnum>> listeners = new ArrayList<>();

    public SessionFeedbackFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.session_feedback_frag, container, false);
        mCollapsingToolbar =
                (CollapsingToolbarLayout) rootView.findViewById(R.id.collapsing_toolbar);
        final Typeface productSans =
                TypefaceUtils.load(getContext().getAssets(), "fonts/ProductSans-Regular.ttf");
        mCollapsingToolbar.setExpandedTitleTypeface(productSans);
        mCollapsingToolbar.setCollapsedTitleTypeface(productSans);
        mSpeakers = (TextView) rootView.findViewById(R.id.feedback_header_session_speakers);
        mOverallFeedbackBar = (CustomRatingBar) rootView.findViewById(R.id.rating_bar_0);
        mSessionRelevantFeedbackBar = (CustomRatingBar) rootView.findViewById(
                R.id.session_relevant_feedback_bar);
        mContentFeedbackBar = (CustomRatingBar) rootView.findViewById(R.id.content_feedback_bar);
        mSpeakerFeedbackBar = (CustomRatingBar) rootView.findViewById(R.id.speaker_feedback_bar);
        rootView.findViewById(R.id.submit_feedback_button).setOnClickListener(
                new OnClickListener() {
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

    @Override
    public void displayData(SessionFeedbackModel model, SessionFeedbackQueryEnum query) {
        switch (query) {
            case SESSION:
                mCollapsingToolbar.setTitle(model.getSessionTitle());
                if (!TextUtils.isEmpty(model.getSessionSpeakers())) {
                    mSpeakers.setText(model.getSessionSpeakers());
                } else {
                    mSpeakers.setVisibility(GONE);
                }

                // ANALYTICS SCREEN: View Send Session Feedback screen
                // Contains: Session title
                AnalyticsHelper.sendScreenView("Feedback: " + model.getSessionTitle(),
                        getActivity());
                break;
        }
    }

    @Override
    public void displayErrorMessage(SessionFeedbackQueryEnum query) {
        // Close the Activity
        getActivity().finish();
    }

    @Override
    public void displayUserActionResult( SessionFeedbackModel model,
            final SessionFeedbackUserActionEnum userAction, final boolean success) {
        // User actions all handled in model
    }

    @Override
    public Uri getDataUri(SessionFeedbackQueryEnum query) {
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
    public void addListener(UserActionListener<SessionFeedbackUserActionEnum> listener) {
        listeners.add(listener);
    }

    private void initPresenter() {
        SessionFeedbackModel model = ModelProvider.provideSessionFeedbackModel(
                ((SessionFeedbackActivity) getActivity()).getSessionUri(), getContext(),
                new FeedbackHelper(getContext()), getLoaderManager());
        PresenterImpl<SessionFeedbackModel, SessionFeedbackQueryEnum, SessionFeedbackUserActionEnum>
                presenter = new PresenterImpl<>(model, this, SessionFeedbackUserActionEnum.values(),
                SessionFeedbackQueryEnum.values());
        presenter.loadInitialQueries();
    }

    private void submitFeedback() {
        Bundle args = new Bundle();
        args.putInt(SessionFeedbackModel.DATA_RATING_INT, mOverallFeedbackBar.getRating());
        args.putInt(SessionFeedbackModel.DATA_SESSION_RELEVANT_ANSWER_INT,
                mSessionRelevantFeedbackBar.getRating());
        args.putInt(SessionFeedbackModel.DATA_CONTENT_ANSWER_INT, mContentFeedbackBar.getRating());
        args.putInt(SessionFeedbackModel.DATA_SPEAKER_ANSWER_INT, mSpeakerFeedbackBar.getRating());
        args.putString(SessionFeedbackModel.DATA_COMMENT_STRING, "");

        for (UserActionListener<SessionFeedbackUserActionEnum> listener : listeners) {
            listener.onUserAction(SessionFeedbackUserActionEnum.SUBMIT, args);
        }
        getActivity().finish();
    }
}
