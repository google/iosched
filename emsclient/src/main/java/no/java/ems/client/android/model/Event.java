package no.java.ems.client.android.model;

import java.net.URI;

public class Event extends EMSBaseEntity {

    public static final String SESSION_COLLECTION = "session collection";

    public Event(Item item) {
        super(item);
    }

    public URI getSessionHref() {
        return super.getLink(SESSION_COLLECTION).href;
    }
}
