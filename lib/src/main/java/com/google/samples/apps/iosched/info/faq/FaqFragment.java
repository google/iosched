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
package com.google.samples.apps.iosched.info.faq;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.info.BaseInfoFragment;
import com.google.samples.apps.iosched.info.CollapsableCard;
import com.google.samples.apps.iosched.lib.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class FaqFragment extends BaseInfoFragment<FaqInfo> {
    private static final String TAG = makeLogTag(FaqFragment.class);

    private FaqInfo mFaqInfo;

    private CollapsableCard mStayInformedCard;
    private CollapsableCard mContentFormatsCard;
    private CollapsableCard mLiveStreamsRecordingsCard;
    private CollapsableCard mAttendanceProTipsCard;
    private CollapsableCard mAdditionalInfoCard;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_faq_frag, container, false);
        mStayInformedCard = (CollapsableCard) root.findViewById(R.id.stayInformedCard);
        mContentFormatsCard = (CollapsableCard) root.findViewById(R.id.contentFormatsCard);
        mLiveStreamsRecordingsCard = (CollapsableCard) root.findViewById(
                R.id.liveStreamRecordingsCard);
        mAttendanceProTipsCard = (CollapsableCard) root.findViewById(R.id.attendanceProTipsCard);
        mAdditionalInfoCard = (CollapsableCard) root.findViewById(R.id.additionalInfo);
        return root;
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(R.string.title_faq);
    }

    @Override
    public void updateInfo(FaqInfo info) {
        mFaqInfo = info;
    }

    @Override
    protected void showInfo() {
        if (mFaqInfo != null) {
            mStayInformedCard.setCardDescription(mFaqInfo.getStayInformedDescription());
            mContentFormatsCard.setCardDescription(mFaqInfo.getContentFormatsDescription());
            mLiveStreamsRecordingsCard.setCardDescription(
                    mFaqInfo.getLiveStreamRecordingsDescription());
            mAttendanceProTipsCard.setCardDescription(mFaqInfo.getAttendanceProTipsDescription());
            mAdditionalInfoCard.setCardDescription(mFaqInfo.getAdditionalInfoDescription());
        } else {
            LOGE(TAG, "FaqInfo should not be null.");
        }
    }
}
