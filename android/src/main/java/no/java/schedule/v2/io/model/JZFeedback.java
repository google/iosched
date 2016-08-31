package no.java.schedule.v2.io.model;

import java.io.Serializable;

public class JZFeedback implements Serializable {
    private int overall;
    private int relevance;
    private int content;
    private int quality;
    private String comment;

    public JZFeedback(int overall, int relevance, int content, int quality, String comment) {
        this.overall = overall;
        this.relevance = relevance;
        this.content = content;
        this.quality = quality;
        this.comment = comment;
    }

    public int getOverall() {
        return overall;
    }

    public void setOverall(int overall) {
        this.overall = overall;
    }

    public int getRelevance() {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }

    public int getContent() {
        return content;
    }

    public void setContent(int content) {
        this.content = content;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public String getComment() { return comment; }

    public void setComment(String f) {
        this.comment = f;
    }
}
