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
package com.google.samples.apps.iosched.session;

import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.archframework.UpdatableView;

public class SessionDetailPresenter extends PresenterImpl<SessionDetailModel,
        SessionDetailModel.SessionDetailQueryEnum,
        SessionDetailModel.SessionDetailUserActionEnum> {

    public SessionDetailPresenter(SessionDetailModel model,
            UpdatableView<SessionDetailModel, SessionDetailModel.SessionDetailQueryEnum,
                    SessionDetailModel.SessionDetailUserActionEnum> view,
            SessionDetailModel.SessionDetailUserActionEnum[] validUserActions,
            SessionDetailModel.SessionDetailQueryEnum[] initialQueries) {
        super(model, view, validUserActions, initialQueries);
    }

    public SessionDetailPresenter(SessionDetailModel model,
            @Nullable UpdatableView<SessionDetailModel, SessionDetailModel.SessionDetailQueryEnum,
                    SessionDetailModel.SessionDetailUserActionEnum>[] views,
            SessionDetailModel.SessionDetailUserActionEnum[] validUserActions,
            SessionDetailModel.SessionDetailQueryEnum[] initialQueries) {
        super(model, views, validUserActions, initialQueries);
    }

    public void initListeners() {
        getModel().initReservationListeners();
    }

    public void cleanUpListeners() {
        getModel().cleanUp();
    }
}
