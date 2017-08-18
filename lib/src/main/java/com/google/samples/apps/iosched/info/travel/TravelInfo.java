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
package com.google.samples.apps.iosched.info.travel;

public class TravelInfo {
    private String shuttleInfo;
    private String publicTransportationInfo;
    private String carpoolingParkingInfo;
    private String bikingInfo;
    private String rideSharingInfo;

    public String getShuttleInfo() {
        return shuttleInfo;
    }

    public void setShuttleInfo(String shuttleInfo) {
        this.shuttleInfo = shuttleInfo;
    }

    public String getPublicTransportationInfo() {
        return publicTransportationInfo;
    }

    public void setPublicTransportationInfo(String publicTransportationInfo) {
        this.publicTransportationInfo = publicTransportationInfo;
    }

    public String getCarpoolingParkingInfo() {
        return carpoolingParkingInfo;
    }

    public void setCarpoolingParkingInfo(String carpoolingParkingInfo) {
        this.carpoolingParkingInfo = carpoolingParkingInfo;
    }

    public String getBikingInfo() {
        return bikingInfo;
    }

    public void setBikingInfo(String bikingInfo) {
        this.bikingInfo = bikingInfo;
    }

    public String getRideSharingInfo() {
        return rideSharingInfo;
    }

    public void setRideSharingInfo(String rideSharingInfo) {
        this.rideSharingInfo = rideSharingInfo;
    }

    @Override
    public String toString() {
        return "TravelInfo{" +
                "shuttleInfo='" + shuttleInfo + '\'' +
                ", publicTransportationInfo='" + publicTransportationInfo + '\'' +
                ", carpoolingParkingInfo='" + carpoolingParkingInfo + '\'' +
                ", bikingInfo='" + bikingInfo + '\'' +
                '}';
    }
}
