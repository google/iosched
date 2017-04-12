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
    private String sandboxDescription;
    private String codeLabsDescription;
    private String officeHoursDescription;
    private String afterHoursDescription;

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

    public String getSandboxDescription() {
        return sandboxDescription;
    }

    public void setSandboxDescription(String sandboxDescription) {
        this.sandboxDescription = sandboxDescription;
    }

    public String getCodeLabsDescription() {
        return codeLabsDescription;
    }

    public void setCodeLabsDescription(String codeLabsDescription) {
        this.codeLabsDescription = codeLabsDescription;
    }

    public String getOfficeHoursDescription() {
        return officeHoursDescription;
    }

    public void setOfficeHoursDescription(String officeHoursDescription) {
        this.officeHoursDescription = officeHoursDescription;
    }

    public String getAfterHoursDescription() {
        return afterHoursDescription;
    }

    public void setAfterHoursDescription(String afterHoursDescription) {
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
