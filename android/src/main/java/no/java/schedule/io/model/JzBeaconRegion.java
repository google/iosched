package no.java.schedule.io.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class JzBeaconRegion {

    @SerializedName("beacons")
    @Expose
    private List<JzBeacon> beacons = new ArrayList<JzBeacon>();

    public List<JzBeacon> getBeacons() {
        return beacons;
    }

    public void setBeacons(List<JzBeacon> beacons) {
        this.beacons = beacons;
    }
}
