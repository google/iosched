package no.java.schedule.io.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class JzRegionList {
    @SerializedName("UUID")
    @Expose
    private String UUID;

    @SerializedName("regions")
    private ArrayList<JzBeaconRegion> regions;

    public ArrayList<JzBeaconRegion> getRegions() {
        return regions;
    }

    public void setRegions(ArrayList<JzBeaconRegion> regions) {
        this.regions = regions;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
