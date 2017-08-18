/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.StringRes;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;

/**
 * A custom 'button' modeling the states a session reservation can be in.
 */
public class ReserveButton extends FrameLayout {

    private TextView text;
    private ReservationStatus status;

    public ReserveButton(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.reserveButtonStyle);
        LayoutInflater.from(context).inflate(R.layout.reserve_button, this, true);
        text = (TextView) findViewById(R.id.reserve_text);
        setStatus(ReservationStatus.RESERVABLE);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        if (this.status == status) {
            return;
        }
        this.status = status;
        refreshDrawableState();
        text.setText(status.text);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (status == null) return super.onCreateDrawableState(extraSpace);
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        mergeDrawableStates(drawableState, status.state);
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        // theme attrs only supported in ColorStateLists on M+, so do it manually.
        text.setTextColor(
                AppCompatResources.getColorStateList(getContext(), R.color.reserve_text));
        setBackgroundColor(AppCompatResources.getColorStateList(
                getContext(), R.color.reserve_background)
                .getColorForState(getDrawableState(), Color.TRANSPARENT));
    }

    /**
     * An enum representing the states that reservation can be and their associated display.
     */
    public enum ReservationStatus {

        RESERVABLE(new int[]{R.attr.state_reservable}, R.string.my_schedule_reserve_seat),
        RESERVATION_PENDING(new int[]{R.attr.state_reservation_pending}, R.string.my_schedule_request_pending),
        RESERVED(new int[]{R.attr.state_reserved}, R.string.my_schedule_already_reserved),
        RESERVATION_DISABLED(new int[]{R.attr.state_reservation_disabled}, R.string.my_schedule_reserve_disabled),
        AUTH_REQUIRED(new int[]{R.attr.state_auth_required}, R.string.my_schedule_reservation_disabled_auth_button_text),
        WAITLIST_AVAILABLE(new int[]{R.attr.state_waitlist_available}, R.string.my_schedule_join_waitlist),
        WAITLISTED(new int[]{R.attr.state_waitlisted}, R.string.my_schedule_waitlisted);

        final int[] state;
        final @StringRes int text;

        ReservationStatus(int[] state, @StringRes int text) {
            this.state = state;
            this.text = text;
        }
    }
}
