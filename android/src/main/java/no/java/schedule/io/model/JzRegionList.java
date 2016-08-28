package no.java.schedule.io.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class JzRegionList {

    @SerializedName("region")
    @Expose
    private List<JzBeaconRegion> region = new ArrayList<JzBeaconRegion>();

    public List<JzBeaconRegion> getRegion() {
        return region;
    }

    public void setRegion1(List<JzBeaconRegion> region) {
        this.region = region;
    }
}
