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
import android.text.Spannable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.samples.apps.iosched.info.event.EventInfo;
import com.google.samples.apps.iosched.info.faq.FaqInfo;
import com.google.samples.apps.iosched.info.travel.TravelInfo;
import com.google.samples.apps.iosched.lib.R;

import static com.google.samples.apps.iosched.util.FirebaseRemoteConfigUtil.getRemoteConfigSpannable;
import static com.google.samples.apps.iosched.util.FirebaseRemoteConfigUtil.stripUnderlines;

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
        Spannable sandboxDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.event_sandbox_description_key));
        stripUnderlines(sandboxDescription);
        Spannable codeLabsDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.event_code_labs_description_key));
        stripUnderlines(codeLabsDescription);
        Spannable officeHoursDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.event_office_hours_description_key));
        stripUnderlines(officeHoursDescription);
        Spannable afterHoursDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.event_after_hours_description_key));
        stripUnderlines(afterHoursDescription);
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
        Spannable travelShuttleService = getRemoteConfigSpannable(
                mContext.getString(R.string.travel_shuttle_service_description_key));
        Spannable travelCarpoolingParking = getRemoteConfigSpannable(
                mContext.getString(R.string.travel_carpooling_parking_description_key));
        Spannable travelPublicTransportation = getRemoteConfigSpannable(
                mContext.getString(R.string.travel_public_transportation_description_key));
        Spannable travelBiking = getRemoteConfigSpannable(
                mContext.getString(R.string.travel_biking_description_key));
        travelInfo.setShuttleInfo(travelShuttleService);
        travelInfo.setCarpoolingParkingInfo(travelCarpoolingParking);
        travelInfo.setPublicTransportationInfo(travelPublicTransportation);
        travelInfo.setBikingInfo(travelBiking);
    }

    @Override
    public void initFaqInfo() {
        final FaqInfo faqInfo = new FaqInfo();
        applyRemoteConfigToFaqInfo(faqInfo);
        mView.showFaqInfo(faqInfo);
        FirebaseRemoteConfig.getInstance().fetch(1800L).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseRemoteConfig.getInstance().activateFetched();
                        applyRemoteConfigToFaqInfo(faqInfo);
                        mView.showFaqInfo(faqInfo);
                    }
                }
        );
    }

    private void applyRemoteConfigToFaqInfo(FaqInfo faqInfo) {
        Spannable stayInformedDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.faq_stay_informed_description_key));
        stripUnderlines(stayInformedDescription);
        faqInfo.setStayInformedDescription(stayInformedDescription);
        Spannable contentFormatsDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.faq_content_formats_description_key));
        stripUnderlines(contentFormatsDescription);
        faqInfo.setContentFormatsDescription(contentFormatsDescription);

        Spannable liveStreamRecordingsDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.faq_livestream_and_recordings_description_key));
        stripUnderlines(liveStreamRecordingsDescription);
        faqInfo.setLiveStreamRecordingsDescription(liveStreamRecordingsDescription);

        Spannable attendanceProTipsDescription = getRemoteConfigSpannable(
                mContext.getString(R.string.faq_attendance_pro_tips_description_key));
        stripUnderlines(attendanceProTipsDescription);
        faqInfo.setAttendanceProTipsDescription(attendanceProTipsDescription);
    }
}