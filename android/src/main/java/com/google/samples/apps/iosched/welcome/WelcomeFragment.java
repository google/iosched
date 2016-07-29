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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.ExploreIOActivity;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Class for use with {@link WelcomeActivity} to embed content into the activity. Contains utilities
 * for attaching the fragment to the activity and updating UI elements.
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

    @Override
    public void onResume() {
        super.onResume();
        TextView titleTV = (TextView) getActivity().findViewById(R.id.title);

        if (titleTV != null) {
            // Set activity to fragment title text view and fire accessibility event so the title
            // gets read by talkback service.
            mActivity.setTitle(titleTV.getText());
            mActivity.getWindow().getDecorView()
                     .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        LOGD(TAG, "Creating View");

        if (mActivity instanceof WelcomeFragmentContainer) {
            WelcomeFragmentContainer activity = (WelcomeFragmentContainer) mActivity;
            attachToPrimaryButton(activity.getPrimaryButton());
            attachToSecondaryButton(activity.getSecondaryButton());
            activity.setButtonBarVisibility(shouldShowButtonBar());
        }
        return view;
    }

    /**
     * Attaches to the primary action button of {@link WelcomeFragmentContainer}.
     *
     * @param button the ui element to attach to.
     */
    protected void attachToPrimaryButton(Button button) {
        button.setText(getPrimaryButtonText());
        button.setOnClickListener(getPrimaryButtonListener());
    }

    public abstract boolean shouldDisplay(Context context);

    /**
     * Attaches to the secondary action button of the WelcomeFragmentContainer.
     *
     * @param button the ui element to attach to.
     */
    protected void attachToSecondaryButton(Button button) {
        String secondaryButtonText = getSecondaryButtonText();
        View.OnClickListener secondaryButtonClickListener = getSecondaryButtonListener();
        if (!TextUtils.isEmpty(secondaryButtonText) && secondaryButtonClickListener != null) {
            button.setVisibility(View.VISIBLE);
            button.setText(secondaryButtonText);
            button.setOnClickListener(secondaryButtonClickListener);
        }
    }


    /**
     * Gets a resource string.
     *
     * @param id the id of the string resource.
     * @return the value of the resource or null.
     */
    protected String getResourceString(int id) {
        if (mActivity != null) {
            return mActivity.getResources().getString(id);
        }
        return null;
    }

    /**
     * Returns the text for the primary action button. Example: "Accept".
     */
    protected abstract String getPrimaryButtonText();

    /**
     * Returns the text for the secondary action button. Example: "Cancel".
     */
    protected abstract String getSecondaryButtonText();

    /**
     * Returns the {@link android.view.View.OnClickListener} for the primary action click event.
     */
    protected abstract View.OnClickListener getPrimaryButtonListener();

    /**
     * Returns the {@link android.view.View.OnClickListener} for the secondary action click event.
     */
    protected abstract View.OnClickListener getSecondaryButtonListener();

    /**
     * Returns whether the button bar should be displayed.
     */
    protected boolean shouldShowButtonBar() {
        return true;
    }

    /**
     * A convenience {@link android.view.View.OnClickListener} for the common use cases.
     */
    protected abstract class WelcomeFragmentOnClickListener implements View.OnClickListener {
        Activity mActivity;

        /**
         * Construct a listener that handles the transition to the next activity.
         *
         * @param activity the Activity to interact with.
         */
        public WelcomeFragmentOnClickListener(Activity activity) {
            mActivity = activity;
        }
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
     * The receiver for the action to be performed on a button click.
     */
    interface WelcomeFragmentClickAction {
        public void doAction(Context context);
    }

    /**
     * The Container for the welcome fragments.
     */
    interface WelcomeFragmentContainer {

        /**
         * Returns the primary action button from the container.
         */
        Button getPrimaryButton();

        /**
         * Enables the primary action button in the container.
         *
         * @param enabled enabled true to enable button, false to disable it.
         */
        void setPrimaryButtonEnabled(Boolean enabled);

        /**
         * Returns the secondary action button from the container.
         */
        Button getSecondaryButton();

        void setButtonBarVisibility(boolean isVisible);
    }
}
