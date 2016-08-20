package no.java.schedule.io.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JzSession {
    public String title;
    public String format;
    public Date startSession;
    public Date endSession;
    public ArrayList<JzSpeaker> speakers;
    public String language;
    public String level;
    public ArrayList<JzLink> links;
    public JzRoom room;
    public List<String> keyWords;
}
