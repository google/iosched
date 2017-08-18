/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.info.travel;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.info.BaseInfoFragment;
import com.google.samples.apps.iosched.info.CollapsibleCard;
import com.google.samples.apps.iosched.lib.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class TravelFragment extends BaseInfoFragment<TravelInfo> {
    private static final String TAG = makeLogTag(TravelFragment.class);

    private TravelInfo mTravelInfo;

    private CollapsibleCard bikingCard;
    private CollapsibleCard shuttleServiceCard;
    private CollapsibleCard carpoolingParkingCard;
    private CollapsibleCard publicTransportationCard;
    private CollapsibleCard rideSharingCard;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_travel_frag, container, false);
        bikingCard = (CollapsibleCard) root.findViewById(R.id.bikingCard);
        shuttleServiceCard = (CollapsibleCard) root.findViewById(R.id.shuttleInfoCard);
        carpoolingParkingCard = (CollapsibleCard) root.findViewById(R.id.carpoolingParkingCard);
        publicTransportationCard =
                (CollapsibleCard) root.findViewById(R.id.publicTransportationCard);
        rideSharingCard = (CollapsibleCard) root.findViewById(R.id.rideSharingCard);
        return root;
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(R.string.title_travel);
    }

    @Override
    public void updateInfo(TravelInfo info) {
        mTravelInfo = info;
    }

    @Override
    protected void showInfo() {
        if (mTravelInfo != null) {
            bikingCard.setCardDescription(mTravelInfo.getBikingInfo());
            shuttleServiceCard.setCardDescription(mTravelInfo.getShuttleInfo());
            carpoolingParkingCard.setCardDescription(mTravelInfo.getCarpoolingParkingInfo());
            publicTransportationCard.setCardDescription(mTravelInfo.getPublicTransportationInfo());
            rideSharingCard.setCardDescription(mTravelInfo.getRideSharingInfo());
        } else {
            LOGE(TAG, "TravelInfo should not be null.");
        }
    }
}
