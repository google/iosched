package com.google.samples.apps.iosched.explore.data;

import java.util.ArrayList;

public class ItemGroup {
    private String mTitle;
    private String mId;
    private ArrayList<SessionData> sessions = new ArrayList<SessionData>();

    public void addSessionData(SessionData session) {
        sessions.add(session);
    }

    /**
     * Trim the session data to {@code sessionLimit} using the
     * {@code random Random Number Generator}.
     */
    public void trimSessionData(int sessionLimit) {
        while (sessions.size() > sessionLimit) {
            sessions.remove(0);
        }
    }

    public String getTitle() { return mTitle; }
    public void setTitle(String title) { mTitle = title; }
    public String getId() { return mId; }
    public void setId(String id) { mId = id; }
    public ArrayList<SessionData> getSessions() { return sessions; }
}
