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
    private CharSequence shuttleInfo;
    private CharSequence publicTransportationInfo;
    private CharSequence carpoolingParkingInfo;
    private CharSequence bikingInfo;

    public CharSequence getShuttleInfo() {
        return shuttleInfo;
    }

    public void setShuttleInfo(CharSequence shuttleInfo) {
        this.shuttleInfo = shuttleInfo;
    }

    public CharSequence getPublicTransportationInfo() {
        return publicTransportationInfo;
    }

    public void setPublicTransportationInfo(CharSequence publicTransportationInfo) {
        this.publicTransportationInfo = publicTransportationInfo;
    }

    public CharSequence getCarpoolingParkingInfo() {
        return carpoolingParkingInfo;
    }

    public void setCarpoolingParkingInfo(CharSequence carpoolingParkingInfo) {
        this.carpoolingParkingInfo = carpoolingParkingInfo;
    }

    public CharSequence getBikingInfo() {
        return bikingInfo;
    }

    public void setBikingInfo(CharSequence bikingInfo) {
        this.bikingInfo = bikingInfo;
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
