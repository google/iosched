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
    private String wiFiNetwork;
    private String wiFiPassword;
    private CharSequence sandboxDescription;
    private CharSequence codeLabsDescription;
    private CharSequence officeHoursDescription;
    private CharSequence afterHoursDescription;

    public String getWiFiNetwork() {
        return wiFiNetwork;
    }

    public void setWiFiNetwork(String wiFiNetwork) {
        this.wiFiNetwork = wiFiNetwork;
    }

    public String getWiFiPassword() {
        return wiFiPassword;
    }

    public void setWiFiPassword(String wiFiPassword) {
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

    @Override
    public String toString() {
        return "EventInfo{" +
                "wiFiNetwork='" + wiFiNetwork + '\'' +
                ", wiFiPassword='" + wiFiPassword + '\'' +
                ", sandboxDescription=" + sandboxDescription +
                ", codeLabsDescription=" + codeLabsDescription +
                ", officeHoursDescription=" + officeHoursDescription +
                ", afterHoursDescription=" + afterHoursDescription +
                '}';
    }
}
