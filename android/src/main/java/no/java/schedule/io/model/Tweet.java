package no.java.schedule.io.model;

import android.net.Uri;

import java.util.Date;

public class Tweet implements Comparable<Tweet>  {

    private long id;
    private Date createdAt;
    private String user;
    private String userName;
    private String text;
    private Uri profileImageUri;

    public Tweet(){}

    public Tweet(long id, Date createdAt, String user, String userName, String text, Uri profileImageUri) {
        this.id = id;
        this.createdAt = createdAt;
        this.user = user;
        this.userName = userName;
        this.text = text;
        this.profileImageUri = profileImageUri;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Uri getProfileImageUri() {
        return profileImageUri;
    }

    public void setProfileImageUri(Uri profileImageUri) {
        this.profileImageUri = profileImageUri;
    }

    @Override
    public int compareTo(Tweet otherTweet) {
        return otherTweet.getCreatedAt().compareTo(getCreatedAt());
    }
}
