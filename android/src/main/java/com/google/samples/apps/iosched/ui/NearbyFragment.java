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

package com.google.samples.apps.iosched.ui;

import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.nearby.NearbyDevice;
import com.google.samples.apps.iosched.nearby.NearbyDeviceManager;

/**
 * ListFragment for displaying BLE devices.
 */
public class NearbyFragment extends ListFragment {
    public static String ARG_HAS_HEADER = "hasHeader";

    public static NearbyFragment newInstance(boolean hasHeader) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HAS_HEADER, hasHeader);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        if (getArguments() != null && !getArguments().getBoolean(ARG_HAS_HEADER, true)) {
            rootView.findViewById(R.id.headerbar).setVisibility(View.GONE);
        }

        Callbacks parentActivity = (Callbacks) getActivity();
        setListAdapter(parentActivity.getNearbyDeviceManager().getAdapter());
        rootView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: refactor to use fragment callbacks
                getFragmentManager().popBackStack();
            }
        });
        return rootView;
    }

    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        NearbyDevice device = (NearbyDevice) parent.getAdapter().getItem(position);
        String url = device.getUrl();
        if (url != null) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } else {
            Toast.makeText(getActivity(), "No URL found.", Toast.LENGTH_SHORT).show();
        }
    }

    public interface Callbacks {
        public NearbyDeviceManager getNearbyDeviceManager();
    }
}
