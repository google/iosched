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

package com.google.samples.apps.iosched.explore;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.data.ItemGroup;
import com.google.samples.apps.iosched.explore.data.LiveStreamData;
import com.google.samples.apps.iosched.explore.data.MessageData;
import com.google.samples.apps.iosched.explore.data.SessionData;
import com.google.samples.apps.iosched.explore.data.ThemeGroup;
import com.google.samples.apps.iosched.explore.data.TopicGroup;
import com.google.samples.apps.iosched.framework.PresenterFragmentImpl;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UpdatableView;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.ThrottledContentObserver;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import static com.google.samples.apps.iosched.settings.ConfMessageCardUtils.ConferencePrefChangeListener;
import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Display the Explore I/O cards. There are three styles of cards, which are
 * referred to as Groups by the {@link CollectionView} implementation.
 * <p/>
 * <ul>
 *     <li>The live-streaming session card.</li>
 *     <li>Time sensitive message cards.</li>
 *     <li>Session topic cards.</li>
 * </ul>
 * <p/>
 * Only the final group of cards is dynamically loaded from a
 * {@link android.content.ContentProvider}.
 */
public class ExploreIOFragment extends Fragment implements UpdatableView<ExploreModel>,
        CollectionViewCallbacks {

    private static final String TAG = makeLogTag(ExploreIOFragment.class);

    private static final int GROUP_ID_KEYNOTE_STREAM_CARD = 10;

    private static final int GROUP_ID_LIVE_STREAM_CARD = 15;

    private static final int GROUP_ID_MESSAGE_CARDS = 20;

    private static final int GROUP_ID_TOPIC_CARDS = 30;

    private static final int GROUP_ID_THEME_CARDS = 40;

    /**
     * Used to load images asynchronously on a background thread.
     */
    private ImageLoader mImageLoader;

    /**
     * CollectionView representing the cards displayed to the user.
     */
    private CollectionView mCollectionView = null;

    /**
     * Empty view displayed when {@code mCollectionView} is empty.
     */
    private View mEmptyView;

    private List<UserActionListener> mListeners = new ArrayList<>();

    private ThrottledContentObserver mSessionsObserver, mTagsObserver;

    private ConferencePrefChangeListener mConfMessagesAnswerChangeListener =
            new ConferencePrefChangeListener() {
        @Override
        protected void onPrefChanged(String key, boolean value) {
            fireReloadEvent();
        }
    };

    private OnSharedPreferenceChangeListener mSettingsChangeListener =
            new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (SettingsUtils.PREF_DECLINED_WIFI_SETUP.equals(key)) {
                fireReloadEvent();
            }
        }
    };

    @Override
    public void displayData(ExploreModel model, QueryEnum query) {
        // Only display data when the tag metadata is available.
        if (model.getTagTitles() != null) {
            updateCollectionView(model);
        }
    }

    @Override
    public void displayErrorMessage(QueryEnum query) {
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void addListener(UserActionListener toAdd) {
        mListeners.add(toAdd);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.explore_io_frag, container, false);
        mCollectionView = (CollectionView) root.findViewById(R.id.explore_collection_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        getActivity().overridePendingTransition(0, 0);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), R.drawable.io_logo);
    }

    private void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();

        // configure fragment's top clearance to take our overlaid controls (Action Bar
        // and spinner box) into account.
        int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Register preference change listeners
        ConfMessageCardUtils.registerPreferencesChangeListener(getContext(),
                mConfMessagesAnswerChangeListener);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        sp.registerOnSharedPreferenceChangeListener(mSettingsChangeListener);

        // Register content observers
        mSessionsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                fireReloadEvent();
                fireReloadTagsEvent();
            }
        });
        mTagsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                fireReloadTagsEvent();
            }
        });

    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mConfMessagesAnswerChangeListener != null) {
            ConfMessageCardUtils.unregisterPreferencesChangeListener(getContext(),
                    mConfMessagesAnswerChangeListener);
        }
        if (mSettingsChangeListener != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            sp.unregisterOnSharedPreferenceChangeListener(mSettingsChangeListener);
        }
        getActivity().getContentResolver().unregisterContentObserver(mSessionsObserver);
        getActivity().getContentResolver().unregisterContentObserver(mTagsObserver);
    }

    /**
     * Update the CollectionView with a new {@link CollectionView.Inventory} of cards to display.
     */
    private void updateCollectionView(ExploreModel model) {
        LOGD(TAG, "Updating collection view.");

        CollectionView.Inventory inventory = new CollectionView.Inventory();
        CollectionView.InventoryGroup inventoryGroup;

        // BEGIN Add Message Cards.
        // Message cards are only used for onsite attendees.
        if (SettingsUtils.isAttendeeAtVenue(getContext())) {
            // Users are required to opt in or out of whether they want conference message cards.
            if (!ConfMessageCardUtils.hasAnsweredConfMessageCardsPrompt(getContext())) {
                // User has not answered whether they want to opt in.
                // Build a opt-in/out card.
                inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_MESSAGE_CARDS);
                MessageData conferenceMessageOptIn = MessageCardHelper
                        .getConferenceOptInMessageData(getContext());
                inventoryGroup.addItemWithTag(conferenceMessageOptIn);
                inventoryGroup.setDisplayCols(1);
                inventory.addGroup(inventoryGroup);
            } else if (ConfMessageCardUtils.isConfMessageCardsEnabled(getContext())) {
                ConfMessageCardUtils.enableActiveCards(getContext());

                // Note that for these special cards, we'll never show more than one at a time to
                // prevent overloading the user with messages. We want each new message to be
                // notable.
                if (shouldShowCard(ConfMessageCardUtils.ConfMessageCard.CONFERENCE_CREDENTIALS)) {
                    inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_MESSAGE_CARDS);
                    MessageData conferenceMessageOptIn = MessageCardHelper
                            .getConferenceCredentialsMessageData(getContext());
                    inventoryGroup.addItemWithTag(conferenceMessageOptIn);
                    inventoryGroup.setDisplayCols(1);
                    inventory.addGroup(inventoryGroup);
                } else if (shouldShowCard(ConfMessageCardUtils.ConfMessageCard.KEYNOTE_ACCESS)) {
                    inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_MESSAGE_CARDS);
                    MessageData conferenceMessageOptIn = MessageCardHelper
                            .getKeynoteAccessMessageData(getContext());
                    inventoryGroup.addItemWithTag(conferenceMessageOptIn);
                    inventoryGroup.setDisplayCols(1);
                    inventory.addGroup(inventoryGroup);
                } else if (shouldShowCard(ConfMessageCardUtils.ConfMessageCard.AFTER_HOURS)) {
                    inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_MESSAGE_CARDS);
                    MessageData conferenceMessageOptIn = MessageCardHelper
                            .getAfterHoursMessageData(getContext());
                    inventoryGroup.addItemWithTag(conferenceMessageOptIn);
                    inventoryGroup.setDisplayCols(1);
                    inventory.addGroup(inventoryGroup);
                } else if (shouldShowCard(ConfMessageCardUtils.ConfMessageCard.WIFI_FEEDBACK)) {
                    if (WiFiUtils.isWiFiEnabled(getContext()) &&
                            WiFiUtils.isWiFiApConfigured(getContext())) {
                        inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_MESSAGE_CARDS);
                        MessageData conferenceMessageOptIn = MessageCardHelper
                                .getWifiFeedbackMessageData(getContext());
                        inventoryGroup.addItemWithTag(conferenceMessageOptIn);
                        inventoryGroup.setDisplayCols(1);
                        inventory.addGroup(inventoryGroup);
                    }
                }
            }
            // Check whether a wifi setup card should be offered.
            if (WiFiUtils.shouldOfferToSetupWifi(getContext(), true)) {
                // Build card asking users whether they want to enable wifi.
                inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_MESSAGE_CARDS);
                MessageData conferenceMessageOptIn = MessageCardHelper
                        .getWifiSetupMessageData(getContext());
                inventoryGroup.addItemWithTag(conferenceMessageOptIn);
                inventoryGroup.setDisplayCols(1);
                inventory.addGroup(inventoryGroup);
            }
        }
        // END Add Message Cards.


        // Add Keynote card.
        SessionData keynoteData = model.getKeynoteData();
        if (keynoteData != null) {
            LOGD(TAG, "Keynote Live stream data found: " + model.getKeynoteData());
            inventoryGroup = new CollectionView.InventoryGroup
                    (GROUP_ID_KEYNOTE_STREAM_CARD);
            inventoryGroup.addItemWithTag(keynoteData);
            inventory.addGroup(inventoryGroup);
        }

        // Add Live Stream card.
        LiveStreamData liveStreamData = model.getLiveStreamData();
        if (liveStreamData != null && liveStreamData.getSessions().size() > 0) {
            LOGD(TAG, "Live session data found: " + liveStreamData);
            inventoryGroup = new CollectionView.InventoryGroup
                    (GROUP_ID_LIVE_STREAM_CARD);
            liveStreamData.setTitle(getResources().getString(R.string.live_now));
            inventoryGroup.addItemWithTag(liveStreamData);
            inventory.addGroup(inventoryGroup);
        }

        LOGD(TAG, "Inventory item count:" + inventory.getGroupCount() + " " + inventory
                .getTotalItemCount());

        ArrayList<CollectionView.InventoryGroup> themeGroups = new ArrayList<>();
        ArrayList<CollectionView.InventoryGroup> topicGroups = new ArrayList<>();

        for (TopicGroup topic : model.getTopics()) {
            LOGD(TAG, topic.getTitle() + ": " + topic.getSessions().size());
            if (topic.getSessions().size() > 0) {
                inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_TOPIC_CARDS);
                inventoryGroup.addItemWithTag(topic);
                topic.setTitle(getTranslatedTitle(topic.getTitle(), model));
                topicGroups.add(inventoryGroup);
            }
        }

        for (ThemeGroup theme : model.getThemes()) {
            LOGD(TAG, theme.getTitle() + ": " + theme.getSessions().size());
            if (theme.getSessions().size() > 0) {
                inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_THEME_CARDS);
                inventoryGroup.addItemWithTag(theme);
                theme.setTitle(getTranslatedTitle(theme.getTitle(), model));
                themeGroups.add(inventoryGroup);
            }
        }

        // We want to evenly disperse the topics between the themes. So we'll divide the topic count
        // by theme count to get the number of themes to display between topics.
        int topicsPerTheme = topicGroups.size();
        if (themeGroups.size() > 0) {
            topicsPerTheme = topicGroups.size() / themeGroups.size();
        }
        Iterator<CollectionView.InventoryGroup> themeIterator = themeGroups.iterator();
        int currentTopicNum = 0;
        for (CollectionView.InventoryGroup topicGroup : topicGroups) {
            inventory.addGroup(topicGroup);
            currentTopicNum++;
            if (currentTopicNum == topicsPerTheme) {
                if (themeIterator.hasNext()) {
                    inventory.addGroup(themeIterator.next());
                }
                currentTopicNum = 0;
            }
        }
        // Append any leftovers.
        while (themeIterator.hasNext()) {
            inventory.addGroup(themeIterator.next());
        }

        Parcelable state = mCollectionView.onSaveInstanceState();
        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inventory, false);
        if (state != null) {
            mCollectionView.onRestoreInstanceState(state);
        }

        // Show empty view if there were no Group cards.
        mEmptyView.setVisibility(inventory.getGroupCount() < 1 ? View.VISIBLE : View.GONE);
    }

    private String getTranslatedTitle(String title, ExploreModel model) {
        if (model.getTagTitles().get(title) != null) {
            return model.getTagTitles().get(title);
        } else {
            return title;
        }
    }

    @Override
    public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
        return LayoutInflater.from(context)
                .inflate(R.layout.explore_io_card_header_with_button, parent, false);
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, final int groupId,
                                         final String headerLabel, Object headerTag) {
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);

        // First inflate the card container.
        int containerLayoutId;
        switch (groupId) {
            case GROUP_ID_TOPIC_CARDS:
            case GROUP_ID_THEME_CARDS:
            case GROUP_ID_LIVE_STREAM_CARD:
                containerLayoutId = R.layout.explore_io_topic_theme_livestream_card_container;
                break;
            default:
                containerLayoutId = R.layout.explore_io_card_container;
                break;
        }
        ViewGroup containerView = (ViewGroup)inflater.inflate(containerLayoutId, parent, false);
        // Explicitly tell Accessibility to ignore the entire containerView since we add specific
        // individual content descriptions on child Views.
        UIUtils.setAccessibilityIgnore(containerView);

        ViewGroup containerContents = (ViewGroup)containerView.findViewById(
                R.id.explore_io_card_container_contents);

        // Now inflate the header within the container cards.
        int headerLayoutId = -1;
        switch (groupId) {
            case GROUP_ID_THEME_CARDS:
            case GROUP_ID_TOPIC_CARDS:
            case GROUP_ID_LIVE_STREAM_CARD:
                headerLayoutId = R.layout.explore_io_card_header_with_button;
                break;
        }
        // Inflate the specified number of items.
        if (headerLayoutId > -1) {
            inflater.inflate(headerLayoutId, containerContents, true);
        }

        // Now inflate the items within the container cards.
        int itemLayoutId = -1;
        int numItems = 1;
        switch (groupId) {
            case GROUP_ID_KEYNOTE_STREAM_CARD:
                itemLayoutId = R.layout.explore_io_keynote_stream_item;
                numItems = 1;
                break;
            case GROUP_ID_THEME_CARDS:
                itemLayoutId = R.layout.explore_io_topic_theme_livestream_item;
                numItems = ExploreModel.getThemeSessionLimit(getContext());
                break;
            case GROUP_ID_TOPIC_CARDS:
                itemLayoutId = R.layout.explore_io_topic_theme_livestream_item;
                numItems = ExploreModel.getTopicSessionLimit(getContext());
                break;
            case GROUP_ID_LIVE_STREAM_CARD:
                itemLayoutId = R.layout.explore_io_topic_theme_livestream_item;
                numItems = 3;
                break;
            case GROUP_ID_MESSAGE_CARDS:
                itemLayoutId = R.layout.explore_io_message_card_item;
                numItems = 1;
                break;
        }
        // Inflate the specified number of items.
        if (itemLayoutId > -1) {
            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
                inflater.inflate(itemLayoutId, containerContents, true);
            }
        }
        return containerView;
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId,
            int indexInGroup, int dataIndex, Object tag) {
        if (GROUP_ID_KEYNOTE_STREAM_CARD == groupId ||
                GROUP_ID_MESSAGE_CARDS == groupId) {
            // These two group id types don't have child views.
            populateSubItemInfo(context, view, groupId, tag);
            // Set the object's data into the view's tag so that the click listener on the view can
            // extract it and use the data to handle a click.
            View clickableView = view.findViewById(R.id.explore_io_clickable_item);
            if (clickableView != null) {
                clickableView.setTag(tag);
            }
        } else {
            // These group ids have children who are child items.
            ViewGroup viewWithChildrenSubItems = (ViewGroup)(view.findViewById(
                    R.id.explore_io_card_container_contents));
            ItemGroup itemGroup = (ItemGroup) tag;

            // Set Header tag and title.
            viewWithChildrenSubItems.getChildAt(0).setTag(tag);
            TextView titleTextView = ((TextView) view.findViewById(android.R.id.title));
            View headerView = view.findViewById(R.id.explore_io_card_header_layout);
            if (headerView != null) {
                headerView.setContentDescription(
                        getString(R.string.more_items_button_desc_with_label_a11y,
                                itemGroup.getTitle()));
            }

            // Set the tag on the moreButton so it can be accessed by the click listener.
            View moreButton = view.findViewById(android.R.id.button1);
            if (moreButton != null) {
                moreButton.setTag(tag);
            }
            if (titleTextView != null) {
                titleTextView.setText(itemGroup.getTitle());
            }

            // Skipping first child b/c it is a header view.
            for (int viewChildIndex = 1; viewChildIndex < viewWithChildrenSubItems.getChildCount(); viewChildIndex++) {
                View childView = viewWithChildrenSubItems.getChildAt(viewChildIndex);

                int sessionIndex = viewChildIndex - 1;
                int sessionSize = itemGroup.getSessions().size();
                if (childView != null && sessionIndex < sessionSize) {
                    childView.setVisibility(View.VISIBLE);
                    SessionData sessionData = itemGroup.getSessions().get(sessionIndex);
                    childView.setTag(sessionData);
                    populateSubItemInfo(context, childView, groupId, sessionData);
                } else if (childView != null) {
                    childView.setVisibility(View.GONE);
                }
            }
        }

    }

    private void populateSubItemInfo(Context context, View view, int groupId, Object tag) {
        // Locate the views that may be used to configure the item being bound to this view.
        // Not all elements are used in all views so some will be null.
        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView descriptionView = (TextView) view.findViewById(R.id.description);
        Button startButton = (Button) view.findViewById(R.id.buttonStart);
        Button endButton = (Button) view.findViewById(R.id.buttonEnd);
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);

        // Load item elements common to THEME and TOPIC group cards.
        if (tag instanceof SessionData) {
            SessionData sessionData = (SessionData)tag;
            titleView.setText(sessionData.getSessionName());
            if (!TextUtils.isEmpty(sessionData.getImageUrl())) {
                ImageView imageView = (ImageView) view.findViewById(R.id.thumbnail);
                mImageLoader.loadImage(sessionData.getImageUrl(), imageView);
            }
            ImageView inScheduleIndicator =
                    (ImageView) view.findViewById(R.id.indicator_in_schedule);
            if (inScheduleIndicator != null) {  // check not keynote
                inScheduleIndicator.setVisibility(
                        sessionData.isInSchedule() ? View.VISIBLE : View.GONE);
            }
            if (!TextUtils.isEmpty(sessionData.getDetails())) {
                descriptionView.setText(sessionData.getDetails());
            }
        }

        // Bind message data if this item is meant to be bound as a message card.
        if (GROUP_ID_MESSAGE_CARDS == groupId) {
            MessageData messageData = (MessageData)tag;
            descriptionView.setText(messageData.getMessageString(context));
            if (messageData.getEndButtonStringResourceId() != -1) {
                endButton.setText(messageData.getEndButtonStringResourceId());
            } else {
                endButton.setVisibility(View.GONE);
            }
            if (messageData.getStartButtonStringResourceId() != -1) {
                startButton.setText(messageData.getStartButtonStringResourceId());
            } else {
                startButton.setVisibility(View.GONE);
            }
            if (messageData.getIconDrawableId() > 0) {
                iconView.setVisibility(View.VISIBLE);
                iconView.setImageResource(messageData.getIconDrawableId());
            } else {
                iconView.setVisibility(View.GONE);
            }
            if (messageData.getStartButtonClickListener() != null) {
                startButton.setOnClickListener(messageData.getStartButtonClickListener());
            }
            if (messageData.getEndButtonClickListener() != null) {
                endButton.setOnClickListener(messageData.getEndButtonClickListener());
            }
        }
    }

    /**
     * Let all UserActionListener know that the video list has been reloaded and that therefore we
     * need to display another random set of sessions.
     */
    private void fireReloadEvent() {
        if (!isAdded()) {
            return;
        }
        for (UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putInt(PresenterFragmentImpl.KEY_RUN_QUERY_ID,
                    ExploreModel.ExploreQueryEnum.SESSIONS.getId());
            h1.onUserAction(ExploreModel.ExploreUserActionEnum.RELOAD, args);
        }
    }

    private void fireReloadTagsEvent() {
        if (!isAdded()) {
            return;
        }
        for (UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putInt(PresenterFragmentImpl.KEY_RUN_QUERY_ID,
                    ExploreModel.ExploreQueryEnum.TAGS.getId());
            h1.onUserAction(ExploreModel.ExploreUserActionEnum.RELOAD, args);
        }
    }

    @Override
    public Uri getDataUri(QueryEnum query) {
        if (query == ExploreModel.ExploreQueryEnum.SESSIONS) {
            return ScheduleContract.Sessions.CONTENT_URI;
        }
        return Uri.EMPTY;
    }

    private boolean shouldShowCard(ConfMessageCardUtils.ConfMessageCard card) {

        boolean shouldShow = ConfMessageCardUtils.shouldShowConfMessageCard(getContext(), card);
        boolean hasDismissed = ConfMessageCardUtils.hasDismissedConfMessageCard(getContext(),
                card);
        return  (shouldShow && !hasDismissed);
    }
}
