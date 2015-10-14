/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.welcome;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.samples.apps.iosched.explore.ExploreIOActivity;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A Fragment class for use with {@link WelcomeActivity} to embed content into the activity.
 *
 * Contains utitlies for attaching the fragment to the activity and updating UI elements.
 */
public abstract class WelcomeFragment extends Fragment {

    private static final String TAG = makeLogTag(WelcomeFragment.class);
    protected Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        LOGD(TAG, "Attaching to activity");
        mActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        LOGD(TAG, "Creating View");

        // If the acitivty the fragment has been attached to is a WelcomeFragmentContainer
        if (mActivity instanceof WelcomeFragmentContainer) {
            WelcomeFragmentContainer activity = (WelcomeFragmentContainer) mActivity;

            // Attach to the UI elements
            attachToPositiveButton(activity.getPositiveButton());
            attachToNegativeButton(activity.getNegativeButton());
        }
        return view;
    }

    /**
     * Attach to the positive action button of the WelcomeFragmentContainer.
     *
     * @param button the ui element to attach to.
     */
    protected void attachToPositiveButton(Button button) {
        // Set the button text
        button.setText(getPositiveText());

        // Set the click listener
        button.setOnClickListener(getPositiveListener());
    }

    /**
     * Attach to the negative action button of the WelcomeFragmentContainer.
     *
     * @param button the ui element to attach to.
     */
    protected void attachToNegativeButton(Button button) {
        // Set the button text
        button.setText(getNegativeText());

        // Set the click listener
        button.setOnClickListener(getNegativeListener());
    }


    /**
     * Get a resource string.
     *
     * @param id the id of the string resource.
     * @return the value of the resource or null.
     */
    protected String getResourceString(int id) {
        if(mActivity != null) {
            return mActivity.getResources().getString(id);
        }
        return null;
    }

    /**
     * Get the text for the positive action button.
     *
     * E.g. Accept
     *
     * @return the text for the button.
     */
    protected abstract String getPositiveText();

    /**
     * Get the text for the negative action button.
     *
     * E.g. Decline
     *
     * @return the text for the negative action button.
     */
    protected abstract String getNegativeText();

    /**
     * Get the {@link android.view.View.OnClickListener} for the positive action click event.
     *
     * @return the click listener.
     */
    protected abstract View.OnClickListener getPositiveListener();

    /**
     * Get the {@link android.view.View.OnClickListener} for the negative action click event.
     *
     * @return the click listener.
     */
    protected abstract View.OnClickListener getNegativeListener();

    /**
     * A convience {@link android.view.View.OnClickListener} for the common use case in the
     * WelcomeActivityContent.
     */
    protected abstract class WelcomeFragmentOnClickListener implements View.OnClickListener {
        /**
         * The action to perform on click, before proceeding to the next activity or exiting the
         * app.
         */
        Activity mActivity;

        /**
         * Construct a listener that will handle the transition to the next activity or exit after
         * completing.
         *
         * @param activity the Activity to interact with.
         */
        public WelcomeFragmentOnClickListener(Activity activity) {
            mActivity = activity;
        }

        /**
         * Proceed to the next activity.
         */
        void doNext() {
            LOGD(TAG, "Proceeding to next activity");
            Intent intent = new Intent(mActivity, ExploreIOActivity.class);
            startActivity(intent);
            mActivity.finish();
        }

        /**
         * Finish the activity.
         *
         * We're done here.
         */
        void doFinish() {
            LOGD(TAG, "Closing app");
            mActivity.finish();
        }
    }

    /**
     * The receiever for the action to be performed on a button click.
     */
    interface WelcomeFragmentClickAction {
        public void doAction(Context context);
    }

    /**
     * The Container for the WelcomeActivityContent.
     */
    interface WelcomeFragmentContainer {

        /**
         * Retrieve a posistive action button from the container.
         *
         * @return the positive action button.
         */
        public Button getPositiveButton();

        /**
         * Enable the positive action button in the container.
         *
         * @param enabled true to enable it, false to disable it.
         */
        public void setPositiveButtonEnabled(Boolean enabled);

        /**
         * Retrieve a negative action button from the container.
         *
         * @return the negative action button.
         */
        public Button getNegativeButton();

        /**
         * Enable the negative action button in the container.
         *
         * @param enabled true to enable it, false to disable it.
         */
        public void setNegativeButtonEnabled(Boolean enabled);
    }
}
