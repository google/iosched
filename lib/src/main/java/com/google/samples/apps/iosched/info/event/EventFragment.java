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
package com.google.samples.apps.iosched.info.event;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.info.BaseInfoFragment;
import com.google.samples.apps.iosched.lib.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class EventFragment extends BaseInfoFragment<EventInfo> {
    private static final String TAG = makeLogTag(EventFragment.class);

    private EventInfo mEventInfo;

    private TextView mWiFiNetworkText;
    private TextView mWiFiPasswordText;
    private EventContentView mSandboxEventContent;
    private EventContentView mCodeLabsEventContent;
    private EventContentView mOfficeHoursEventContent;
    private EventContentView mAfterHoursEventContent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_event_frag, container, false);
        mWiFiNetworkText = (TextView) root.findViewById(R.id.wiFiNetworkValue);
        mWiFiPasswordText = (TextView) root.findViewById(R.id.wiFiPasswordValue);
        mSandboxEventContent = (EventContentView) root.findViewById(R.id.sandboxEventContent);
        mCodeLabsEventContent = (EventContentView) root.findViewById(R.id.codeLabsEventContent);
        mOfficeHoursEventContent =
                (EventContentView) root.findViewById(R.id.officeHoursEventContent);
        mAfterHoursEventContent =
                (EventContentView) root.findViewById(R.id.afterHoursEventContent);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        mWiFiPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) getActivity()
                        .getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData passwordClip = ClipData
                        .newPlainText("password", mEventInfo.getWiFiPassword());
                clipboardManager.setPrimaryClip(passwordClip);
            }
        });
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(R.string.title_event);
    }

    @Override
    public void updateInfo(EventInfo info) {
        mEventInfo = info;
    }

    @Override
    protected void showInfo() {
        if (mEventInfo != null) {
            mWiFiNetworkText.setText(mEventInfo.getWiFiNetwork());
            mWiFiPasswordText.setText(mEventInfo.getWiFiPassword());
            mSandboxEventContent.setEventDescription(mEventInfo.getSandboxDescription());
            mCodeLabsEventContent.setEventDescription(mEventInfo.getCodeLabsDescription());
            mOfficeHoursEventContent.setEventDescription(mEventInfo.getOfficeHoursDescription());
            mAfterHoursEventContent.setEventDescription(mEventInfo.getAfterHoursDescription());
        } else {
            LOGE(TAG, "EventInfo should not be null.");
        }
    }
}
