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

public class EventInfo {
    private CharSequence wiFiNetwork;
    private CharSequence wiFiPassword;
    private CharSequence sandboxDescription;
    private CharSequence codeLabsDescription;
    private CharSequence officeHoursDescription;
    private CharSequence afterHoursDescription;

    public CharSequence getWiFiNetwork() {
        return wiFiNetwork;
    }

    public void setWiFiNetwork(CharSequence wiFiNetwork) {
        this.wiFiNetwork = wiFiNetwork;
    }

    public CharSequence getWiFiPassword() {
        return wiFiPassword;
    }

    public void setWiFiPassword(CharSequence wiFiPassword) {
        this.wiFiPassword = wiFiPassword;
    }

    public CharSequence getSandboxDescription() {
        return sandboxDescription;
    }

    public void setSandboxDescription(CharSequence sandboxDescription) {
        this.sandboxDescription = sandboxDescription;
    }

    public CharSequence getCodeLabsDescription() {
        return codeLabsDescription;
    }

    public void setCodeLabsDescription(CharSequence codeLabsDescription) {
        this.codeLabsDescription = codeLabsDescription;
    }

    public CharSequence getOfficeHoursDescription() {
        return officeHoursDescription;
    }

    public void setOfficeHoursDescription(CharSequence officeHoursDescription) {
        this.officeHoursDescription = officeHoursDescription;
    }

    public CharSequence getAfterHoursDescription() {
        return afterHoursDescription;
    }

    public void setAfterHoursDescription(CharSequence afterHoursDescription) {
        this.afterHoursDescription = afterHoursDescription;
    }
}
