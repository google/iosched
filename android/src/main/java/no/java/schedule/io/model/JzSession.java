package no.java.schedule.io.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JzSession {
    @SerializedName("tittel")
    public String title;
    public String format;
    @SerializedName("starter")
    public Date startSession;
    @SerializedName("stopper")
    public Date endSession;
    @SerializedName("foredragsholdere")
    public ArrayList<JzSpeaker> speakers;
    @SerializedName("sprak")
    public String language;
    @SerializedName("level")
    public String level;
    @SerializedName("links")
    public ArrayList<JzLink> links;
    @SerializedName("rom")
    public JzRoom room;
    @SerializedName("nokkelord")
    public List<String> keyWords;
}
