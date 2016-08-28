package no.java.schedule.io.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class JzBeacon {

    @SerializedName("UUID")
    @Expose
    private String uUID;
    @SerializedName("major")
    @Expose
    private Integer major;
    @SerializedName("minor")
    @Expose
    private Integer minor;
    public String getUUID() {
        return uUID;
    }

    public void setUUID(String uUID) {
        this.uUID = uUID;
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }
}
