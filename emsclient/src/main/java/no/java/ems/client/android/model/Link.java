package no.java.ems.client.android.model;

import java.net.URI;

public class Link {
    public URI href;
    public String rel;

    @Override
    public String toString() {
        return "Link{" +
                "href=" + href +
                ", rel='" + rel + '\'' +
                '}';
    }
}
