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
package com.google.samples.apps.iosched.info;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.samples.apps.iosched.info.about.AboutInfo;
import com.google.samples.apps.iosched.info.event.EventInfo;
import com.google.samples.apps.iosched.info.travel.TravelInfo;
import com.google.samples.apps.iosched.lib.R;

import static com.google.samples.apps.iosched.util.FirebaseRemoteConfigUtil.getRemoteConfigSequence;


public class InfoPresenter implements InfoContract.Presenter {

    private InfoContract.View mView;
    private Context mContext;

    public InfoPresenter(Context context, InfoContract.View view) {
        mView = view;
        mContext = context;
    }

    public void initEventInfo() {
        final EventInfo eventInfo = new EventInfo();
        applyRemoteConfigToEventInfo(eventInfo);
        mView.showEventInfo(eventInfo);
        FirebaseRemoteConfig.getInstance().fetch(1800L).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseRemoteConfig.getInstance().activateFetched();
                        applyRemoteConfigToEventInfo(eventInfo);
                        mView.showEventInfo(eventInfo);
                    }
                });
    }

    private void applyRemoteConfigToEventInfo(EventInfo eventInfo) {
        String wiFiNetwork = FirebaseRemoteConfig.getInstance()
                .getString(mContext.getString(R.string.default_wifi_network_key));
        String wiFiPassword = FirebaseRemoteConfig.getInstance()
                .getString(mContext.getString(R.string.default_wifi_password_key));
        String sandboxDescription = getRemoteConfigSequence(
                mContext.getString(R.string.event_sandbox_description_key));
        String codeLabsDescription = getRemoteConfigSequence(
                mContext.getString(R.string.event_code_labs_description_key));
        String officeHoursDescription = getRemoteConfigSequence(
                mContext.getString(R.string.event_office_hours_description_key));
        String afterHoursDescription = getRemoteConfigSequence(
                mContext.getString(R.string.event_after_hours_description_key));
        eventInfo.setWiFiNetwork(wiFiNetwork);
        eventInfo.setWiFiPassword(wiFiPassword);
        eventInfo.setSandboxDescription(sandboxDescription);
        eventInfo.setCodeLabsDescription(codeLabsDescription);
        eventInfo.setOfficeHoursDescription(officeHoursDescription);
        eventInfo.setAfterHoursDescription(afterHoursDescription);
    }

    @Override
    public void initTravelInfo() {
        final TravelInfo travelInfo = new TravelInfo();
        applyRemoteConfigToTravelInfo(travelInfo);
        mView.showTravelInfo(travelInfo);
        FirebaseRemoteConfig.getInstance().fetch(1800L).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseRemoteConfig.getInstance().activateFetched();
                        applyRemoteConfigToTravelInfo(travelInfo);
                        mView.showTravelInfo(travelInfo);
                    }
                }
        );
    }

    private void applyRemoteConfigToTravelInfo(TravelInfo travelInfo) {
        String travelShuttleService = getRemoteConfigSequence(
                mContext.getString(R.string.travel_shuttle_service_description_key));
        String travelCarpoolingParking = getRemoteConfigSequence(
                mContext.getString(R.string.travel_carpooling_parking_description_key));
        String travelPublicTransportation = getRemoteConfigSequence(
                mContext.getString(R.string.travel_public_transportation_description_key));
        String travelBiking = getRemoteConfigSequence(
                mContext.getString(R.string.travel_biking_description_key));
        String travelRideSharing = getRemoteConfigSequence(
                mContext.getString(R.string.travel_ride_sharing_description_key));
        travelInfo.setShuttleInfo(travelShuttleService);
        travelInfo.setCarpoolingParkingInfo(travelCarpoolingParking);
        travelInfo.setPublicTransportationInfo(travelPublicTransportation);
        travelInfo.setBikingInfo(travelBiking);
        travelInfo.setRideSharingInfo(travelRideSharing);
    }

    @Override
    public void initAboutInfo() {
        final AboutInfo aboutInfo = new AboutInfo();
        applyRemoteConfigToAboutInfo(aboutInfo);
        mView.showAboutInfo(aboutInfo);
        FirebaseRemoteConfig.getInstance().fetch(1800L).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseRemoteConfig.getInstance().activateFetched();
                        applyRemoteConfigToAboutInfo(aboutInfo);
                        mView.showAboutInfo(aboutInfo);
                    }
                }
        );
    }

    private void applyRemoteConfigToAboutInfo(AboutInfo aboutInfo) {
        String stayInformedDescription = getRemoteConfigSequence(
                mContext.getString(R.string.faq_stay_informed_description_key));
        aboutInfo.setStayInformedDescription(stayInformedDescription);

        String contentFormatsDescription = getRemoteConfigSequence(
                mContext.getString(R.string.faq_content_formats_description_key));
        aboutInfo.setContentFormatsDescription(contentFormatsDescription);

        String liveStreamRecordingsDescription = getRemoteConfigSequence(
                mContext.getString(R.string.faq_livestream_and_recordings_description_key));
        aboutInfo.setLiveStreamRecordingsDescription(liveStreamRecordingsDescription);

        String attendanceProTipsDescription = getRemoteConfigSequence(
                mContext.getString(R.string.faq_attendance_pro_tips_description_key));
        aboutInfo.setAttendanceProTipsDescription(attendanceProTipsDescription);

        String moreDescription = getRemoteConfigSequence(
                mContext.getString(R.string.faq_additional_info_description_key));
        aboutInfo.setAdditionalInfoDescription(moreDescription);
    }
}